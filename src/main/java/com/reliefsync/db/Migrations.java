package com.reliefsync.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Minimal versioned migration runner. Each version is applied exactly once, in
 * order, and recorded in schema_version, so startup is repeatable and older
 * databases upgrade automatically.
 */
final class Migrations {

    private Migrations() {
    }

    private static final List<List<String>> VERSIONS = List.of(
            List.of(
                    """
                    CREATE TABLE users (
                      id            INTEGER PRIMARY KEY AUTOINCREMENT,
                      username      TEXT NOT NULL UNIQUE,
                      full_name     TEXT NOT NULL,
                      role          TEXT NOT NULL,
                      password_hash TEXT NOT NULL,
                      created_at    TEXT NOT NULL
                    )""",
                    """
                    CREATE TABLE affected_areas (
                      id         INTEGER PRIMARY KEY AUTOINCREMENT,
                      name       TEXT NOT NULL UNIQUE,
                      district   TEXT NOT NULL,
                      population INTEGER NOT NULL CHECK (population >= 0),
                      severity   INTEGER NOT NULL CHECK (severity BETWEEN 1 AND 5),
                      active     INTEGER NOT NULL DEFAULT 1
                    )""",
                    """
                    CREATE TABLE relief_centers (
                      id       INTEGER PRIMARY KEY AUTOINCREMENT,
                      name     TEXT NOT NULL UNIQUE,
                      location TEXT NOT NULL,
                      capacity INTEGER NOT NULL CHECK (capacity >= 0),
                      active   INTEGER NOT NULL DEFAULT 1
                    )""",
                    """
                    CREATE TABLE resources (
                      id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                      name                TEXT NOT NULL UNIQUE,
                      unit                TEXT NOT NULL,
                      low_stock_threshold INTEGER NOT NULL DEFAULT 10 CHECK (low_stock_threshold >= 0),
                      active              INTEGER NOT NULL DEFAULT 1
                    )""",
                    """
                    CREATE TABLE inventory (
                      id          INTEGER PRIMARY KEY AUTOINCREMENT,
                      center_id   INTEGER NOT NULL REFERENCES relief_centers(id),
                      resource_id INTEGER NOT NULL REFERENCES resources(id),
                      quantity    INTEGER NOT NULL CHECK (quantity >= 0),
                      UNIQUE (center_id, resource_id)
                    )""",
                    """
                    CREATE TABLE relief_requests (
                      id         INTEGER PRIMARY KEY AUTOINCREMENT,
                      area_id    INTEGER NOT NULL REFERENCES affected_areas(id),
                      priority   TEXT NOT NULL,
                      status     TEXT NOT NULL,
                      note       TEXT NOT NULL DEFAULT '',
                      created_by INTEGER NOT NULL REFERENCES users(id),
                      created_at TEXT NOT NULL
                    )""",
                    """
                    CREATE TABLE request_items (
                      id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                      request_id         INTEGER NOT NULL REFERENCES relief_requests(id) ON DELETE CASCADE,
                      resource_id        INTEGER NOT NULL REFERENCES resources(id),
                      quantity_requested INTEGER NOT NULL CHECK (quantity_requested > 0),
                      quantity_allocated INTEGER NOT NULL DEFAULT 0 CHECK (quantity_allocated >= 0),
                      UNIQUE (request_id, resource_id)
                    )""",
                    """
                    CREATE TABLE verifications (
                      id          INTEGER PRIMARY KEY AUTOINCREMENT,
                      request_id  INTEGER NOT NULL REFERENCES relief_requests(id) ON DELETE CASCADE,
                      round_role  TEXT NOT NULL,
                      verifier_id INTEGER NOT NULL REFERENCES users(id),
                      approved    INTEGER NOT NULL,
                      comment     TEXT NOT NULL DEFAULT '',
                      decided_at  TEXT NOT NULL
                    )""",
                    """
                    CREATE TABLE allocations (
                      id          INTEGER PRIMARY KEY AUTOINCREMENT,
                      request_id  INTEGER NOT NULL REFERENCES relief_requests(id) ON DELETE CASCADE,
                      center_id   INTEGER NOT NULL REFERENCES relief_centers(id),
                      resource_id INTEGER NOT NULL REFERENCES resources(id),
                      quantity    INTEGER NOT NULL CHECK (quantity > 0),
                      strategy    TEXT NOT NULL,
                      created_at  TEXT NOT NULL
                    )""",
                    """
                    CREATE TABLE status_history (
                      id          INTEGER PRIMARY KEY AUTOINCREMENT,
                      request_id  INTEGER NOT NULL REFERENCES relief_requests(id) ON DELETE CASCADE,
                      from_status TEXT NOT NULL,
                      to_status   TEXT NOT NULL,
                      changed_by  TEXT NOT NULL,
                      changed_at  TEXT NOT NULL
                    )""",
                    "CREATE INDEX idx_requests_status ON relief_requests(status)",
                    "CREATE INDEX idx_inventory_center ON inventory(center_id)"
            ));

    static void apply(Connection c) {
        try {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS schema_version ("
                        + "version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)");
            }
            int current = currentVersion(c);
            for (int v = current + 1; v <= VERSIONS.size(); v++) {
                try (Statement st = c.createStatement()) {
                    for (String sql : VERSIONS.get(v - 1)) {
                        st.execute(sql);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO schema_version(version, applied_at) VALUES (?, ?)")) {
                    ps.setInt(1, v);
                    ps.setString(2, LocalDateTime.now().withNano(0).toString());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database migration failed: " + e.getMessage(), e);
        }
    }

    private static int currentVersion(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
