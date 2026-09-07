package com.reliefsync.repository.sqlite;

import com.reliefsync.exception.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class SQLiteSupport {
  private SQLiteSupport() {}

  static long generatedId(PreparedStatement statement, String operation) throws SQLException {
    try (ResultSet keys = statement.getGeneratedKeys()) {
      if (keys.next()) {
        return keys.getLong(1);
      }
    }
    throw new PersistenceException(operation + " did not return a generated ID.");
  }

  static LocalDate date(ResultSet result, String column) throws SQLException {
    String value = result.getString(column);
    return value == null ? null : LocalDate.parse(value);
  }

  static LocalDateTime dateTime(ResultSet result, String column) throws SQLException {
    String value = result.getString(column);
    return value == null ? null : LocalDateTime.parse(value);
  }

  static Double nullableDouble(ResultSet result, String column) throws SQLException {
    double value = result.getDouble(column);
    return result.wasNull() ? null : value;
  }

  static Long nullableLong(ResultSet result, String column) throws SQLException {
    long value = result.getLong(column);
    return result.wasNull() ? null : value;
  }

  static void setNullableLong(PreparedStatement statement, int index, Long value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.INTEGER);
    } else {
      statement.setLong(index, value);
    }
  }

  static void setNullableDouble(PreparedStatement statement, int index, Double value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.REAL);
    } else {
      statement.setDouble(index, value);
    }
  }
}
