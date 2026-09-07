package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.Dispatch;
import com.reliefsync.model.enums.DispatchStatus;
import com.reliefsync.repository.DispatchRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteDispatchRepository implements DispatchRepository {
  private final DatabaseManager databaseManager;

  public SQLiteDispatchRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<Dispatch> findById(long id) {
    return query("SELECT * FROM dispatches WHERE id=?", id, null).stream().findFirst();
  }

  public List<Dispatch> findByAllocationId(long id) {
    return query(
        "SELECT * FROM dispatches WHERE allocation_id=? ORDER BY created_at DESC", id, null);
  }

  public List<Dispatch> findByStatus(DispatchStatus status) {
    return query(
        "SELECT * FROM dispatches WHERE status=? ORDER BY created_at DESC", null, status.name());
  }

  public List<Dispatch> findAll() {
    return query("SELECT * FROM dispatches ORDER BY created_at DESC", null, null);
  }

  public long save(Dispatch v) {
    String sql =
        "INSERT INTO"
            + " dispatches(allocation_id,vehicle_id,source_center_id,destination_area_id,status,created_at,dispatched_at,delivered_at,failure_reason)"
            + " VALUES(?,?,?,?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving dispatch");
    } catch (SQLException e) {
      throw fail("save dispatch", e);
    }
  }

  public void update(Dispatch v) {
    String sql =
        "UPDATE dispatches SET"
            + " allocation_id=?,vehicle_id=?,source_center_id=?,destination_area_id=?,status=?,created_at=?,dispatched_at=?,delivered_at=?,failure_reason=?"
            + " WHERE id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Dispatch was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update dispatch", e);
    }
  }

  private List<Dispatch> query(String sql, Long number, String text) {
    List<Dispatch> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (number != null) s.setLong(1, number);
      else if (text != null) s.setString(1, text);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) rows.add(map(r));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find dispatches", e);
    }
  }

  private void bind(PreparedStatement s, Dispatch v, boolean id) throws SQLException {
    s.setLong(1, v.allocationId());
    s.setLong(2, v.vehicleId());
    s.setLong(3, v.sourceCenterId());
    s.setLong(4, v.destinationAreaId());
    s.setString(5, v.status().name());
    s.setString(6, v.createdAt().toString());
    s.setString(7, v.dispatchedAt() == null ? null : v.dispatchedAt().toString());
    s.setString(8, v.deliveredAt() == null ? null : v.deliveredAt().toString());
    s.setString(9, v.failureReason());
    if (id) s.setLong(10, v.id());
  }

  private Dispatch map(ResultSet r) throws SQLException {
    return new Dispatch(
        r.getLong("id"),
        r.getLong("allocation_id"),
        r.getLong("vehicle_id"),
        r.getLong("source_center_id"),
        r.getLong("destination_area_id"),
        DispatchStatus.valueOf(r.getString("status")),
        SQLiteSupport.dateTime(r, "created_at"),
        SQLiteSupport.dateTime(r, "dispatched_at"),
        SQLiteSupport.dateTime(r, "delivered_at"),
        r.getString("failure_reason"));
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
