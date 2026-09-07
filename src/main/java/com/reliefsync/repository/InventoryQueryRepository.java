package com.reliefsync.repository;

import com.reliefsync.model.search.InventorySearchCriteria;
import com.reliefsync.service.dto.InventoryOverview;
import java.util.List;

public interface InventoryQueryRepository {
  List<InventoryOverview> search(InventorySearchCriteria criteria, int limit);
}
