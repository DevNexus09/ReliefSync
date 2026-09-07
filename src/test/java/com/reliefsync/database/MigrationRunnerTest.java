package com.reliefsync.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.exception.PersistenceException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationRunnerTest {
  @TempDir Path directory;

  @Test
  void givenFreshDatabase_whenMigrationsRun_thenAllCoreTablesAndVersionsExist() throws Exception {
    DatabaseManager manager = manager();
    MigrationRunner runner = new MigrationRunner(manager);

    runner.runMigrations();

    assertEquals(3, scalar(manager, "SELECT COUNT(*) FROM schema_migrations"));
    assertEquals(
        17,
        scalar(
            manager,
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"));
    assertEquals(1, scalar(manager, "SELECT COUNT(*) FROM schema_migrations WHERE version='V001'"));
    assertEquals(1, scalar(manager, "SELECT COUNT(*) FROM schema_migrations WHERE version='V002'"));
    assertEquals(1, scalar(manager, "SELECT COUNT(*) FROM schema_migrations WHERE version='V003'"));
  }

  @Test
  void givenMigratedDatabase_whenMigrationsRunAgain_thenHistoryIsNotDuplicated() throws Exception {
    MigrationRunner runner = new MigrationRunner(manager());
    runner.runMigrations();
    runner.runMigrations();
    assertEquals(3, scalar(manager(), "SELECT COUNT(*) FROM schema_migrations"));
  }

  @Test
  void givenMigratedDatabase_whenIntegrityChecked_thenDatabaseIsValid() throws Exception {
    DatabaseManager manager = manager();
    new MigrationRunner(manager).runMigrations();
    try (Connection connection = manager.openConnection();
        Statement statement = connection.createStatement()) {
      try (ResultSet result = statement.executeQuery("PRAGMA foreign_key_check")) {
        assertTrue(!result.next());
      }
      try (ResultSet result = statement.executeQuery("PRAGMA integrity_check")) {
        assertTrue(result.next());
        assertEquals("ok", result.getString(1));
      }
    }
  }

  @Test
  void givenTransactionFailure_whenWorkThrows_thenWritesAreRolledBack() throws Exception {
    DatabaseManager manager = manager();
    new MigrationRunner(manager).runMigrations();
    TransactionManager transactions = new TransactionManager(manager);

    org.junit.jupiter.api.Assertions.assertThrows(
        PersistenceException.class,
        () ->
            transactions.execute(
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                        "INSERT INTO resources(name,category,unit,minimum_stock_threshold,active)"
                            + " VALUES('Test','Test','item',0,1)");
                  }
                  throw new IllegalStateException("force rollback");
                }));

    assertEquals(0, scalar(manager, "SELECT COUNT(*) FROM resources"));
  }

  private DatabaseManager manager() {
    return new DatabaseManager(new DatabaseConfig(directory.resolve("migrations.db")));
  }

  private long scalar(DatabaseManager manager, String sql) throws Exception {
    try (Connection connection = manager.openConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(sql)) {
      assertTrue(result.next());
      return result.getLong(1);
    }
  }
}
