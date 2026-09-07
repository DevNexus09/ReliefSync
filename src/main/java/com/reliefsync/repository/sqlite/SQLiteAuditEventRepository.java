package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.AuditEvent;
import com.reliefsync.repository.AuditEventRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteAuditEventRepository implements AuditEventRepository {
  private final DatabaseManager databaseManager;

  public SQLiteAuditEventRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public long save(AuditEvent v) {
    String sql =
        "INSERT INTO"
            + " audit_events(actor_user_id,event_type,entity_type,entity_id,description,created_at)"
            + " VALUES(?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      SQLiteSupport.setNullableLong(s, 1, v.actorUserId());
      s.setString(2, v.eventType());
      s.setString(3, v.entityType());
      SQLiteSupport.setNullableLong(s, 4, v.entityId());
      s.setString(5, v.description());
      s.setString(6, v.createdAt().toString());
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving audit event");
    } catch (SQLException e) {
      throw fail("save audit event", e);
    }
  }

  public List<AuditEvent> findByEntity(String type, long id) {
    String sql =
        "SELECT * FROM audit_events WHERE entity_type=? AND entity_id=? ORDER BY created_at";
    List<AuditEvent> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setString(1, type);
      s.setLong(2, id);
      try (ResultSet r = s.executeQuery()) {
        while (r.next())
          rows.add(
              new AuditEvent(
                  r.getLong("id"),
                  SQLiteSupport.nullableLong(r, "actor_user_id"),
                  r.getString("event_type"),
                  r.getString("entity_type"),
                  SQLiteSupport.nullableLong(r, "entity_id"),
                  r.getString("description"),
                  SQLiteSupport.dateTime(r, "created_at")));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find audit events", e);
    }
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
