package com.reliefsync.repository.sqlite;

import com.reliefsync.database.TransactionManager;
import com.reliefsync.exception.BusinessRuleException;
import com.reliefsync.exception.NotFoundException;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.exception.ValidationException;
import com.reliefsync.model.AuditEvent;
import com.reliefsync.model.CenterInventory;
import com.reliefsync.repository.InventoryTransactionRepository;
import com.reliefsync.repository.InventoryUnitOfWork;
import java.sql.*;
import java.util.Optional;

public final class SQLiteInventoryTransactionRepository implements InventoryTransactionRepository {
  private final TransactionManager transactions;

  public SQLiteInventoryTransactionRepository(TransactionManager transactions) {
    this.transactions = transactions;
  }

  public <T> T execute(InventoryWork<T> work) {
    try {
      return transactions.execute(c -> work.execute(new Context(c)));
    } catch (PersistenceException exception) {
      if (exception.getCause() instanceof ValidationException validation) throw validation;
      if (exception.getCause() instanceof BusinessRuleException businessRule) throw businessRule;
      if (exception.getCause() instanceof NotFoundException notFound) throw notFound;
      throw exception;
    }
  }

  private static final class Context implements InventoryUnitOfWork {
    private final Connection connection;

    Context(Connection connection) {
      this.connection = connection;
    }

    public Optional<CenterInventory> find(long center, long resource) {
      try (PreparedStatement s =
          connection.prepareStatement(
              "SELECT * FROM center_inventory WHERE relief_center_id=? AND resource_id=?")) {
        s.setLong(1, center);
        s.setLong(2, resource);
        try (ResultSet r = s.executeQuery()) {
          return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    public long save(CenterInventory v) {
      String sql =
          "INSERT INTO"
              + " center_inventory(relief_center_id,resource_id,total_quantity,reserved_quantity,dispatched_quantity,updated_at)"
              + " VALUES(?,?,?,?,?,?)";
      try (PreparedStatement s =
          connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        bind(s, v);
        s.executeUpdate();
        return SQLiteSupport.generatedId(s, "Saving inventory");
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    public void update(CenterInventory v) {
      String sql =
          "UPDATE center_inventory SET"
              + " total_quantity=?,reserved_quantity=?,dispatched_quantity=?,updated_at=? WHERE"
              + " id=?";
      try (PreparedStatement s = connection.prepareStatement(sql)) {
        s.setLong(1, v.totalQuantity());
        s.setLong(2, v.reservedQuantity());
        s.setLong(3, v.dispatchedQuantity());
        s.setString(4, v.updatedAt().toString());
        s.setLong(5, v.id());
        if (s.executeUpdate() == 0)
          throw new PersistenceException("Inventory was not found: " + v.id());
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    public long saveAudit(AuditEvent v) {
      String sql =
          "INSERT INTO"
              + " audit_events(actor_user_id,event_type,entity_type,entity_id,description,created_at)"
              + " VALUES(?,?,?,?,?,?)";
      try (PreparedStatement s =
          connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        SQLiteSupport.setNullableLong(s, 1, v.actorUserId());
        s.setString(2, v.eventType());
        s.setString(3, v.entityType());
        SQLiteSupport.setNullableLong(s, 4, v.entityId());
        s.setString(5, v.description());
        s.setString(6, v.createdAt().toString());
        s.executeUpdate();
        return SQLiteSupport.generatedId(s, "Saving inventory audit");
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    private static void bind(PreparedStatement s, CenterInventory v) throws SQLException {
      s.setLong(1, v.reliefCenterId());
      s.setLong(2, v.resourceId());
      s.setLong(3, v.totalQuantity());
      s.setLong(4, v.reservedQuantity());
      s.setLong(5, v.dispatchedQuantity());
      s.setString(6, v.updatedAt().toString());
    }

    private static CenterInventory map(ResultSet r) throws SQLException {
      return new CenterInventory(
          r.getLong("id"),
          r.getLong("relief_center_id"),
          r.getLong("resource_id"),
          r.getLong("total_quantity"),
          r.getLong("reserved_quantity"),
          r.getLong("dispatched_quantity"),
          SQLiteSupport.dateTime(r, "updated_at"));
    }

    private static PersistenceException fail(SQLException e) {
      return new PersistenceException("Inventory transaction failed.", e);
    }
  }
}
