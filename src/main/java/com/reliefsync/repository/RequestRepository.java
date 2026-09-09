package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.AreaFulfillment;
import com.reliefsync.model.Priority;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.RequestRow;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.StatusChange;
import com.reliefsync.model.StatusCount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RequestRepository {

    private static final String ROW_SELECT = """
            SELECT q.id, a.name AS area_name, q.priority, q.status, q.created_at, q.note,
                   u.full_name AS created_by_name
            FROM relief_requests q
            JOIN affected_areas a ON a.id = q.area_id
            JOIN users u ON u.id = q.created_by
            """;

    private Connection c() {
        return Database.getInstance().connection();
    }

    private RequestRow mapRow(ResultSet rs) throws SQLException {
        return new RequestRow(rs.getLong("id"), rs.getString("area_name"),
                Priority.valueOf(rs.getString("priority")), RequestStatus.valueOf(rs.getString("status")),
                rs.getString("created_at"), rs.getString("created_by_name"), rs.getString("note"));
    }

    public long insert(long areaId, Priority priority, RequestStatus status, String note,
                       long createdBy, String createdAt) {
        String sql = "INSERT INTO relief_requests(area_id, priority, status, note, created_by, created_at)"
                + " VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, areaId);
            ps.setString(2, priority.name());
            ps.setString(3, status.name());
            ps.setString(4, note);
            ps.setLong(5, createdBy);
            ps.setString(6, createdAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create the request", e);
        }
    }

    public void insertItem(long requestId, long resourceId, int quantity) {
        String sql = "INSERT INTO request_items(request_id, resource_id, quantity_requested) VALUES (?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            ps.setLong(2, resourceId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save a request item", e);
        }
    }

    public Optional<ReliefRequest> findById(long id) {
        String sql = "SELECT * FROM relief_requests WHERE id = ?";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ReliefRequest(rs.getLong("id"), rs.getLong("area_id"),
                        Priority.valueOf(rs.getString("priority")),
                        RequestStatus.valueOf(rs.getString("status")),
                        rs.getString("note"), rs.getLong("created_by"), rs.getString("created_at")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load the request", e);
        }
    }

    public void updateStatus(long id, RequestStatus status) {
        try (PreparedStatement ps = c().prepareStatement("UPDATE relief_requests SET status=? WHERE id=?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update the request status", e);
        }
    }

    public List<RequestItem> items(long requestId) {
        String sql = """
                SELECT i.id, i.request_id, i.resource_id, r.name AS resource_name,
                       i.quantity_requested, i.quantity_allocated
                FROM request_items i JOIN resources r ON r.id = i.resource_id
                WHERE i.request_id = ? ORDER BY r.name
                """;
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                List<RequestItem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new RequestItem(rs.getLong("id"), rs.getLong("request_id"),
                            rs.getLong("resource_id"), rs.getString("resource_name"),
                            rs.getInt("quantity_requested"), rs.getInt("quantity_allocated")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load request items", e);
        }
    }

    public void addToItemAllocated(long requestId, long resourceId, int quantity) {
        String sql = "UPDATE request_items SET quantity_allocated = quantity_allocated + ?"
                + " WHERE request_id = ? AND resource_id = ?"
                + " AND quantity_allocated + ? BETWEEN 0 AND quantity_requested";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, requestId);
            ps.setLong(3, resourceId);
            ps.setInt(4, quantity);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Allocated quantity would be outside the requested range");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update allocated quantities", e);
        }
    }

    /** Bounded search over area name, filtered by status when given. */
    public List<RequestRow> rows(String search, RequestStatus statusOrNull) {
        StringBuilder sql = new StringBuilder(ROW_SELECT).append(" WHERE a.name LIKE ?");
        if (statusOrNull != null) {
            sql.append(" AND q.status = ?");
        }
        sql.append(" ORDER BY q.id DESC LIMIT 200");
        try (PreparedStatement ps = c().prepareStatement(sql.toString())) {
            ps.setString(1, "%" + search.trim() + "%");
            if (statusOrNull != null) {
                ps.setString(2, statusOrNull.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<RequestRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not search requests", e);
        }
    }

    public List<RequestRow> rowsByStatuses(List<RequestStatus> statuses) {
        String placeholders = String.join(",", statuses.stream().map(s -> "?").toList());
        String sql = ROW_SELECT + " WHERE q.status IN (" + placeholders + ") ORDER BY q.id DESC LIMIT 200";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            for (int i = 0; i < statuses.size(); i++) {
                ps.setString(i + 1, statuses.get(i).name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<RequestRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load requests", e);
        }
    }

    /** Open (not yet closed) requests for one area — used for duplicate warnings. */
    public int openRequestCountForArea(long areaId) {
        String sql = "SELECT COUNT(*) FROM relief_requests"
                + " WHERE area_id = ? AND status IN ('DRAFT','SUBMITTED','VERIFIED')";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, areaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not check for duplicates", e);
        }
    }

    public void addHistory(long requestId, String from, String to, String changedBy, String changedAt) {
        String sql = "INSERT INTO status_history(request_id, from_status, to_status, changed_by, changed_at)"
                + " VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            ps.setString(2, from);
            ps.setString(3, to);
            ps.setString(4, changedBy);
            ps.setString(5, changedAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record status history", e);
        }
    }

    public List<StatusChange> history(long requestId) {
        String sql = "SELECT from_status, to_status, changed_by, changed_at FROM status_history"
                + " WHERE request_id = ? ORDER BY id";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                List<StatusChange> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new StatusChange(rs.getString(1), rs.getString(2),
                            rs.getString(3), rs.getString(4)));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load status history", e);
        }
    }

    public List<StatusCount> statusSummary() {
        String sql = "SELECT status, COUNT(*) AS n FROM relief_requests GROUP BY status ORDER BY status";
        try (PreparedStatement ps = c().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<StatusCount> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new StatusCount(RequestStatus.valueOf(rs.getString("status")), rs.getInt("n")));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not build the status summary", e);
        }
    }

    public List<AreaFulfillment> areaFulfillment() {
        String sql = """
                SELECT a.name AS area_name,
                       COALESCE(SUM(i.quantity_requested), 0) AS requested,
                       COALESCE(SUM(i.quantity_allocated), 0) AS allocated
                FROM affected_areas a
                JOIN relief_requests q ON q.area_id = a.id AND q.status NOT IN ('CANCELLED','REJECTED')
                JOIN request_items i ON i.request_id = q.id
                GROUP BY a.id ORDER BY a.name
                """;
        try (PreparedStatement ps = c().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<AreaFulfillment> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new AreaFulfillment(rs.getString("area_name"),
                        rs.getInt("requested"), rs.getInt("allocated")));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not build the fulfillment report", e);
        }
    }
}
