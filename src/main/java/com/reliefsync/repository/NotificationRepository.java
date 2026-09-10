package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.Notification;
import com.reliefsync.model.NotificationEventType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {
    private Connection c() { return Database.getInstance().connection(); }

    public boolean insert(long recipientId, NotificationEventType type, String eventKey,
                          String title, String message, Long requestId, String createdAt) {
        String sql = "INSERT INTO notifications(recipient_id,event_type,event_key,title,message,request_id,created_at)"
                + " VALUES (?,?,?,?,?,?,?) ON CONFLICT(recipient_id,event_key) DO NOTHING";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, recipientId);
            ps.setString(2, type.name());
            ps.setString(3, eventKey);
            ps.setString(4, title);
            ps.setString(5, message);
            if (requestId == null) ps.setNull(6, Types.INTEGER); else ps.setLong(6, requestId);
            ps.setString(7, createdAt);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create notification", e);
        }
    }

    public List<Notification> forUser(long recipientId) {
        return query("SELECT * FROM notifications WHERE recipient_id=? ORDER BY created_at DESC,id DESC",
                recipientId);
    }

    public List<Notification> unreadForUser(long recipientId) {
        return query("SELECT * FROM notifications WHERE recipient_id=? AND read_at IS NULL"
                + " ORDER BY created_at DESC,id DESC", recipientId);
    }

    public List<Notification> forRequest(long recipientId, long requestId) {
        return query("SELECT * FROM notifications WHERE recipient_id=? AND request_id=?"
                + " ORDER BY created_at DESC,id DESC", recipientId, requestId);
    }

    public List<Notification> byEventType(long recipientId, NotificationEventType type) {
        return query("SELECT * FROM notifications WHERE recipient_id=? AND event_type=?"
                + " ORDER BY created_at DESC,id DESC", recipientId, type.name());
    }

    public int unreadCount(long recipientId) {
        try (PreparedStatement ps = c().prepareStatement(
                "SELECT COUNT(*) FROM notifications WHERE recipient_id=? AND read_at IS NULL")) {
            ps.setLong(1, recipientId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count unread notifications", e);
        }
    }

    public boolean markRead(long recipientId, long notificationId, String readAt) {
        try (PreparedStatement ps = c().prepareStatement(
                "UPDATE notifications SET read_at=? WHERE id=? AND recipient_id=? AND read_at IS NULL")) {
            ps.setString(1, readAt);
            ps.setLong(2, notificationId);
            ps.setLong(3, recipientId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not mark notification as read", e);
        }
    }

    public int markAllRead(long recipientId, String readAt) {
        try (PreparedStatement ps = c().prepareStatement(
                "UPDATE notifications SET read_at=? WHERE recipient_id=? AND read_at IS NULL")) {
            ps.setString(1, readAt);
            ps.setLong(2, recipientId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not mark notifications as read", e);
        }
    }

    /** Returns a new transition number only when the warning changes inactive -> active. */
    public Integer activateLowStock(long centerId, long resourceId, String changedAt) {
        String sql = """
                INSERT INTO low_stock_alert_state(center_id,resource_id,active,transition_no,last_changed_at)
                VALUES (?,?,1,1,?)
                ON CONFLICT(center_id,resource_id) DO UPDATE SET
                  active=1, transition_no=transition_no+1, last_changed_at=excluded.last_changed_at
                WHERE active=0
                RETURNING transition_no
                """;
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, centerId);
            ps.setLong(2, resourceId);
            ps.setString(3, changedAt);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update low-stock warning state", e);
        }
    }

    public void clearLowStock(long centerId, long resourceId, String changedAt) {
        String sql = "UPDATE low_stock_alert_state SET active=0,last_changed_at=?"
                + " WHERE center_id=? AND resource_id=? AND active=1";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, changedAt);
            ps.setLong(2, centerId);
            ps.setLong(3, resourceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not re-arm low-stock warning", e);
        }
    }

    public void rememberLowStockActive(long centerId, long resourceId, String changedAt) {
        String sql = "INSERT INTO low_stock_alert_state(center_id,resource_id,active,transition_no,last_changed_at)"
                + " VALUES (?,?,1,0,?) ON CONFLICT(center_id,resource_id) DO NOTHING";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setLong(1, centerId);
            ps.setLong(2, resourceId);
            ps.setString(3, changedAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not synchronize low-stock warning state", e);
        }
    }

    private List<Notification> query(String sql, Object... values) {
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<Notification> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load notifications", e);
        }
    }

    private static Notification map(ResultSet rs) throws SQLException {
        long requestId = rs.getLong("request_id");
        return new Notification(rs.getLong("id"), rs.getLong("recipient_id"),
                NotificationEventType.valueOf(rs.getString("event_type")), rs.getString("event_key"),
                rs.getString("title"), rs.getString("message"), rs.wasNull() ? null : requestId,
                rs.getString("created_at"), rs.getString("read_at"));
    }
}
