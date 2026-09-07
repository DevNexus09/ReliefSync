package com.reliefsync.repository;

import com.reliefsync.model.ReliefCenter;
import com.reliefsync.model.search.ReliefCenterSearchCriteria;
import java.util.List;
import java.util.Optional;

public interface ReliefCenterRepository {
  Optional<ReliefCenter> findById(long id);

  List<ReliefCenter> findAll();

  List<ReliefCenter> search(ReliefCenterSearchCriteria criteria, int limit);

  long save(ReliefCenter center);

  void update(ReliefCenter center);
}
