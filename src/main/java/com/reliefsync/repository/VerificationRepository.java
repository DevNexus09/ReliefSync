package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.Role;
import com.reliefsync.model.Verification;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VerificationRepository {

    private Connection c() {
        return Database.getInstance().connection();
    }

    public List<Verification> forRequest(long requestId) {
        String sql = """
                SELECT v.id, v.request_id, v.round_role, v.verifier_id, u.full_name AS verifier_name,
                       v.approved, v.comment, v.decided_at
                FROM verifications v JOIN users u ON u.id = v.verifier_id
                WHERE v.request_id = ? ORDER BY v.id
                """;
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Verification> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Verification(rs.getLong("id"), rs.getLong("request_id"),
                            Role.valueOf(rs.getString("round_role")), rs.getLong("verifier_id"),
                            rs.getString("verifier_name"), rs.getInt("approved") == 1,
                            rs.getString("comment"), rs.getString("decided_at")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load verification history", e);
        }
    }

    public void insert(long requestId, Role roundRole, long verifierId, boolean approved, String comment) {
        String sql = "INSERT INTO verifications(request_id, round_role, verifier_id, approved, comment, decided_at)"
                + " VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            ps.setString(2, roundRole.name());
            ps.setLong(3, verifierId);
            ps.setInt(4, approved ? 1 : 0);
            ps.setString(5, comment);
            ps.setString(6, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record the verification decision", e);
        }
    }
}
