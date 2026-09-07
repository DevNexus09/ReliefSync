package com.reliefsync.repository;

import com.reliefsync.model.AffectedArea;
import java.util.List;
import java.util.Optional;

public interface AffectedAreaRepository {
  Optional<AffectedArea> findById(long id);

  List<AffectedArea> findAll();

  List<AffectedArea> findByDisasterEventId(long disasterEventId);

  long save(AffectedArea area);

  void update(AffectedArea area);
}
