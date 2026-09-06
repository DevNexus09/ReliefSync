package com.reliefsync.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class DatabaseHealthCheck {
    private final DatabaseManager databaseManager;

    public DatabaseHealthCheck() {
        this(new DatabaseManager());
    }

    public DatabaseHealthCheck(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "Database manager must not be null.");
    }

    public HealthStatus check() {
        try (Connection connection = databaseManager.openConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("SELECT 1 AS result")) {
                if (!result.next() || result.getInt("result") != 1) {
                    return HealthStatus.failed("SQLite SELECT 1 returned an unexpected result.");
                }
            }

            try (ResultSet version = statement.executeQuery("SELECT sqlite_version() AS version")) {
                if (!version.next()) {
                    return HealthStatus.failed("SQLite did not report its version.");
                }
                return HealthStatus.connected(version.getString("version"));
            }
        } catch (SQLException exception) {
            return HealthStatus.failed(exception.getMessage());
        }
    }

    public record HealthStatus(boolean connected, String sqliteVersion, String detail) {
        private static HealthStatus connected(String sqliteVersion) {
            return new HealthStatus(true, sqliteVersion, "Connection successful");
        }

        private static HealthStatus failed(String detail) {
            String safeDetail = detail == null || detail.isBlank() ? "Unknown database error" : detail;
            return new HealthStatus(false, "Unavailable", safeDetail);
        }
    }
}
