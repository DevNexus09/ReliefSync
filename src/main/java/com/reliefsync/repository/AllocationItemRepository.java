package com.reliefsync.repository;

import com.reliefsync.model.AllocationItem;
import java.util.List;
import java.util.Optional;

public interface AllocationItemRepository {
  Optional<AllocationItem> findById(long id);

  List<AllocationItem> findByAllocationId(long allocationId);

  long save(AllocationItem item);

  void update(AllocationItem item);
}
