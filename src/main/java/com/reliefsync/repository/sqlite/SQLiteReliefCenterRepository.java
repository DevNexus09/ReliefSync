package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.ReliefCenter;
import com.reliefsync.repository.ReliefCenterRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteReliefCenterRepository implements ReliefCenterRepository {
  private final DatabaseManager databaseManager;

  public SQLiteReliefCenterRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<ReliefCenter> findById(long id) {
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement("SELECT * FROM relief_centers WHERE id=?")) {
      s.setLong(1, id);
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? Optional.of(map(r)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw fail("find relief center", e);
    }
  }

  public List<ReliefCenter> findAll() {
    List<ReliefCenter> list = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement("SELECT * FROM relief_centers ORDER BY name");
        ResultSet r = s.executeQuery()) {
      while (r.next()) list.add(map(r));
      return list;
    } catch (SQLException e) {
      throw fail("find relief centers", e);
    }
  }

  public long save(ReliefCenter v) {
    String sql =
        "INSERT INTO"
            + " relief_centers(name,district,latitude,longitude,contact_info,active,created_at)"
            + " VALUES(?,?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving relief center");
    } catch (SQLException e) {
      throw fail("save relief center", e);
    }
  }

  public void update(ReliefCenter v) {
    String sql =
        "UPDATE relief_centers SET"
            + " name=?,district=?,latitude=?,longitude=?,contact_info=?,active=?,created_at=? WHERE"
            + " id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Relief center was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update relief center", e);
    }
  }

  private void bind(PreparedStatement s, ReliefCenter v, boolean id) throws SQLException {
    s.setString(1, v.name());
    s.setString(2, v.district());
    SQLiteSupport.setNullableDouble(s, 3, v.latitude());
    SQLiteSupport.setNullableDouble(s, 4, v.longitude());
    s.setString(5, v.contactInfo());
    s.setInt(6, v.active() ? 1 : 0);
    s.setString(7, v.createdAt().toString());
    if (id) s.setLong(8, v.id());
  }

  private ReliefCenter map(ResultSet r) throws SQLException {
    return new ReliefCenter(
        r.getLong("id"),
        r.getString("name"),
        r.getString("district"),
        SQLiteSupport.nullableDouble(r, "latitude"),
        SQLiteSupport.nullableDouble(r, "longitude"),
        r.getString("contact_info"),
        r.getInt("active") == 1,
        SQLiteSupport.dateTime(r, "created_at"));
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
