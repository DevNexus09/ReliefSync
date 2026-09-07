package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.Allocation;
import com.reliefsync.model.enums.AllocationStatus;
import com.reliefsync.repository.AllocationRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteAllocationRepository implements AllocationRepository {
  private final DatabaseManager databaseManager;

  public SQLiteAllocationRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<Allocation> findById(long id) {
    return query("SELECT * FROM allocations WHERE id=?", id).stream().findFirst();
  }

  public List<Allocation> findByRequestId(long id) {
    return query("SELECT * FROM allocations WHERE request_id=? ORDER BY created_at DESC", id);
  }

  public List<Allocation> findAll() {
    return query("SELECT * FROM allocations ORDER BY created_at DESC", null);
  }

  public long save(Allocation v) {
    String sql =
        "INSERT INTO"
            + " allocations(request_id,strategy_type,status,created_by,created_at,confirmed_at,cancelled_at)"
            + " VALUES(?,?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving allocation");
    } catch (SQLException e) {
      throw fail("save allocation", e);
    }
  }

  public void update(Allocation v) {
    String sql =
        "UPDATE allocations SET"
            + " request_id=?,strategy_type=?,status=?,created_by=?,created_at=?,confirmed_at=?,cancelled_at=?"
            + " WHERE id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Allocation was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update allocation", e);
    }
  }

  private List<Allocation> query(String sql, Long id) {
    List<Allocation> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (id != null) s.setLong(1, id);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) rows.add(map(r));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find allocations", e);
    }
  }

  private void bind(PreparedStatement s, Allocation v, boolean id) throws SQLException {
    s.setLong(1, v.requestId());
    s.setString(2, v.strategyType());
    s.setString(3, v.status().name());
    s.setLong(4, v.createdBy());
    s.setString(5, v.createdAt().toString());
    s.setString(6, v.confirmedAt() == null ? null : v.confirmedAt().toString());
    s.setString(7, v.cancelledAt() == null ? null : v.cancelledAt().toString());
    if (id) s.setLong(8, v.id());
  }

  private Allocation map(ResultSet r) throws SQLException {
    return new Allocation(
        r.getLong("id"),
        r.getLong("request_id"),
        r.getString("strategy_type"),
        AllocationStatus.valueOf(r.getString("status")),
        r.getLong("created_by"),
        SQLiteSupport.dateTime(r, "created_at"),
        SQLiteSupport.dateTime(r, "confirmed_at"),
        SQLiteSupport.dateTime(r, "cancelled_at"));
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
