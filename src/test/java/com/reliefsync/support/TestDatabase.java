package com.reliefsync.support;

import com.reliefsync.database.DatabaseConfig;
import com.reliefsync.database.DatabaseManager;
import com.reliefsync.database.MigrationRunner;
import java.nio.file.Path;

public final class TestDatabase {
  private TestDatabase() {}

  public static DatabaseManager migrated(Path directory) {
    DatabaseManager manager = new DatabaseManager(new DatabaseConfig(directory.resolve("test.db")));
    new MigrationRunner(manager).runMigrations();
    return manager;
  }
}
