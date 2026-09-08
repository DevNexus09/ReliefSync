package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.ReliefCenter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CenterRepository {

    private Connection c() {
        return Database.getInstance().connection();
    }

    private ReliefCenter map(ResultSet rs) throws SQLException {
        return new ReliefCenter(rs.getLong("id"), rs.getString("name"), rs.getString("location"),
                rs.getInt("capacity"), rs.getInt("active") == 1);
    }

    public List<ReliefCenter> search(String text) {
        String sql = "SELECT * FROM relief_centers WHERE name LIKE ? OR location LIKE ? ORDER BY name LIMIT 200";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            String like = "%" + text.trim() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<ReliefCenter> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not search relief centers", e);
        }
    }

    public List<ReliefCenter> findActive() {
        try (PreparedStatement ps = c().prepareStatement(
                "SELECT * FROM relief_centers WHERE active = 1 ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            List<ReliefCenter> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load relief centers", e);
        }
    }

    public void insert(String name, String location, int capacity) {
        try (PreparedStatement ps = c().prepareStatement(
                "INSERT INTO relief_centers(name, location, capacity) VALUES (?,?,?)")) {
            ps.setString(1, name);
            ps.setString(2, location);
            ps.setInt(3, capacity);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save center (is the name unique?)", e);
        }
    }

    public void update(long id, String name, String location, int capacity) {
        try (PreparedStatement ps = c().prepareStatement(
                "UPDATE relief_centers SET name=?, location=?, capacity=? WHERE id=?")) {
            ps.setString(1, name);
            ps.setString(2, location);
            ps.setInt(3, capacity);
            ps.setLong(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update center (is the name unique?)", e);
        }
    }

    public void setActive(long id, boolean active) {
        try (PreparedStatement ps = c().prepareStatement("UPDATE relief_centers SET active=? WHERE id=?")) {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not change center status", e);
        }
    }
}
