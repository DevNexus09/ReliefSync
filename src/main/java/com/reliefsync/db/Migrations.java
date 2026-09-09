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
            ),
            List.of(
                    "ALTER TABLE allocations ADD COLUMN active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))",
                    "ALTER TABLE allocations ADD COLUMN released_at TEXT",
                    "ALTER TABLE allocations ADD COLUMN released_by INTEGER REFERENCES users(id)",
                    """
                    CREATE TABLE allocation_events (
                      id            INTEGER PRIMARY KEY AUTOINCREMENT,
                      request_id    INTEGER NOT NULL REFERENCES relief_requests(id) ON DELETE CASCADE,
                      allocation_id INTEGER REFERENCES allocations(id) ON DELETE SET NULL,
                      event_type    TEXT NOT NULL CHECK (event_type IN ('ALLOCATED','REALLOCATED','RELEASED')),
                      center_id     INTEGER NOT NULL REFERENCES relief_centers(id),
                      resource_id   INTEGER NOT NULL REFERENCES resources(id),
                      quantity      INTEGER NOT NULL CHECK (quantity > 0),
                      actor_id      INTEGER NOT NULL REFERENCES users(id),
                      occurred_at   TEXT NOT NULL
                    )
                    """,
                    "CREATE INDEX idx_allocations_request_active ON allocations(request_id, active)",
                    "CREATE INDEX idx_allocation_events_request ON allocation_events(request_id)"
            ),
            List.of(
                    """
                    CREATE TABLE vehicles (
                      id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                      registration_number TEXT NOT NULL UNIQUE,
                      vehicle_type        TEXT NOT NULL,
                      capacity            INTEGER NOT NULL CHECK (capacity > 0),
                      status              TEXT NOT NULL CHECK (status IN ('AVAILABLE','IN_TRANSIT','MAINTENANCE','INACTIVE')),
                      created_at          TEXT NOT NULL
                    )
                    """,
                    """
                    CREATE TABLE dispatch_manifests (
                      id            INTEGER PRIMARY KEY AUTOINCREMENT,
                      request_id    INTEGER NOT NULL UNIQUE REFERENCES relief_requests(id),
                      vehicle_id    INTEGER NOT NULL REFERENCES vehicles(id),
                      driver_name   TEXT NOT NULL,
                      status        TEXT NOT NULL CHECK (status IN ('DISPATCHED','DELIVERED')),
                      assigned_at   TEXT NOT NULL,
                      dispatched_at TEXT NOT NULL,
                      delivered_at  TEXT
                    )
                    """,
                    """
                    CREATE TABLE dispatch_manifest_items (
                      id            INTEGER PRIMARY KEY AUTOINCREMENT,
                      manifest_id   INTEGER NOT NULL REFERENCES dispatch_manifests(id) ON DELETE CASCADE,
                      allocation_id INTEGER NOT NULL UNIQUE REFERENCES allocations(id),
                      quantity      INTEGER NOT NULL CHECK (quantity > 0)
                    )
                    """,
                    "CREATE INDEX idx_vehicles_status ON vehicles(status)",
                    "CREATE INDEX idx_dispatch_manifests_vehicle ON dispatch_manifests(vehicle_id)",
                    "CREATE INDEX idx_dispatch_manifest_items_manifest ON dispatch_manifest_items(manifest_id)"
            ));

    static void apply(Connection c) {
        try {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS schema_version ("
                        + "version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)");
            }
            int current = currentVersion(c);
            for (int v = current + 1; v <= VERSIONS.size(); v++) {
                c.setAutoCommit(false);
                try {
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
                    c.commit();
                } catch (SQLException e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(true);
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
