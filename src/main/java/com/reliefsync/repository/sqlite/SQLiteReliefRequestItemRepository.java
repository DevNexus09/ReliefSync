package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.ReliefRequestItem;
import com.reliefsync.repository.ReliefRequestItemRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteReliefRequestItemRepository implements ReliefRequestItemRepository {
  private final DatabaseManager databaseManager;

  public SQLiteReliefRequestItemRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<ReliefRequestItem> findById(long id) {
    return query("SELECT * FROM relief_request_items WHERE id=?", id).stream().findFirst();
  }

  public List<ReliefRequestItem> findByRequestId(long id) {
    return query("SELECT * FROM relief_request_items WHERE request_id=? ORDER BY id", id);
  }

  public long save(ReliefRequestItem v) {
    String sql =
        "INSERT INTO"
            + " relief_request_items(request_id,resource_id,requested_quantity,allocated_quantity,delivered_quantity)"
            + " VALUES(?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving request item");
    } catch (SQLException e) {
      throw fail("save request item", e);
    }
  }

  public void update(ReliefRequestItem v) {
    String sql =
        "UPDATE relief_request_items SET"
            + " request_id=?,resource_id=?,requested_quantity=?,allocated_quantity=?,delivered_quantity=?"
            + " WHERE id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Request item was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update request item", e);
    }
  }

  private List<ReliefRequestItem> query(String sql, long value) {
    List<ReliefRequestItem> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setLong(1, value);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) rows.add(map(r));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find request items", e);
    }
  }

  private void bind(PreparedStatement s, ReliefRequestItem v, boolean id) throws SQLException {
    s.setLong(1, v.requestId());
    s.setLong(2, v.resourceId());
    s.setLong(3, v.requestedQuantity());
    s.setLong(4, v.allocatedQuantity());
    s.setLong(5, v.deliveredQuantity());
    if (id) s.setLong(6, v.id());
  }

  private ReliefRequestItem map(ResultSet r) throws SQLException {
    return new ReliefRequestItem(
        r.getLong("id"),
        r.getLong("request_id"),
        r.getLong("resource_id"),
        r.getLong("requested_quantity"),
        r.getLong("allocated_quantity"),
        r.getLong("delivered_quantity"));
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
