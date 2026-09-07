package com.reliefsync.repository;

import com.reliefsync.model.DisasterEvent;
import com.reliefsync.model.enums.DisasterStatus;
import java.util.List;
import java.util.Optional;

public interface DisasterEventRepository {
  Optional<DisasterEvent> findById(long id);

  List<DisasterEvent> findAll();

  List<DisasterEvent> findByStatus(DisasterStatus status);

  long save(DisasterEvent event);

  void update(DisasterEvent event);
}
