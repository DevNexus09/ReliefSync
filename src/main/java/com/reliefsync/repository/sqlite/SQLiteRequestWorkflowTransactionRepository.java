package com.reliefsync.repository.sqlite;

import com.reliefsync.database.TransactionManager;
import com.reliefsync.exception.AuthorizationException;
import com.reliefsync.exception.BusinessRuleException;
import com.reliefsync.exception.NotFoundException;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.exception.ValidationException;
import com.reliefsync.model.*;
import com.reliefsync.model.enums.*;
import com.reliefsync.repository.*;
import java.sql.*;
import java.util.*;

public final class SQLiteRequestWorkflowTransactionRepository
    implements RequestWorkflowTransactionRepository {
  private final TransactionManager transactions;

  public SQLiteRequestWorkflowTransactionRepository(TransactionManager transactions) {
    this.transactions = transactions;
  }

  public <T> T execute(Work<T> work) {
    try {
      return transactions.execute(c -> work.execute(new Context(c)));
    } catch (PersistenceException e) {
      if (e.getCause() instanceof ValidationException x) throw x;
      if (e.getCause() instanceof BusinessRuleException x) throw x;
      if (e.getCause() instanceof NotFoundException x) throw x;
      if (e.getCause() instanceof AuthorizationException x) throw x;
      throw e;
    }
  }

  private static final class Context implements RequestWorkflowUnitOfWork {
    private final Connection c;

    Context(Connection c) {
      this.c = c;
    }

    public Optional<ReliefRequest> findRequest(long id) {
      try (PreparedStatement s = c.prepareStatement("SELECT * FROM relief_requests WHERE id=?")) {
        s.setLong(1, id);
        try (ResultSet r = s.executeQuery()) {
          return r.next() ? Optional.of(request(r)) : Optional.empty();
        }
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    public Optional<AffectedArea> findArea(long id) {
      try (PreparedStatement s = c.prepareStatement("SELECT * FROM affected_areas WHERE id=?")) {
        s.setLong(1, id);
        try (ResultSet r = s.executeQuery()) {
          return r.next() ? Optional.of(area(r)) : Optional.empty();
        }
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    public List<ReliefRequestItem> findItems(long id) {
      List<ReliefRequestItem> out = new ArrayList<>();
      try (PreparedStatement s =
          c.prepareStatement("SELECT * FROM relief_request_items WHERE request_id=? ORDER BY id")) {
        s.setLong(1, id);
        try (ResultSet r = s.executeQuery()) {
          while (r.next()) out.add(item(r));
        }
        return out;
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    public List<VerificationRecord> findVerifications(long id, int round) {
      List<VerificationRecord> out = new ArrayList<>();
      try (PreparedStatement s =
          c.prepareStatement(
              "SELECT * FROM verification_records WHERE request_id=? AND verification_round=? ORDER"
                  + " BY id")) {
        s.setLong(1, id);
        s.setInt(2, round);
        try (ResultSet r = s.executeQuery()) {
          while (r.next()) out.add(record(r));
        }
        return out;
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    public long saveRequest(ReliefRequest v) {
      String sql =
          "INSERT INTO"
              + " relief_requests(disaster_event_id,affected_area_id,requested_by,priority,state,description,submitted_at,verified_at,verification_round,created_at,updated_at)"
              + " VALUES(?,?,?,?,?,?,?,?,?,?,?)";
      try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        bindRequest(s, v);
        s.executeUpdate();
        return SQLiteSupport.generatedId(s, "Saving relief request");
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    public void updateRequest(ReliefRequest v) {
      String sql =
          "UPDATE relief_requests SET"
              + " disaster_event_id=?,affected_area_id=?,requested_by=?,priority=?,state=?,description=?,submitted_at=?,verified_at=?,verification_round=?,created_at=?,updated_at=?"
              + " WHERE id=?";
      try (PreparedStatement s = c.prepareStatement(sql)) {
        bindRequest(s, v);
        s.setLong(12, v.id());
        if (s.executeUpdate() == 0)
          throw new PersistenceException("Relief request was not found: " + v.id());
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    public void saveItems(long id, List<ReliefRequestItem> items) {
      for (ReliefRequestItem v : items) insertItem(id, v);
    }

    public void replaceItems(long id, List<ReliefRequestItem> items) {
      try (PreparedStatement s =
          c.prepareStatement("DELETE FROM relief_request_items WHERE request_id=?")) {
        s.setLong(1, id);
        s.executeUpdate();
      } catch (SQLException e) {
        throw fail(e);
      }
      saveItems(id, items);
    }

    public long saveVerification(VerificationRecord v) {
      String sql =
          "INSERT INTO"
              + " verification_records(request_id,level,reviewer_id,decision,reason,verification_round,created_at)"
              + " VALUES(?,?,?,?,?,?,?)";
      try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        s.setLong(1, v.requestId());
        s.setString(2, v.level());
        s.setLong(3, v.reviewerId());
        s.setString(4, v.decision().name());
        s.setString(5, v.reason());
        s.setInt(6, v.verificationRound());
        s.setString(7, v.createdAt().toString());
        s.executeUpdate();
        return SQLiteSupport.generatedId(s, "Saving verification record");
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    private void insertItem(long id, ReliefRequestItem v) {
      String sql =
          "INSERT INTO"
              + " relief_request_items(request_id,resource_id,requested_quantity,allocated_quantity,delivered_quantity)"
              + " VALUES(?,?,?,?,?)";
      try (PreparedStatement s = c.prepareStatement(sql)) {
        s.setLong(1, id);
        s.setLong(2, v.resourceId());
        s.setLong(3, v.requestedQuantity());
        s.setLong(4, v.allocatedQuantity());
        s.setLong(5, v.deliveredQuantity());
        s.executeUpdate();
      } catch (SQLException e) {
        throw fail(e);
      }
    }

    private static void bindRequest(PreparedStatement s, ReliefRequest v) throws SQLException {
      s.setLong(1, v.disasterEventId());
      s.setLong(2, v.affectedAreaId());
      s.setLong(3, v.requestedBy());
      s.setString(4, v.priority().name());
      s.setString(5, v.state().name());
      s.setString(6, v.description());
      s.setString(7, v.submittedAt() == null ? null : v.submittedAt().toString());
      s.setString(8, v.verifiedAt() == null ? null : v.verifiedAt().toString());
      s.setInt(9, v.verificationRound());
      s.setString(10, v.createdAt().toString());
      s.setString(11, v.updatedAt().toString());
    }

    private static ReliefRequest request(ResultSet r) throws SQLException {
      return new ReliefRequest(
          r.getLong("id"),
          r.getLong("disaster_event_id"),
          r.getLong("affected_area_id"),
          r.getLong("requested_by"),
          RequestPriority.valueOf(r.getString("priority")),
          RequestStateType.valueOf(r.getString("state")),
          r.getString("description"),
          SQLiteSupport.dateTime(r, "submitted_at"),
          SQLiteSupport.dateTime(r, "verified_at"),
          r.getInt("verification_round"),
          SQLiteSupport.dateTime(r, "created_at"),
          SQLiteSupport.dateTime(r, "updated_at"));
    }

    private static ReliefRequestItem item(ResultSet r) throws SQLException {
      return new ReliefRequestItem(
          r.getLong("id"),
          r.getLong("request_id"),
          r.getLong("resource_id"),
          r.getLong("requested_quantity"),
          r.getLong("allocated_quantity"),
          r.getLong("delivered_quantity"));
    }

    private static AffectedArea area(ResultSet r) throws SQLException {
      return new AffectedArea(
          r.getLong("id"),
          r.getLong("disaster_event_id"),
          r.getString("name"),
          r.getString("district"),
          SQLiteSupport.nullableDouble(r, "latitude"),
          SQLiteSupport.nullableDouble(r, "longitude"),
          r.getLong("population_affected"),
          r.getLong("families_affected"),
          com.reliefsync.model.enums.Severity.valueOf(r.getString("severity")),
          com.reliefsync.model.enums.Accessibility.valueOf(r.getString("accessibility")),
          com.reliefsync.model.enums.MedicalUrgency.valueOf(r.getString("medical_urgency")),
          r.getString("water_access"),
          r.getString("status"),
          r.getString("notes"),
          SQLiteSupport.dateTime(r, "created_at"),
          SQLiteSupport.dateTime(r, "updated_at"));
    }

    private static VerificationRecord record(ResultSet r) throws SQLException {
      return new VerificationRecord(
          r.getLong("id"),
          r.getLong("request_id"),
          r.getString("level"),
          r.getLong("reviewer_id"),
          VerificationDecision.valueOf(r.getString("decision")),
          r.getString("reason"),
          r.getInt("verification_round"),
          SQLiteSupport.dateTime(r, "created_at"));
    }

    private static PersistenceException fail(SQLException e) {
      return new PersistenceException("Request workflow transaction failed.", e);
    }
  }
}
