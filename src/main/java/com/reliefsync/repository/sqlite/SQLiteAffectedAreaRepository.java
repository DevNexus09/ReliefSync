package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.AffectedArea;
import com.reliefsync.model.enums.Accessibility;
import com.reliefsync.model.enums.MedicalUrgency;
import com.reliefsync.model.enums.Severity;
import com.reliefsync.repository.AffectedAreaRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteAffectedAreaRepository implements AffectedAreaRepository {
  private final DatabaseManager databaseManager;

  public SQLiteAffectedAreaRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<AffectedArea> findById(long id) {
    List<AffectedArea> rows = query("SELECT * FROM affected_areas WHERE id=?", id);
    return rows.stream().findFirst();
  }

  public List<AffectedArea> findAll() {
    return query("SELECT * FROM affected_areas ORDER BY name", null);
  }

  public List<AffectedArea> findByDisasterEventId(long id) {
    return query("SELECT * FROM affected_areas WHERE disaster_event_id=? ORDER BY name", id);
  }

  public long save(AffectedArea v) {
    String sql =
        "INSERT INTO"
            + " affected_areas(disaster_event_id,name,district,latitude,longitude,population_affected,families_affected,severity,accessibility,medical_urgency,water_access,status,notes,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving affected area");
    } catch (SQLException e) {
      throw fail("save affected area", e);
    }
  }

  public void update(AffectedArea v) {
    String sql =
        "UPDATE affected_areas SET"
            + " disaster_event_id=?,name=?,district=?,latitude=?,longitude=?,population_affected=?,families_affected=?,severity=?,accessibility=?,medical_urgency=?,water_access=?,status=?,notes=?,created_at=?,updated_at=?"
            + " WHERE id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Affected area was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update affected area", e);
    }
  }

  private List<AffectedArea> query(String sql, Long value) {
    List<AffectedArea> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (value != null) s.setLong(1, value);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) rows.add(map(r));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find affected areas", e);
    }
  }

  private void bind(PreparedStatement s, AffectedArea v, boolean id) throws SQLException {
    s.setLong(1, v.disasterEventId());
    s.setString(2, v.name());
    s.setString(3, v.district());
    SQLiteSupport.setNullableDouble(s, 4, v.latitude());
    SQLiteSupport.setNullableDouble(s, 5, v.longitude());
    s.setLong(6, v.populationAffected());
    s.setLong(7, v.familiesAffected());
    s.setString(8, v.severity().name());
    s.setString(9, v.accessibility().name());
    s.setString(10, v.medicalUrgency().name());
    s.setString(11, v.waterAccess());
    s.setString(12, v.status());
    s.setString(13, v.notes());
    s.setString(14, v.createdAt().toString());
    s.setString(15, v.updatedAt().toString());
    if (id) s.setLong(16, v.id());
  }

  private AffectedArea map(ResultSet r) throws SQLException {
    return new AffectedArea(
        r.getLong("id"),
        r.getLong("disaster_event_id"),
        r.getString("name"),
        r.getString("district"),
        SQLiteSupport.nullableDouble(r, "latitude"),
        SQLiteSupport.nullableDouble(r, "longitude"),
        r.getLong("population_affected"),
        r.getLong("families_affected"),
        Severity.valueOf(r.getString("severity")),
        Accessibility.valueOf(r.getString("accessibility")),
        MedicalUrgency.valueOf(r.getString("medical_urgency")),
        r.getString("water_access"),
        r.getString("status"),
        r.getString("notes"),
        SQLiteSupport.dateTime(r, "created_at"),
        SQLiteSupport.dateTime(r, "updated_at"));
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
