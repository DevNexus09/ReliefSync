package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.User;
import com.reliefsync.model.enums.Role;
import com.reliefsync.repository.UserRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SQLiteUserRepository implements UserRepository {
  private final DatabaseManager databaseManager;

  public SQLiteUserRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  @Override
  public Optional<User> findById(long id) {
    return findOne("SELECT * FROM users WHERE id = ?", id, "find user by ID");
  }

  @Override
  public Optional<User> findByUsername(String username) {
    String sql = "SELECT * FROM users WHERE username = ?";
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, username);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? Optional.of(map(result)) : Optional.empty();
      }
    } catch (SQLException exception) {
      throw failure("find user by username", exception);
    }
  }

  @Override
  public List<User> findAll() {
    String sql = "SELECT * FROM users ORDER BY full_name";
    List<User> users = new ArrayList<>();
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet result = statement.executeQuery()) {
      while (result.next()) users.add(map(result));
      return users;
    } catch (SQLException exception) {
      throw failure("find users", exception);
    }
  }

  @Override
  public long save(User user) {
    String sql =
        """
INSERT INTO users(full_name, username, password_hash, password_salt, role, active, created_at)
VALUES (?, ?, ?, ?, ?, ?, ?)
""";
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement =
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(statement, user, false);
      statement.executeUpdate();
      return SQLiteSupport.generatedId(statement, "Saving user");
    } catch (SQLException exception) {
      throw failure("save user", exception);
    }
  }

  @Override
  public void update(User user) {
    String sql =
        """
UPDATE users SET full_name=?, username=?, password_hash=?, password_salt=?, role=?, active=?, created_at=?
WHERE id=?
""";
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, user, true);
      if (statement.executeUpdate() == 0)
        throw new PersistenceException("User was not found: " + user.id());
    } catch (SQLException exception) {
      throw failure("update user", exception);
    }
  }

  @Override
  public boolean existsByUsername(String username) {
    return findByUsername(username).isPresent();
  }

  private Optional<User> findOne(String sql, long id, String operation) {
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? Optional.of(map(result)) : Optional.empty();
      }
    } catch (SQLException exception) {
      throw failure(operation, exception);
    }
  }

  private void bind(PreparedStatement statement, User user, boolean includeId) throws SQLException {
    statement.setString(1, user.fullName());
    statement.setString(2, user.username());
    statement.setString(3, user.passwordHash());
    statement.setString(4, user.passwordSalt());
    statement.setString(5, user.role().name());
    statement.setInt(6, user.active() ? 1 : 0);
    statement.setString(7, user.createdAt().toString());
    if (includeId) statement.setLong(8, user.id());
  }

  private User map(ResultSet result) throws SQLException {
    return new User(
        result.getLong("id"),
        result.getString("full_name"),
        result.getString("username"),
        result.getString("password_hash"),
        result.getString("password_salt"),
        Role.valueOf(result.getString("role")),
        result.getInt("active") == 1,
        SQLiteSupport.dateTime(result, "created_at"));
  }

  private PersistenceException failure(String operation, SQLException exception) {
    return new PersistenceException("Failed to " + operation + ".", exception);
  }
}
