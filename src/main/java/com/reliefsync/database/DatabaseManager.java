package com.reliefsync.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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

    public boolean isConnectionValid() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1")) {
            return result.next() && result.getInt(1) == 1;
        }
    }

    public String getSqliteVersion() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT sqlite_version()")) {
            if (!result.next()) {
                throw new SQLException("SQLite did not return a version.");
            }
            return result.getString(1);
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
