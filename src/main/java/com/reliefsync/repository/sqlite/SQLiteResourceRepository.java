package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.Resource;
import com.reliefsync.model.search.ResourceSearchCriteria;
import com.reliefsync.repository.ResourceRepository;
import java.sql.*;
import java.util.*;

public final class SQLiteResourceRepository implements ResourceRepository {
  private final DatabaseManager databaseManager;

  public SQLiteResourceRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public Optional<Resource> findById(long id) {
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement("SELECT * FROM resources WHERE id=?")) {
      s.setLong(1, id);
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? Optional.of(map(r)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw fail("find resource", e);
    }
  }

  public List<Resource> findAll() {
    List<Resource> list = new ArrayList<>();
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement("SELECT * FROM resources ORDER BY name");
        ResultSet r = s.executeQuery()) {
      while (r.next()) list.add(map(r));
      return list;
    } catch (SQLException e) {
      throw fail("find resources", e);
    }
  }

  public Optional<Resource> findByNameAndUnit(String name, String unit) {
    SqlSearch search =
        new SqlSearch(
            "SELECT * FROM resources WHERE LOWER(name)=LOWER(?) AND LOWER(unit)=LOWER(?)");
    search.add("", name);
    search.add("", unit);
    try (Connection c = databaseManager.openConnection()) {
      return search.execute(c, " ORDER BY id", 1, this::map).stream().findFirst();
    } catch (SQLException e) {
      throw fail("find resource by name and unit", e);
    }
  }

  public List<Resource> search(ResourceSearchCriteria criteria, int limit) {
    Objects.requireNonNull(criteria);
    SqlSearch search = new SqlSearch("SELECT * FROM resources WHERE 1=1");
    search.add(
        " AND LOWER(name) LIKE LOWER(?)",
        criteria.name() == null ? null : "%" + criteria.name().trim() + "%");
    search.add(" AND LOWER(category)=LOWER(?)", criteria.category());
    search.add(" AND active=?", criteria.active() == null ? null : criteria.active() ? 1 : 0);
    try (Connection c = databaseManager.openConnection()) {
      return search.execute(c, " ORDER BY name", limit, this::map);
    } catch (SQLException e) {
      throw fail("search resources", e);
    }
  }

  public long save(Resource v) {
    String sql =
        "INSERT INTO resources(name,category,unit,minimum_stock_threshold,active)"
            + " VALUES(?,?,?,?,?)";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, v, false);
      s.executeUpdate();
      return SQLiteSupport.generatedId(s, "Saving resource");
    } catch (SQLException e) {
      throw fail("save resource", e);
    }
  }

  public void update(Resource v) {
    String sql =
        "UPDATE resources SET name=?,category=?,unit=?,minimum_stock_threshold=?,active=? WHERE"
            + " id=?";
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, v, true);
      if (s.executeUpdate() == 0)
        throw new PersistenceException("Resource was not found: " + v.id());
    } catch (SQLException e) {
      throw fail("update resource", e);
    }
  }

  private void bind(PreparedStatement s, Resource v, boolean id) throws SQLException {
    s.setString(1, v.name());
    s.setString(2, v.category());
    s.setString(3, v.unit());
    s.setLong(4, v.minimumStockThreshold());
    s.setInt(5, v.active() ? 1 : 0);
    if (id) s.setLong(6, v.id());
  }

  private Resource map(ResultSet r) throws SQLException {
    return new Resource(
        r.getLong("id"),
        r.getString("name"),
        r.getString("category"),
        r.getString("unit"),
        r.getLong("minimum_stock_threshold"),
        r.getInt("active") == 1);
  }

  private PersistenceException fail(String op, SQLException e) {
    return new PersistenceException("Failed to " + op + ".", e);
  }
}
