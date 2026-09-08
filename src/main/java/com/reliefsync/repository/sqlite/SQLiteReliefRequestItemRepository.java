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

  public void saveAll(long requestId, List<ReliefRequestItem> items) {
    try (Connection c = databaseManager.openConnection()) {
      c.setAutoCommit(false);
      try {
        for (ReliefRequestItem item : items)
          insert(
              c,
              new ReliefRequestItem(
                  0,
                  requestId,
                  item.resourceId(),
                  item.requestedQuantity(),
                  item.allocatedQuantity(),
                  item.deliveredQuantity()));
        c.commit();
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw fail("save request items", e);
    }
  }

  public void replaceItems(long requestId, List<ReliefRequestItem> items) {
    try (Connection c = databaseManager.openConnection()) {
      c.setAutoCommit(false);
      try (PreparedStatement d =
          c.prepareStatement("DELETE FROM relief_request_items WHERE request_id=?")) {
        d.setLong(1, requestId);
        d.executeUpdate();
        for (ReliefRequestItem item : items)
          insert(
              c,
              new ReliefRequestItem(
                  0,
                  requestId,
                  item.resourceId(),
                  item.requestedQuantity(),
                  item.allocatedQuantity(),
                  item.deliveredQuantity()));
        c.commit();
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw fail("replace request items", e);
    }
  }

  public Map<Long, Set<Long>> findResourceIdsByRequestIds(Collection<Long> ids) {
    if (ids.isEmpty()) return Map.of();
    String marks = String.join(",", Collections.nCopies(ids.size(), "?"));
    String sql =
        "SELECT request_id,resource_id FROM relief_request_items WHERE request_id IN ("
            + marks
            + ")";
    Map<Long, Set<Long>> result = new HashMap<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      int index = 1;
      for (Long id : ids) s.setLong(index++, id);
      try (ResultSet r = s.executeQuery()) {
        while (r.next())
          result.computeIfAbsent(r.getLong(1), ignored -> new HashSet<>()).add(r.getLong(2));
      }
      return result;
    } catch (SQLException e) {
      throw fail("find request resource IDs", e);
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

  private long insert(Connection c, ReliefRequestItem v) throws SQLException {
    String sql =
        "INSERT INTO"
            + " relief_request_items(request_id,resource_id,requested_quantity,allocated_quantity,delivered_quantity)"
            + " VALUES(?,?,?,?,?)";
    try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving request item");
    }
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
