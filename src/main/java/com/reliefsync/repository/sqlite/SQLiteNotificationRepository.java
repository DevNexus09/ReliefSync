package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.Notification;
import com.reliefsync.repository.NotificationRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteNotificationRepository implements NotificationRepository {
  private final DatabaseManager databaseManager;

  public SQLiteNotificationRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public List<Notification> findByUserId(long id) {
    return query("SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC", id);
  }

  public List<Notification> findUnreadByUserId(long id) {
    return query(
        "SELECT * FROM notifications WHERE user_id=? AND is_read=0 ORDER BY created_at DESC", id);
  }

  public long save(Notification v) {
    String sql =
        "INSERT INTO notifications(user_id,event_type,title,message,is_read,created_at)"
            + " VALUES(?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      s.setLong(1, v.userId());
      s.setString(2, v.eventType());
      s.setString(3, v.title());
      s.setString(4, v.message());
      s.setInt(5, v.read() ? 1 : 0);
      s.setString(6, v.createdAt().toString());
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving notification");
    } catch (SQLException e) {
      throw fail("save notification", e);
    }
  }

  public void markRead(long id) {
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement("UPDATE notifications SET is_read=1 WHERE id=?")) {
      s.setLong(1, id);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Notification was not found: " + id);
    } catch (SQLException e) {
      throw fail("mark notification read", e);
    }
  }

  private List<Notification> query(String sql, long id) {
    List<Notification> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setLong(1, id);
      try (ResultSet r = s.executeQuery()) {
        while (r.next())
          rows.add(
              new Notification(
                  r.getLong("id"),
                  r.getLong("user_id"),
                  r.getString("event_type"),
                  r.getString("title"),
                  r.getString("message"),
                  r.getInt("is_read") == 1,
                  SQLiteSupport.dateTime(r, "created_at")));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find notifications", e);
    }
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
