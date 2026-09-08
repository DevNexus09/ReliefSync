package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.AffectedArea;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AreaRepository {

    private Connection c() {
        return Database.getInstance().connection();
    }

    private AffectedArea map(ResultSet rs) throws SQLException {
        return new AffectedArea(rs.getLong("id"), rs.getString("name"), rs.getString("district"),
                rs.getInt("population"), rs.getInt("severity"), rs.getInt("active") == 1);
    }

    /** Bounded, parameterized search over name and district. */
    public List<AffectedArea> search(String text) {
        String sql = "SELECT * FROM affected_areas WHERE name LIKE ? OR district LIKE ? ORDER BY name LIMIT 200";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            String like = "%" + text.trim() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<AffectedArea> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not search affected areas", e);
        }
    }

    public List<AffectedArea> findActive() {
        try (PreparedStatement ps = c().prepareStatement(
                "SELECT * FROM affected_areas WHERE active = 1 ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            List<AffectedArea> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load affected areas", e);
        }
    }

    public void insert(String name, String district, int population, int severity) {
        String sql = "INSERT INTO affected_areas(name, district, population, severity) VALUES (?,?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, district);
            ps.setInt(3, population);
            ps.setInt(4, severity);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save area (is the name unique?)", e);
        }
    }

    public void update(long id, String name, String district, int population, int severity) {
        String sql = "UPDATE affected_areas SET name=?, district=?, population=?, severity=? WHERE id=?";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, district);
            ps.setInt(3, population);
            ps.setInt(4, severity);
            ps.setLong(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update area (is the name unique?)", e);
        }
    }

    public void setActive(long id, boolean active) {
        try (PreparedStatement ps = c().prepareStatement("UPDATE affected_areas SET active=? WHERE id=?")) {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not change area status", e);
        }
    }
}
