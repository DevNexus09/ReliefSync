package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.DeliveryFailure;
import com.reliefsync.model.DeliveryRecoveryAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeliveryFailureRepository {

    private Connection c() {
        return Database.getInstance().connection();
    }

    public long insert(long requestId, long manifestId, String reason, long reportedBy,
                       String reportedAt, DeliveryRecoveryAction action, String recoveryNotes) {
        String sql = "INSERT INTO delivery_failures(request_id, manifest_id, reason, reported_by,"
                + " reported_at, recovery_action, recovery_notes) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, requestId);
            ps.setLong(2, manifestId);
            ps.setString(3, reason);
            ps.setLong(4, reportedBy);
            ps.setString(5, reportedAt);
            ps.setString(6, action.name());
            ps.setString(7, normalizeNotes(recoveryNotes));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Could not obtain the delivery failure id");
                }
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record the delivery failure", e);
        }
    }

    public Optional<DeliveryFailure> latestForRequest(long requestId) {
        String sql = select() + " WHERE f.request_id=? ORDER BY f.id DESC LIMIT 1";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load the latest delivery failure", e);
        }
    }

    public List<DeliveryFailure> forRequest(long requestId) {
        String sql = select() + " WHERE f.request_id=? ORDER BY f.id";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                List<DeliveryFailure> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load delivery failures", e);
        }
    }

    public boolean markResolved(long failureId, String resolvedAt, String recoveryNotes) {
        String sql = "UPDATE delivery_failures SET resolved_at=?, recovery_notes="
                + "CASE WHEN ? IS NULL OR trim(?)='' THEN recovery_notes ELSE trim(?) END"
                + " WHERE id=? AND resolved_at IS NULL";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, resolvedAt);
            ps.setString(2, recoveryNotes);
            ps.setString(3, recoveryNotes);
            ps.setString(4, recoveryNotes);
            ps.setLong(5, failureId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not resolve the delivery failure", e);
        }
    }

    private static String select() {
        return """
                SELECT f.id, f.request_id, f.manifest_id, m.attempt_number, f.reason,
                       f.reported_by, u.full_name AS reporter_name, f.reported_at,
                       f.recovery_action, f.resolved_at, f.recovery_notes
                FROM delivery_failures f
                JOIN users u ON u.id=f.reported_by
                JOIN dispatch_manifests m ON m.id=f.manifest_id
                """;
    }

    private static DeliveryFailure map(ResultSet rs) throws SQLException {
        return new DeliveryFailure(rs.getLong("id"), rs.getLong("request_id"),
                rs.getLong("manifest_id"), rs.getInt("attempt_number"), rs.getString("reason"),
                rs.getLong("reported_by"), rs.getString("reporter_name"), rs.getString("reported_at"),
                DeliveryRecoveryAction.valueOf(rs.getString("recovery_action")),
                rs.getString("resolved_at"), rs.getString("recovery_notes"));
    }

    private static String normalizeNotes(String notes) {
        if (notes == null || notes.trim().isEmpty()) {
            return null;
        }
        return notes.trim();
    }
}
