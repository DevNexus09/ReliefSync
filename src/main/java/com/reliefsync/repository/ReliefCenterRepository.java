package com.reliefsync.repository;

import com.reliefsync.model.ReliefCenter;
import java.util.List;
import java.util.Optional;

public interface ReliefCenterRepository {
  Optional<ReliefCenter> findById(long id);

  List<ReliefCenter> findAll();

  long save(ReliefCenter center);

  void update(ReliefCenter center);
}
