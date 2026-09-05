package com.reliefsync.database;

import java.nio.file.Path;
import java.util.Objects;

public final class DatabaseConfig {
    private static final Path DEFAULT_DATABASE_PATH = Path.of("data", "reliefsync.db");

    private final Path databasePath;

    public DatabaseConfig() {
        this(DEFAULT_DATABASE_PATH);
    }

    public DatabaseConfig(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "Database path must not be null.")
                .toAbsolutePath()
                .normalize();
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public String getJdbcUrl() {
        return "jdbc:sqlite:" + databasePath;
    }
}
