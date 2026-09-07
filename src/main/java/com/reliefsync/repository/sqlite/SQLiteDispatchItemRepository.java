package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.DispatchItem;
import com.reliefsync.repository.DispatchItemRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteDispatchItemRepository implements DispatchItemRepository {
  private final DatabaseManager databaseManager;

  public SQLiteDispatchItemRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<DispatchItem> findById(long id) {
    return query("SELECT * FROM dispatch_items WHERE id=?", id).stream().findFirst();
  }

  public List<DispatchItem> findByDispatchId(long id) {
    return query("SELECT * FROM dispatch_items WHERE dispatch_id=? ORDER BY id", id);
  }

  public long save(DispatchItem v) {
    String sql =
        "INSERT INTO"
            + " dispatch_items(dispatch_id,allocation_item_id,resource_id,quantity_dispatched,quantity_delivered)"
            + " VALUES(?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving dispatch item");
    } catch (SQLException e) {
      throw fail("save dispatch item", e);
    }
  }

  public void update(DispatchItem v) {
    String sql =
        "UPDATE dispatch_items SET"
            + " dispatch_id=?,allocation_item_id=?,resource_id=?,quantity_dispatched=?,quantity_delivered=?"
            + " WHERE id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Dispatch item was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update dispatch item", e);
    }
  }

  private List<DispatchItem> query(String sql, long id) {
    List<DispatchItem> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setLong(1, id);
      try (ResultSet r = s.executeQuery()) {
        while (r.next())
          rows.add(
              new DispatchItem(
                  r.getLong("id"),
                  r.getLong("dispatch_id"),
                  r.getLong("allocation_item_id"),
                  r.getLong("resource_id"),
                  r.getLong("quantity_dispatched"),
                  r.getLong("quantity_delivered")));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find dispatch items", e);
    }
  }

  private void bind(PreparedStatement s, DispatchItem v, boolean id) throws SQLException {
    s.setLong(1, v.dispatchId());
    s.setLong(2, v.allocationItemId());
    s.setLong(3, v.resourceId());
    s.setLong(4, v.quantityDispatched());
    s.setLong(5, v.quantityDelivered());
    if (id) s.setLong(6, v.id());
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
