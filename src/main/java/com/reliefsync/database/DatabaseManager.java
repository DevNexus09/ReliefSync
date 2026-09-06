package com.reliefsync.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class DatabaseManager {
    private final DatabaseConfig config;

    public DatabaseManager() {
        this(new DatabaseConfig());
    }

    public DatabaseManager(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config, "Database configuration must not be null.");
    }

    public Connection openConnection() throws SQLException {
        createDatabaseDirectory();
        Connection connection = DriverManager.getConnection(config.getJdbcUrl());
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    private void createDatabaseDirectory() throws SQLException {
        Path parent = config.getDatabasePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new SQLException("Unable to create the database directory: " + parent, exception);
        }
    }
}
