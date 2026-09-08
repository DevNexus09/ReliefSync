package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.enums.RequestPriority;
import com.reliefsync.model.enums.RequestStateType;
import com.reliefsync.model.search.ReliefRequestSearchCriteria;
import com.reliefsync.repository.ReliefRequestRepository;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public final class SQLiteReliefRequestRepository implements ReliefRequestRepository {
  private final DatabaseManager databaseManager;

  public SQLiteReliefRequestRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<ReliefRequest> findById(long id) {
    return query("SELECT * FROM relief_requests WHERE id=?", id, null).stream().findFirst();
  }

  public List<ReliefRequest> findAll() {
    return query("SELECT * FROM relief_requests ORDER BY created_at DESC", null, null);
  }

  public List<ReliefRequest> findByDisasterEventId(long id) {
    return query(
        "SELECT * FROM relief_requests WHERE disaster_event_id=? ORDER BY created_at DESC",
        id,
        null);
  }

  public List<ReliefRequest> findByAffectedAreaId(long id) {
    return query(
        "SELECT * FROM relief_requests WHERE affected_area_id=? ORDER BY created_at DESC",
        id,
        null);
  }

  public List<ReliefRequest> findByState(RequestStateType state) {
    return query(
        "SELECT * FROM relief_requests WHERE state=? ORDER BY created_at DESC", null, state.name());
  }

  public List<ReliefRequest> search(ReliefRequestSearchCriteria criteria, int limit) {
    Objects.requireNonNull(criteria);
    SqlSearch q = new SqlSearch("SELECT * FROM relief_requests WHERE 1=1");
    q.add(" AND disaster_event_id=?", criteria.disasterEventId());
    q.add(" AND affected_area_id=?", criteria.affectedAreaId());
    q.add(" AND priority=?", criteria.priority() == null ? null : criteria.priority().name());
    q.add(" AND state=?", criteria.state() == null ? null : criteria.state().name());
    q.add(
        " AND date(submitted_at)=?",
        criteria.submittedOn() == null ? null : criteria.submittedOn().toString());
    try (Connection c = databaseManager.openConnection()) {
      return q.execute(c, " ORDER BY submitted_at DESC", limit, this::map);
    } catch (SQLException e) {
      throw fail("search relief requests", e);
    }
  }

  public List<ReliefRequest> findPotentialDuplicates(
      long event, long area, LocalDateTime after, int limit) {
    SqlSearch q =
        new SqlSearch(
            "SELECT * FROM relief_requests WHERE disaster_event_id=? AND affected_area_id=? AND"
                + " submitted_at>=? AND state NOT IN ('DELIVERED','REJECTED','CANCELLED')");
    q.add("", event);
    q.add("", area);
    q.add("", after.toString());
    try (Connection c = databaseManager.openConnection()) {
      return q.execute(c, " ORDER BY submitted_at DESC", limit, this::map);
    } catch (SQLException e) {
      throw fail("find duplicate request candidates", e);
    }
  }

  public long save(ReliefRequest v) {
    String sql =
        "INSERT INTO"
            + " relief_requests(disaster_event_id,affected_area_id,requested_by,priority,state,description,submitted_at,verified_at,verification_round,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving relief request");
    } catch (SQLException e) {
      throw fail("save relief request", e);
    }
  }

  public void update(ReliefRequest v) {
    String sql =
        "UPDATE relief_requests SET"
            + " disaster_event_id=?,affected_area_id=?,requested_by=?,priority=?,state=?,description=?,submitted_at=?,verified_at=?,verification_round=?,created_at=?,updated_at=?"
            + " WHERE id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Relief request was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update relief request", e);
    }
  }

  public void updateState(long id, RequestStateType state, LocalDateTime updatedAt) {
    executeUpdate(
        "UPDATE relief_requests SET state=?,updated_at=? WHERE id=?",
        state.name(),
        updatedAt.toString(),
        id);
  }

  public void updateVerificationRound(
      long id, int round, LocalDateTime submittedAt, LocalDateTime updatedAt) {
    String sql =
        "UPDATE relief_requests SET"
            + " verification_round=?,submitted_at=?,verified_at=NULL,updated_at=? WHERE id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setInt(1, round);
      s.setString(2, submittedAt.toString());
      s.setString(3, updatedAt.toString());
      s.setLong(4, id);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Relief request was not found: " + id);
    } catch (SQLException e) {
      throw fail("update verification round", e);
    }
  }

  private void executeUpdate(String sql, String first, String second, long id) {
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setString(1, first);
      s.setString(2, second);
      s.setLong(3, id);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Relief request was not found: " + id);
    } catch (SQLException e) {
      throw fail("update request state", e);
    }
  }

  private List<ReliefRequest> query(String sql, Long number, String text) {
    List<ReliefRequest> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (number != null) s.setLong(1, number);
      else if (text != null) s.setString(1, text);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) rows.add(map(r));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find relief requests", e);
    }
  }

  private void bind(PreparedStatement s, ReliefRequest v, boolean id) throws SQLException {
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
    if (id) s.setLong(12, v.id());
  }

  private ReliefRequest map(ResultSet r) throws SQLException {
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

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
