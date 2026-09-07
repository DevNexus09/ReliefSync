package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.DisasterEvent;
import com.reliefsync.model.enums.DisasterStatus;
import com.reliefsync.model.enums.DisasterType;
import com.reliefsync.repository.DisasterEventRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteDisasterEventRepository implements DisasterEventRepository {
  private final DatabaseManager databaseManager;

  public SQLiteDisasterEventRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<DisasterEvent> findById(long id) {
    return one("SELECT * FROM disaster_events WHERE id=?", id);
  }

  public List<DisasterEvent> findAll() {
    return many("SELECT * FROM disaster_events ORDER BY start_date DESC", null);
  }

  public List<DisasterEvent> findByStatus(DisasterStatus status) {
    return many(
        "SELECT * FROM disaster_events WHERE status=? ORDER BY start_date DESC", status.name());
  }

  public long save(DisasterEvent event) {
    String sql =
        "INSERT INTO"
            + " disaster_events(name,type,description,start_date,end_date,status,created_by,created_at)"
            + " VALUES(?,?,?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, event, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving disaster event");
    } catch (SQLException e) {
      throw fail("save disaster event", e);
    }
  }

  public void update(DisasterEvent event) {
    String sql =
        "UPDATE disaster_events SET"
            + " name=?,type=?,description=?,start_date=?,end_date=?,status=?,created_by=?,created_at=?"
            + " WHERE id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, event, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Disaster event was not found: " + event.id());
    } catch (SQLException e) {
      throw fail("update disaster event", e);
    }
  }

  private Optional<DisasterEvent> one(String sql, long id) {
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setLong(1, id);
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? Optional.of(map(r)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw fail("find disaster event", e);
    }
  }

  private List<DisasterEvent> many(String sql, String value) {
    List<DisasterEvent> values = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (value != null) s.setString(1, value);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) values.add(map(r));
      }
      return values;
    } catch (SQLException e) {
      throw fail("find disaster events", e);
    }
  }

  private void bind(PreparedStatement s, DisasterEvent e, boolean id) throws SQLException {
    s.setString(1, e.name());
    s.setString(2, e.type().name());
    s.setString(3, e.description());
    s.setString(4, e.startDate().toString());
    s.setString(5, e.endDate() == null ? null : e.endDate().toString());
    s.setString(6, e.status().name());
    s.setLong(7, e.createdBy());
    s.setString(8, e.createdAt().toString());
    if (id) s.setLong(9, e.id());
  }

  private DisasterEvent map(ResultSet r) throws SQLException {
    return new DisasterEvent(
        r.getLong("id"),
        r.getString("name"),
        DisasterType.valueOf(r.getString("type")),
        r.getString("description"),
        SQLiteSupport.date(r, "start_date"),
        SQLiteSupport.date(r, "end_date"),
        DisasterStatus.valueOf(r.getString("status")),
        r.getLong("created_by"),
        SQLiteSupport.dateTime(r, "created_at"));
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
