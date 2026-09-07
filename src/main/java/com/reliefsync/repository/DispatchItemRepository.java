package com.reliefsync.repository;

import com.reliefsync.model.DispatchItem;
import java.util.List;
import java.util.Optional;

public interface DispatchItemRepository {
  Optional<DispatchItem> findById(long id);

  List<DispatchItem> findByDispatchId(long dispatchId);

  long save(DispatchItem item);

  void update(DispatchItem item);
}
