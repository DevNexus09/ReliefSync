package com.reliefsync.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationsTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        Database.reset();
    }

    @Test
    void versionOneDatabaseUpgradesWithoutLosingAllocations() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("version-one.db");
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE schema_version(version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)");
            statement.execute("INSERT INTO schema_version VALUES (1, '2026-01-01T00:00:00')");
            statement.execute("CREATE TABLE users(id INTEGER PRIMARY KEY, full_name TEXT NOT NULL)");
            statement.execute("CREATE TABLE affected_areas(id INTEGER PRIMARY KEY)");
            statement.execute("CREATE TABLE relief_centers(id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
            statement.execute("CREATE TABLE resources(id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
            statement.execute("CREATE TABLE relief_requests(id INTEGER PRIMARY KEY)");
            statement.execute("CREATE TABLE allocations(id INTEGER PRIMARY KEY, request_id INTEGER NOT NULL, "
                    + "center_id INTEGER NOT NULL, resource_id INTEGER NOT NULL, quantity INTEGER NOT NULL, "
                    + "strategy TEXT NOT NULL, created_at TEXT NOT NULL)");
            statement.execute("INSERT INTO users VALUES (1, 'Existing User')");
            statement.execute("INSERT INTO affected_areas VALUES (1)");
            statement.execute("INSERT INTO relief_centers VALUES (1, 'Existing Center')");
            statement.execute("INSERT INTO resources VALUES (1, 'Existing Resource')");
            statement.execute("INSERT INTO relief_requests VALUES (1)");
            statement.execute("INSERT INTO allocations VALUES (1, 1, 1, 1, 25, 'Fewest Centers', "
                    + "'2026-01-01T00:00:00')");
        }

        Database.init(url);
        try (var statement = Database.getInstance().connection().createStatement()) {
            try (var rs = statement.executeQuery("SELECT MAX(version) FROM schema_version")) {
                assertEquals(3, rs.getInt(1));
            }
            try (var rs = statement.executeQuery("SELECT quantity, active FROM allocations WHERE id=1")) {
                assertEquals(25, rs.getInt("quantity"));
                assertEquals(1, rs.getInt("active"));
            }
            try (var rs = statement.executeQuery("SELECT COUNT(*) FROM allocation_events")) {
                assertEquals(0, rs.getInt(1));
            }
            try (var rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table'"
                            + " AND name IN ('vehicles','dispatch_manifests','dispatch_manifest_items')")) {
                assertEquals(3, rs.getInt(1));
            }
        }
    }

    @Test
    void versionTwoDatabaseUpgradesToTransportSchemaWithoutChangingAllocation() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("version-two.db");
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE schema_version(version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)");
            statement.execute("INSERT INTO schema_version VALUES (2, '2026-01-01T00:00:00')");
            statement.execute("CREATE TABLE users(id INTEGER PRIMARY KEY, full_name TEXT NOT NULL)");
            statement.execute("CREATE TABLE relief_centers(id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
            statement.execute("CREATE TABLE resources(id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
            statement.execute("CREATE TABLE relief_requests(id INTEGER PRIMARY KEY)");
            statement.execute("CREATE TABLE allocations(id INTEGER PRIMARY KEY, request_id INTEGER NOT NULL, "
                    + "center_id INTEGER NOT NULL, resource_id INTEGER NOT NULL, quantity INTEGER NOT NULL, "
                    + "strategy TEXT NOT NULL, created_at TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, "
                    + "released_at TEXT, released_by INTEGER)");
            statement.execute("CREATE TABLE allocation_events(id INTEGER PRIMARY KEY)");
            statement.execute("INSERT INTO allocations VALUES (7, 1, 1, 1, 40, 'Fewest Centers', "
                    + "'2026-01-01T00:00:00', 1, NULL, NULL)");
        }

        Database.init(url);
        try (var statement = Database.getInstance().connection().createStatement()) {
            try (var rs = statement.executeQuery("SELECT MAX(version) FROM schema_version")) {
                assertEquals(3, rs.getInt(1));
            }
            try (var rs = statement.executeQuery("SELECT quantity, active FROM allocations WHERE id=7")) {
                assertEquals(40, rs.getInt("quantity"));
                assertEquals(1, rs.getInt("active"));
            }
            try (var rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table'"
                            + " AND name IN ('vehicles','dispatch_manifests','dispatch_manifest_items')")) {
                assertEquals(3, rs.getInt(1));
            }
        }
    }
}
