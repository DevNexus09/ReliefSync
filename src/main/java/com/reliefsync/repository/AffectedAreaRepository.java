package com.reliefsync.repository;

import com.reliefsync.model.AffectedArea;
import com.reliefsync.model.search.AffectedAreaSearchCriteria;
import java.util.List;
import java.util.Optional;

public interface AffectedAreaRepository {
  Optional<AffectedArea> findById(long id);

  List<AffectedArea> findAll();

  List<AffectedArea> findByDisasterEventId(long disasterEventId);

  List<AffectedArea> search(AffectedAreaSearchCriteria criteria, int limit);

  long save(AffectedArea area);

  void update(AffectedArea area);
}
