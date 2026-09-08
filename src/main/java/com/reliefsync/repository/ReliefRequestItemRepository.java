package com.reliefsync.repository;

import com.reliefsync.model.ReliefRequestItem;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ReliefRequestItemRepository {
  Optional<ReliefRequestItem> findById(long id);

  List<ReliefRequestItem> findByRequestId(long requestId);

  long save(ReliefRequestItem item);

  void saveAll(long requestId, List<ReliefRequestItem> items);

  void replaceItems(long requestId, List<ReliefRequestItem> items);

  Map<Long, Set<Long>> findResourceIdsByRequestIds(Collection<Long> requestIds);

  void update(ReliefRequestItem item);
}
