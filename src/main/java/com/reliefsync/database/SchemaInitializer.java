package com.reliefsync.database;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class SchemaInitializer {
  private final MigrationRunner migrationRunner;
  private final DatabaseSeeder databaseSeeder;
  private final BooleanSupplier demoSeedEnabled;

  public SchemaInitializer(MigrationRunner migrationRunner, DatabaseSeeder databaseSeeder) {
    this(
        migrationRunner,
        databaseSeeder,
        () -> "true".equalsIgnoreCase(System.getenv("RELIEFSYNC_SEED_DEMO")));
  }

  public SchemaInitializer(
      MigrationRunner migrationRunner,
      DatabaseSeeder databaseSeeder,
      BooleanSupplier demoSeedEnabled) {
    this.migrationRunner =
        Objects.requireNonNull(migrationRunner, "Migration runner must not be null.");
    this.databaseSeeder =
        Objects.requireNonNull(databaseSeeder, "Database seeder must not be null.");
    this.demoSeedEnabled =
        Objects.requireNonNull(demoSeedEnabled, "Seed setting must not be null.");
  }

  public void initialize() {
    migrationRunner.runMigrations();
    if (demoSeedEnabled.getAsBoolean()) {
      databaseSeeder.seedDemoData();
    }
  }
}
