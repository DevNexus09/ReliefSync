package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.Allocation;
import com.reliefsync.model.DispatchManifest;
import com.reliefsync.model.DispatchManifestItem;
import com.reliefsync.model.ManifestStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DispatchManifestRepository {

    private static final String MANIFEST_SELECT = """
            SELECT m.id, m.request_id, m.attempt_number, m.vehicle_id,
                   v.registration_number, v.vehicle_type, v.capacity, m.driver_name,
                   m.status, m.assigned_at, m.dispatched_at, m.delivered_at, m.failed_at,
                   COALESCE(SUM(i.quantity), 0) AS total_load
            FROM dispatch_manifests m
            JOIN vehicles v ON v.id=m.vehicle_id
            LEFT JOIN dispatch_manifest_items i ON i.manifest_id=m.id
            """;

    private Connection c() {
        return Database.getInstance().connection();
    }

    public long insert(long requestId, long vehicleId, String driverName, String timestamp) {
        String sql = "INSERT INTO dispatch_manifests(request_id, vehicle_id, driver_name, status,"
                + " assigned_at, dispatched_at, attempt_number) VALUES (?,?,?,'DISPATCHED',?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, requestId);
            ps.setLong(2, vehicleId);
            ps.setString(3, driverName);
            ps.setString(4, timestamp);
            ps.setString(5, timestamp);
            ps.setInt(6, nextAttemptNumber(requestId));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create dispatch manifest", e);
        }
    }

    /** Inserts only when the allocation is active and belongs to the dispatched request. */
    public void insertItem(long manifestId, long requestId, Allocation allocation) {
        String sql = "INSERT INTO dispatch_manifest_items(manifest_id, allocation_id, quantity) "
                + "SELECT ?, id, quantity FROM allocations"
                + " WHERE id=? AND request_id=? AND active=1 AND quantity=? AND quantity>0";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, manifestId);
            ps.setLong(2, allocation.id());
            ps.setLong(3, requestId);
            ps.setInt(4, allocation.quantity());
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Allocation #" + allocation.id()
                        + " is not an active reservation for this request");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not add allocation to dispatch manifest", e);
        }
    }

    public Optional<DispatchManifest> findByRequest(long requestId) {
        String sql = MANIFEST_SELECT
                + " WHERE m.request_id=? GROUP BY m.id ORDER BY m.attempt_number DESC LIMIT 1";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load dispatch manifest", e);
        }
    }

    public List<DispatchManifest> allForRequest(long requestId) {
        String sql = MANIFEST_SELECT
                + " WHERE m.request_id=? GROUP BY m.id ORDER BY m.attempt_number";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                List<DispatchManifest> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load dispatch attempts", e);
        }
    }

    public int nextAttemptNumber(long requestId) {
        String sql = "SELECT COALESCE(MAX(attempt_number), 0) + 1 FROM dispatch_manifests WHERE request_id=?";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not determine the next dispatch attempt", e);
        }
    }

    public List<DispatchManifestItem> items(long manifestId) {
        String sql = """
                SELECT i.id, i.manifest_id, i.allocation_id, c.name AS center_name,
                       r.name AS resource_name, i.quantity
                FROM dispatch_manifest_items i
                JOIN allocations a ON a.id=i.allocation_id
                JOIN relief_centers c ON c.id=a.center_id
                JOIN resources r ON r.id=a.resource_id
                WHERE i.manifest_id=? ORDER BY c.name, r.name
                """;
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, manifestId);
            try (ResultSet rs = ps.executeQuery()) {
                List<DispatchManifestItem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new DispatchManifestItem(rs.getLong("id"), rs.getLong("manifest_id"),
                            rs.getLong("allocation_id"), rs.getString("center_name"),
                            rs.getString("resource_name"), rs.getInt("quantity")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load dispatch manifest items", e);
        }
    }

    public boolean markDelivered(long manifestId, String deliveredAt) {
        String sql = "UPDATE dispatch_manifests SET status='DELIVERED', delivered_at=?"
                + " WHERE id=? AND status='DISPATCHED'";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, deliveredAt);
            ps.setLong(2, manifestId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not complete dispatch manifest", e);
        }
    }

    public boolean markDeliveryFailed(long manifestId, String failedAt) {
        String sql = "UPDATE dispatch_manifests SET status='DELIVERY_FAILED', failed_at=?"
                + " WHERE id=? AND status='DISPATCHED'";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, failedAt);
            ps.setLong(2, manifestId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not mark the dispatch attempt as failed", e);
        }
    }

    private static DispatchManifest map(ResultSet rs) throws SQLException {
        return new DispatchManifest(rs.getLong("id"), rs.getLong("request_id"),
                rs.getInt("attempt_number"), rs.getLong("vehicle_id"),
                rs.getString("registration_number"), rs.getString("vehicle_type"),
                rs.getInt("capacity"), rs.getString("driver_name"),
                ManifestStatus.valueOf(rs.getString("status")), rs.getString("assigned_at"),
                rs.getString("dispatched_at"), rs.getString("delivered_at"),
                rs.getString("failed_at"), rs.getInt("total_load"));
    }
}
