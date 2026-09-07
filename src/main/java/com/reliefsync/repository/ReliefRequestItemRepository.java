package com.reliefsync.repository;

import com.reliefsync.model.ReliefRequestItem;
import java.util.List;
import java.util.Optional;

public interface ReliefRequestItemRepository {
  Optional<ReliefRequestItem> findById(long id);

  List<ReliefRequestItem> findByRequestId(long requestId);

  long save(ReliefRequestItem item);

  void update(ReliefRequestItem item);
}
