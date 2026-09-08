package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.VerificationRecord;
import com.reliefsync.model.enums.VerificationDecision;
import com.reliefsync.model.verification.VerificationLevel;
import com.reliefsync.repository.VerificationRecordRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteVerificationRecordRepository implements VerificationRecordRepository {
  private final DatabaseManager databaseManager;

  public SQLiteVerificationRecordRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<VerificationRecord> findById(long id) {
    return query("SELECT * FROM verification_records WHERE id=?", id).stream().findFirst();
  }

  public List<VerificationRecord> findByRequestId(long id) {
    return query("SELECT * FROM verification_records WHERE request_id=? ORDER BY created_at", id);
  }

  public List<VerificationRecord> findByRequestAndRound(long id, int round) {
    return queryRound(
        "SELECT * FROM verification_records WHERE request_id=? AND verification_round=? ORDER BY"
            + " id",
        id,
        round);
  }

  public boolean existsForLevel(long id, int round, VerificationLevel level) {
    String sql =
        "SELECT 1 FROM verification_records WHERE request_id=? AND verification_round=? AND"
            + " level=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setLong(1, id);
      s.setInt(2, round);
      s.setString(3, level.name());
      try (ResultSet r = s.executeQuery()) {
        return r.next();
      }
    } catch (SQLException e) {
      throw fail("check verification level", e);
    }
  }

  public long save(VerificationRecord v) {
    String sql =
        "INSERT INTO"
            + " verification_records(request_id,level,reviewer_id,decision,reason,verification_round,created_at)"
            + " VALUES(?,?,?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
      throw fail("save verification record", e);
    }
  }

  private List<VerificationRecord> query(String sql, long value) {
    List<VerificationRecord> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setLong(1, value);
      try (ResultSet r = s.executeQuery()) {
        while (r.next())
          rows.add(
              new VerificationRecord(
                  r.getLong("id"),
                  r.getLong("request_id"),
                  r.getString("level"),
                  r.getLong("reviewer_id"),
                  VerificationDecision.valueOf(r.getString("decision")),
                  r.getString("reason"),
                  r.getInt("verification_round"),
                  SQLiteSupport.dateTime(r, "created_at")));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find verification records", e);
    }
  }

  private List<VerificationRecord> queryRound(String sql, long id, int round) {
    List<VerificationRecord> rows = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setLong(1, id);
      s.setInt(2, round);
      try (ResultSet r = s.executeQuery()) {
        while (r.next())
          rows.add(
              new VerificationRecord(
                  r.getLong("id"),
                  r.getLong("request_id"),
                  r.getString("level"),
                  r.getLong("reviewer_id"),
                  VerificationDecision.valueOf(r.getString("decision")),
                  r.getString("reason"),
                  r.getInt("verification_round"),
                  SQLiteSupport.dateTime(r, "created_at")));
      }
      return rows;
    } catch (SQLException e) {
      throw fail("find verification records by round", e);
    }
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
