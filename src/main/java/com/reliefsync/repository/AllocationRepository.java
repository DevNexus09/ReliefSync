package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.Allocation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AllocationRepository {

    private Connection c() {
        return Database.getInstance().connection();
    }

    public void insert(long requestId, long centerId, long resourceId, int quantity, String strategy) {
        String sql = "INSERT INTO allocations(request_id, center_id, resource_id, quantity, strategy, created_at)"
                + " VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            ps.setLong(2, centerId);
            ps.setLong(3, resourceId);
            ps.setInt(4, quantity);
            ps.setString(5, strategy);
            ps.setString(6, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record the allocation", e);
        }
    }

    public List<Allocation> forRequest(long requestId) {
        String sql = """
                SELECT al.id, al.request_id, al.center_id, c.name AS center_name,
                       al.resource_id, r.name AS resource_name, al.quantity, al.strategy, al.created_at
                FROM allocations al
                JOIN relief_centers c ON c.id = al.center_id
                JOIN resources r ON r.id = al.resource_id
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
                            rs.getInt("quantity"), rs.getString("strategy"), rs.getString("created_at")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load allocations", e);
        }
    }
}
