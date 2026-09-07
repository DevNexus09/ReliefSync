package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.AllocationItem;
import com.reliefsync.repository.AllocationItemRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteAllocationItemRepository implements AllocationItemRepository {
  private final DatabaseManager databaseManager;

  public SQLiteAllocationItemRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<AllocationItem> findById(long id) {
    return query("SELECT * FROM allocation_items WHERE id=?", id).stream().findFirst();
  }

  public List<AllocationItem> findByAllocationId(long id) {
    return query("SELECT * FROM allocation_items WHERE allocation_id=? ORDER BY id", id);
  }

  public long save(AllocationItem v) {
    String sql =
        "INSERT INTO"
            + " allocation_items(allocation_id,relief_center_id,resource_id,quantity,dispatched_quantity,delivered_quantity)"
            + " VALUES(?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving allocation item");
    } catch (SQLException e) {
      throw fail("save allocation item", e);
    }
  }

  public void update(AllocationItem v) {
    String sql =
        "UPDATE allocation_items SET"
            + " allocation_id=?,relief_center_id=?,resource_id=?,quantity=?,dispatched_quantity=?,delivered_quantity=?"
            + " WHERE id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Allocation item was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update allocation item", e);
    }
  }

  private List<AllocationItem> query(String sql, long id) {
    List<AllocationItem> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setLong(1, id);
      try (ResultSet r = s.executeQuery()) {
        while (r.next())
          rows.add(
              new AllocationItem(
                  r.getLong("id"),
                  r.getLong("allocation_id"),
                  r.getLong("relief_center_id"),
                  r.getLong("resource_id"),
                  r.getLong("quantity"),
                  r.getLong("dispatched_quantity"),
                  r.getLong("delivered_quantity")));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find allocation items", e);
    }
  }

  private void bind(PreparedStatement s, AllocationItem v, boolean id) throws SQLException {
    s.setLong(1, v.allocationId());
    s.setLong(2, v.reliefCenterId());
    s.setLong(3, v.resourceId());
    s.setLong(4, v.quantity());
    s.setLong(5, v.dispatchedQuantity());
    s.setLong(6, v.deliveredQuantity());
    if (id) s.setLong(7, v.id());
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
