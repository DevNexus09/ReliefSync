package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.Resource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResourceRepository {

    private Connection c() {
        return Database.getInstance().connection();
    }

    private Resource map(ResultSet rs) throws SQLException {
        return new Resource(rs.getLong("id"), rs.getString("name"), rs.getString("unit"),
                rs.getInt("low_stock_threshold"), rs.getInt("active") == 1);
    }

    public List<Resource> search(String text) {
        String sql = "SELECT * FROM resources WHERE name LIKE ? ORDER BY name LIMIT 200";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, "%" + text.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Resource> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not search resources", e);
        }
    }

    public List<Resource> findActive() {
        try (PreparedStatement ps = c().prepareStatement(
                "SELECT * FROM resources WHERE active = 1 ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            List<Resource> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load resources", e);
        }
    }

    public void insert(String name, String unit, int lowStockThreshold) {
        try (PreparedStatement ps = c().prepareStatement(
                "INSERT INTO resources(name, unit, low_stock_threshold) VALUES (?,?,?)")) {
            ps.setString(1, name);
            ps.setString(2, unit);
            ps.setInt(3, lowStockThreshold);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save resource (is the name unique?)", e);
        }
    }

    public void update(long id, String name, String unit, int lowStockThreshold) {
        try (PreparedStatement ps = c().prepareStatement(
                "UPDATE resources SET name=?, unit=?, low_stock_threshold=? WHERE id=?")) {
            ps.setString(1, name);
            ps.setString(2, unit);
            ps.setInt(3, lowStockThreshold);
            ps.setLong(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update resource (is the name unique?)", e);
        }
    }

    public void setActive(long id, boolean active) {
        try (PreparedStatement ps = c().prepareStatement("UPDATE resources SET active=? WHERE id=?")) {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not change resource status", e);
        }
    }
}
