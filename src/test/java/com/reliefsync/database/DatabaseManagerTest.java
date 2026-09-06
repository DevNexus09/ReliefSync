package com.reliefsync.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void givenTemporaryPath_whenConnectionOpens_thenDatabaseFileIsCreated() throws Exception {
        DatabaseManager manager = managerForTemporaryDatabase();
        Path databasePath = temporaryDirectory.resolve("reliefsync-test.db");

        try (Connection connection = manager.openConnection()) {
            assertTrue(connection.isValid(1));
        }

        assertTrue(java.nio.file.Files.exists(databasePath));
    }

    @Test
    void givenOpenConnection_whenSelectOneRuns_thenItReturnsOne() throws Exception {
        DatabaseManager manager = managerForTemporaryDatabase();

        try (Connection connection = manager.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1 AS result")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt("result"));
        }
    }

    @Test
    void givenManagedConnection_whenForeignKeysQueried_thenTheyAreEnabled() throws Exception {
        DatabaseManager manager = managerForTemporaryDatabase();

        try (Connection connection = manager.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void givenManagedConnection_whenClosed_thenItReportsClosed() throws Exception {
        DatabaseManager manager = managerForTemporaryDatabase();
        Connection connection = manager.openConnection();

        connection.close();

        assertTrue(connection.isClosed());
    }

    @Test
    void givenValidDatabase_whenHealthChecked_thenStatusIsConnected() {
        DatabaseManager manager = managerForTemporaryDatabase();

        DatabaseHealthCheck.HealthStatus status = new DatabaseHealthCheck(manager).check();

        assertTrue(status.connected());
        assertTrue(status.detail().contains("successful"));
        assertTrue(status.sqliteVersion().matches("\\d+\\.\\d+\\.\\d+"));
    }

    private DatabaseManager managerForTemporaryDatabase() {
        Path databasePath = temporaryDirectory.resolve("reliefsync-test.db");
        return new DatabaseManager(new DatabaseConfig(databasePath));
    }
}
