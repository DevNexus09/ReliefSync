package com.reliefsync.database;

import com.reliefsync.exception.PersistenceException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class MigrationRunner {
  private static final String MIGRATION_ROOT = "/database/migration/";
  private static final List<Migration> MIGRATIONS =
      List.of(
          new Migration("V001", "initial schema", "V001__initial_schema.sql"),
          new Migration("V002", "indexes", "V002__indexes.sql"));

  private final DatabaseManager databaseManager;

  public MigrationRunner(DatabaseManager databaseManager) {
    this.databaseManager =
        Objects.requireNonNull(databaseManager, "Database manager must not be null.");
  }

  public void runMigrations() {
    createMigrationTable();
    for (Migration migration : MIGRATIONS) {
      if (!isApplied(migration.version())) {
        apply(migration);
      }
    }
  }

  private void createMigrationTable() {
    String sql =
        """
        CREATE TABLE IF NOT EXISTS schema_migrations (
            version TEXT PRIMARY KEY,
            description TEXT NOT NULL,
            applied_at TEXT NOT NULL
        )
        """;
    try (Connection connection = databaseManager.openConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
    } catch (SQLException exception) {
      throw new PersistenceException("Unable to create migration history table.", exception);
    }
  }

  private boolean isApplied(String version) {
    String sql = "SELECT 1 FROM schema_migrations WHERE version = ?";
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, version);
      try (ResultSet result = statement.executeQuery()) {
        return result.next();
      }
    } catch (SQLException exception) {
      throw new PersistenceException("Unable to inspect migration " + version + ".", exception);
    }
  }

  private void apply(Migration migration) {
    String script = readResource(MIGRATION_ROOT + migration.resource());
    try (Connection connection = databaseManager.openConnection()) {
      connection.setAutoCommit(false);
      try {
        executeScript(connection, script);
        recordMigration(connection, migration);
        connection.commit();
      } catch (SQLException exception) {
        rollback(connection, exception);
        throw new PersistenceException(
            "Failed to apply migration " + migration.version() + ".", exception);
      }
    } catch (SQLException exception) {
      throw new PersistenceException(
          "Unable to run migration " + migration.version() + ".", exception);
    }
  }

  private void executeScript(Connection connection, String script) throws SQLException {
    for (String statementText : script.split(";")) {
      String sql = statementText.trim();
      if (!sql.isEmpty()) {
        try (Statement statement = connection.createStatement()) {
          statement.execute(sql);
        }
      }
    }
  }

  private void recordMigration(Connection connection, Migration migration) throws SQLException {
    String sql = "INSERT INTO schema_migrations(version, description, applied_at) VALUES (?, ?, ?)";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, migration.version());
      statement.setString(2, migration.description());
      statement.setString(3, LocalDateTime.now().toString());
      statement.executeUpdate();
    }
  }

  private String readResource(String path) {
    try (InputStream stream = MigrationRunner.class.getResourceAsStream(path)) {
      if (stream == null) {
        throw new PersistenceException("Migration resource was not found: " + path);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new PersistenceException("Unable to read migration resource: " + path, exception);
    }
  }

  private void rollback(Connection connection, SQLException original) {
    try {
      connection.rollback();
    } catch (SQLException rollbackFailure) {
      original.addSuppressed(rollbackFailure);
    }
  }

  private record Migration(String version, String description, String resource) {}
}
