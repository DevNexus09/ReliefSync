package com.reliefsync.repository;

import com.reliefsync.model.Dispatch;
import com.reliefsync.model.enums.DispatchStatus;
import java.util.List;
import java.util.Optional;

public interface DispatchRepository {
  Optional<Dispatch> findById(long id);

  List<Dispatch> findByAllocationId(long allocationId);

  List<Dispatch> findByStatus(DispatchStatus status);

  List<Dispatch> findAll();

  long save(Dispatch dispatch);

  void update(Dispatch dispatch);
}
