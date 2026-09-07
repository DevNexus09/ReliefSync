package com.reliefsync.repository;

import com.reliefsync.model.Allocation;
import java.util.List;
import java.util.Optional;

public interface AllocationRepository {
  Optional<Allocation> findById(long id);

  List<Allocation> findByRequestId(long requestId);

  List<Allocation> findAll();

  long save(Allocation allocation);

  void update(Allocation allocation);
}
