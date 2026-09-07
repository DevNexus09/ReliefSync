package com.reliefsync.repository.sqlite;

import com.reliefsync.exception.PersistenceException;
import com.reliefsync.repository.SearchLimits;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class SqlSearch {
  private final StringBuilder sql;
  private final List<Object> parameters = new ArrayList<>();

  SqlSearch(String baseSql) {
    sql = new StringBuilder(baseSql);
  }

  void add(String predefinedFragment, Object value) {
    if (value != null && (!(value instanceof String text) || !text.isBlank())) {
      sql.append(predefinedFragment);
      parameters.add(value instanceof String text ? text.trim() : value);
    }
  }

  void addFlag(String predefinedFragment, boolean enabled) {
    if (enabled) sql.append(predefinedFragment);
  }

  <T> List<T> execute(Connection connection, String orderBy, int limit, RowMapper<T> mapper) {
    SearchLimits.requireValid(limit);
    sql.append(orderBy).append(" LIMIT ?");
    try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      int index = 1;
      for (Object value : parameters) statement.setObject(index++, value);
      statement.setInt(index, limit);
      List<T> rows = new ArrayList<>();
      try (ResultSet result = statement.executeQuery()) {
        while (result.next()) rows.add(mapper.map(result));
      }
      return rows;
    } catch (SQLException exception) {
      throw new PersistenceException("Search query failed.", exception);
    }
  }

  @FunctionalInterface
  interface RowMapper<T> {
    T map(ResultSet result) throws SQLException;
  }
}
