package com.reliefsync.repository;

import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.enums.RequestStateType;
import java.util.List;
import java.util.Optional;

public interface ReliefRequestRepository {
  Optional<ReliefRequest> findById(long id);

  List<ReliefRequest> findAll();

  List<ReliefRequest> findByDisasterEventId(long disasterEventId);

  List<ReliefRequest> findByAffectedAreaId(long affectedAreaId);

  List<ReliefRequest> findByState(RequestStateType state);

  long save(ReliefRequest request);

  void update(ReliefRequest request);
}
