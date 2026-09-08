package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.StockView;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventoryRepository {

    private static final String BASE_SELECT = """
            SELECT c.id AS center_id, c.name AS center_name,
                   r.id AS resource_id, r.name AS resource_name,
                   i.quantity, r.low_stock_threshold
            FROM inventory i
            JOIN relief_centers c ON c.id = i.center_id
            JOIN resources r ON r.id = i.resource_id
            """;

    private Connection c() {
        return Database.getInstance().connection();
    }

    private StockView map(ResultSet rs) throws SQLException {
        return new StockView(rs.getLong("center_id"), rs.getString("center_name"),
                rs.getLong("resource_id"), rs.getString("resource_name"),
                rs.getInt("quantity"), rs.getInt("low_stock_threshold"));
    }

    private List<StockView> query(String sql, Object... params) {
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<StockView> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load inventory", e);
        }
    }

    public List<StockView> stockForCenter(long centerId) {
        return query(BASE_SELECT + " WHERE i.center_id = ? ORDER BY r.name", centerId);
    }

    /** Stock usable for allocation planning: positive quantity, active center and resource. */
    public List<StockView> availableStock() {
        return query(BASE_SELECT
                + " WHERE i.quantity > 0 AND c.active = 1 AND r.active = 1 ORDER BY c.name, r.name");
    }

    /** Low-stock report: at or below the resource's threshold, in active centers. */
    public List<StockView> lowStock() {
        return query(BASE_SELECT
                + " WHERE i.quantity <= r.low_stock_threshold AND c.active = 1 AND r.active = 1"
                + " ORDER BY i.quantity, c.name");
    }

    public void upsertQuantity(long centerId, long resourceId, int quantity) {
        String sql = """
                INSERT INTO inventory(center_id, resource_id, quantity) VALUES (?,?,?)
                ON CONFLICT(center_id, resource_id) DO UPDATE SET quantity = excluded.quantity
                """;
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, centerId);
            ps.setLong(2, resourceId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not set stock quantity", e);
        }
    }

    public void adjust(long centerId, long resourceId, int delta) {
        String sql = """
                UPDATE inventory SET quantity = quantity + ?
                WHERE center_id = ? AND resource_id = ? AND quantity + ? >= 0
                """;
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, centerId);
            ps.setLong(3, resourceId);
            ps.setInt(4, delta);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                if (delta > 0) {
                    upsertQuantity(centerId, resourceId, delta);
                } else {
                    throw new IllegalStateException("Adjustment would make the stock negative");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not adjust stock", e);
        }
    }

    /** Reserves stock for an allocation; fails if the stock changed underneath. */
    public void decrement(long centerId, long resourceId, int quantity) {
        String sql = "UPDATE inventory SET quantity = quantity - ?"
                + " WHERE center_id = ? AND resource_id = ? AND quantity >= ?";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, centerId);
            ps.setLong(3, resourceId);
            ps.setInt(4, quantity);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException(
                        "Stock changed while allocating; please preview the plan again");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not reserve stock", e);
        }
    }
}
