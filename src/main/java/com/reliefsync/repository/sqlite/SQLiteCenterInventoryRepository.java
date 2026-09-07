package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.CenterInventory;
import com.reliefsync.repository.CenterInventoryRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteCenterInventoryRepository implements CenterInventoryRepository {
  private final DatabaseManager databaseManager;

  public SQLiteCenterInventoryRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<CenterInventory> findById(long id) {
    return query("SELECT * FROM center_inventory WHERE id=?", id, null).stream().findFirst();
  }

  public Optional<CenterInventory> findByCenterAndResource(long centerId, long resourceId) {
    return query(
            "SELECT * FROM center_inventory WHERE relief_center_id=? AND resource_id=?",
            centerId,
            resourceId)
        .stream()
        .findFirst();
  }

  public List<CenterInventory> findByReliefCenterId(long id) {
    return query(
        "SELECT * FROM center_inventory WHERE relief_center_id=? ORDER BY resource_id", id, null);
  }

  public List<CenterInventory> findByResourceId(long id) {
    return query(
        "SELECT * FROM center_inventory WHERE resource_id=? ORDER BY relief_center_id", id, null);
  }

  public List<CenterInventory> findAll() {
    return query(
        "SELECT * FROM center_inventory ORDER BY relief_center_id,resource_id", null, null);
  }

  public long save(CenterInventory v) {
    String sql =
        "INSERT INTO"
            + " center_inventory(relief_center_id,resource_id,total_quantity,reserved_quantity,dispatched_quantity,updated_at)"
            + " VALUES(?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving inventory");
    } catch (SQLException e) {
      throw fail("save inventory", e);
    }
  }

  public void update(CenterInventory v) {
    String sql =
        "UPDATE center_inventory SET"
            + " relief_center_id=?,resource_id=?,total_quantity=?,reserved_quantity=?,dispatched_quantity=?,updated_at=?"
            + " WHERE id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Inventory was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update inventory", e);
    }
  }

  private List<CenterInventory> query(String sql, Long first, Long second) {
    List<CenterInventory> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (first != null) s.setLong(1, first);
      if (second != null) s.setLong(2, second);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) rows.add(map(r));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find inventory", e);
    }
  }

  private void bind(PreparedStatement s, CenterInventory v, boolean id) throws SQLException {
    s.setLong(1, v.reliefCenterId());
    s.setLong(2, v.resourceId());
    s.setLong(3, v.totalQuantity());
    s.setLong(4, v.reservedQuantity());
    s.setLong(5, v.dispatchedQuantity());
    s.setString(6, v.updatedAt().toString());
    if (id) s.setLong(7, v.id());
  }

  private CenterInventory map(ResultSet r) throws SQLException {
    return new CenterInventory(
        r.getLong("id"),
        r.getLong("relief_center_id"),
        r.getLong("resource_id"),
        r.getLong("total_quantity"),
        r.getLong("reserved_quantity"),
        r.getLong("dispatched_quantity"),
        SQLiteSupport.dateTime(r, "updated_at"));
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
