package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.Allocation;
import com.reliefsync.model.AllocationEvent;
import com.reliefsync.model.AllocationEventType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AllocationRepository {

    private Connection c() {
        return Database.getInstance().connection();
    }

    public long insert(long requestId, long centerId, long resourceId, int quantity, String strategy) {
        String sql = "INSERT INTO allocations(request_id, center_id, resource_id, quantity, strategy, created_at)"
                + " VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, requestId);
            ps.setLong(2, centerId);
            ps.setLong(3, resourceId);
            ps.setInt(4, quantity);
            ps.setString(5, strategy);
            ps.setString(6, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Could not obtain the allocation id");
                }
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record the allocation", e);
        }
    }

    public List<Allocation> forRequest(long requestId) {
        String sql = """
                SELECT al.id, al.request_id, al.center_id, c.name AS center_name,
                       al.resource_id, r.name AS resource_name, al.quantity, al.strategy, al.created_at,
                       al.active, al.released_at, u.full_name AS released_by_name
                FROM allocations al
                JOIN relief_centers c ON c.id = al.center_id
                JOIN resources r ON r.id = al.resource_id
                LEFT JOIN users u ON u.id = al.released_by
                WHERE al.request_id = ? ORDER BY al.id
                """;
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Allocation> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Allocation(rs.getLong("id"), rs.getLong("request_id"),
                            rs.getLong("center_id"), rs.getString("center_name"),
                            rs.getLong("resource_id"), rs.getString("resource_name"),
                            rs.getInt("quantity"), rs.getString("strategy"), rs.getString("created_at"),
                            rs.getBoolean("active"), rs.getString("released_at"),
                            rs.getString("released_by_name")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load allocations", e);
        }
    }

    public List<Allocation> activeForRequest(long requestId) {
        return forRequest(requestId).stream().filter(Allocation::active).toList();
    }

    /** Marks one reservation released. Returns false if it was already released. */
    public boolean markReleased(long allocationId, long actorId, String releasedAt) {
        String sql = "UPDATE allocations SET active=0, released_at=?, released_by=? WHERE id=? AND active=1";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, releasedAt);
            ps.setLong(2, actorId);
            ps.setLong(3, allocationId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not release the allocation", e);
        }
    }

    public void addEvent(long requestId, long allocationId, AllocationEventType type,
                         long centerId, long resourceId, int quantity, long actorId, String occurredAt) {
        String sql = "INSERT INTO allocation_events(request_id, allocation_id, event_type, center_id,"
                + " resource_id, quantity, actor_id, occurred_at) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            ps.setLong(2, allocationId);
            ps.setString(3, type.name());
            ps.setLong(4, centerId);
            ps.setLong(5, resourceId);
            ps.setInt(6, quantity);
            ps.setLong(7, actorId);
            ps.setString(8, occurredAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record the allocation event", e);
        }
    }

    public List<AllocationEvent> eventsForRequest(long requestId) {
        String sql = """
                SELECT e.id, e.request_id, e.allocation_id, e.event_type,
                       c.name AS center_name, r.name AS resource_name, e.quantity,
                       u.full_name AS actor_name, e.occurred_at
                FROM allocation_events e
                JOIN relief_centers c ON c.id = e.center_id
                JOIN resources r ON r.id = e.resource_id
                JOIN users u ON u.id = e.actor_id
                WHERE e.request_id = ? ORDER BY e.id
                """;
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AllocationEvent> out = new ArrayList<>();
                while (rs.next()) {
                    long allocationId = rs.getLong("allocation_id");
                    out.add(new AllocationEvent(rs.getLong("id"), rs.getLong("request_id"),
                            rs.wasNull() ? null : allocationId,
                            AllocationEventType.valueOf(rs.getString("event_type")),
                            rs.getString("center_name"), rs.getString("resource_name"),
                            rs.getInt("quantity"), rs.getString("actor_name"),
                            rs.getString("occurred_at")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load allocation events", e);
        }
    }
}
