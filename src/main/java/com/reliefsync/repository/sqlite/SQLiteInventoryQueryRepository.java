package com.reliefsync.repository.sqlite;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.search.InventorySearchCriteria;
import com.reliefsync.repository.InventoryQueryRepository;
import com.reliefsync.service.dto.InventoryOverview;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class SQLiteInventoryQueryRepository implements InventoryQueryRepository {
  private final DatabaseManager databaseManager;

  public SQLiteInventoryQueryRepository(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
  }

  public List<InventoryOverview> search(InventorySearchCriteria criteria, int limit) {
    Objects.requireNonNull(criteria);
    SqlSearch q =
        new SqlSearch(
            "SELECT i.id inventory_id,c.id center_id,c.name center_name,r.id resource_id,r.name"
                + " resource_name,r.category,i.total_quantity,i.reserved_quantity,i.dispatched_quantity,r.minimum_stock_threshold"
                + " FROM center_inventory i JOIN relief_centers c ON c.id=i.relief_center_id JOIN"
                + " resources r ON r.id=i.resource_id WHERE 1=1");
    q.add(" AND c.id=?", criteria.reliefCenterId());
    q.add(" AND r.id=?", criteria.resourceId());
    q.add(" AND LOWER(r.category)=LOWER(?)", criteria.category());
    q.addFlag(
        " AND (i.total_quantity-i.reserved_quantity-i.dispatched_quantity)<=r.minimum_stock_threshold",
        criteria.lowStockOnly());
    try (Connection c = databaseManager.openConnection()) {
      return q.execute(
          c,
          " ORDER BY c.name,r.name",
          limit,
          r -> {
            long total = r.getLong("total_quantity"),
                reserved = r.getLong("reserved_quantity"),
                dispatched = r.getLong("dispatched_quantity"),
                threshold = r.getLong("minimum_stock_threshold"),
                available = total - reserved - dispatched;
            return new InventoryOverview(
                r.getLong("inventory_id"),
                r.getLong("center_id"),
                r.getString("center_name"),
                r.getLong("resource_id"),
                r.getString("resource_name"),
                r.getString("category"),
                total,
                reserved,
                dispatched,
                available,
                threshold,
                available <= threshold);
          });
    } catch (SQLException e) {
      throw new PersistenceException("Failed to search inventory.", e);
    }
  }
}
