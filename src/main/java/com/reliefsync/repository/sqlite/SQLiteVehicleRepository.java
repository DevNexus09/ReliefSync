package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.Vehicle;
import com.reliefsync.model.enums.VehicleStatus;
import com.reliefsync.repository.VehicleRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteVehicleRepository implements VehicleRepository {
  private final DatabaseManager databaseManager;

  public SQLiteVehicleRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<Vehicle> findById(long id) {
    return query("SELECT * FROM vehicles WHERE id=?", id, null).stream().findFirst();
  }

  public Optional<Vehicle> findByRegistrationNo(String value) {
    return query("SELECT * FROM vehicles WHERE registration_no=?", null, value).stream()
        .findFirst();
  }

  public List<Vehicle> findAll() {
    return query("SELECT * FROM vehicles ORDER BY registration_no", null, null);
  }

  public List<Vehicle> findByStatus(VehicleStatus status) {
    return query(
        "SELECT * FROM vehicles WHERE status=? ORDER BY registration_no", null, status.name());
  }

  public long save(Vehicle v) {
    String sql =
        "INSERT INTO vehicles(registration_no,type,capacity,status,relief_center_id,active)"
            + " VALUES(?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving vehicle");
    } catch (SQLException e) {
      throw fail("save vehicle", e);
    }
  }

  public void update(Vehicle v) {
    String sql =
        "UPDATE vehicles SET"
            + " registration_no=?,type=?,capacity=?,status=?,relief_center_id=?,active=? WHERE"
            + " id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Vehicle was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update vehicle", e);
    }
  }

  private List<Vehicle> query(String sql, Long number, String text) {
    List<Vehicle> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (number != null) s.setLong(1, number);
      else if (text != null) s.setString(1, text);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) rows.add(map(r));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find vehicles", e);
    }
  }

  private void bind(PreparedStatement s, Vehicle v, boolean id) throws SQLException {
    s.setString(1, v.registrationNo());
    s.setString(2, v.type());
    s.setLong(3, v.capacity());
    s.setString(4, v.status().name());
    SQLiteSupport.setNullableLong(s, 5, v.reliefCenterId());
    s.setInt(6, v.active() ? 1 : 0);
    if (id) s.setLong(7, v.id());
  }

  private Vehicle map(ResultSet r) throws SQLException {
    return new Vehicle(
        r.getLong("id"),
        r.getString("registration_no"),
        r.getString("type"),
        r.getLong("capacity"),
        VehicleStatus.valueOf(r.getString("status")),
        SQLiteSupport.nullableLong(r, "relief_center_id"),
        r.getInt("active") == 1);
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
