package com.reliefsync.repository;

import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.enums.RequestStateType;
import com.reliefsync.model.search.ReliefRequestSearchCriteria;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReliefRequestRepository {
  Optional<ReliefRequest> findById(long id);

  List<ReliefRequest> findAll();

  List<ReliefRequest> findByDisasterEventId(long disasterEventId);

  List<ReliefRequest> findByAffectedAreaId(long affectedAreaId);

  List<ReliefRequest> findByState(RequestStateType state);

  List<ReliefRequest> search(ReliefRequestSearchCriteria criteria, int limit);

  List<ReliefRequest> findPotentialDuplicates(
      long disasterEventId, long affectedAreaId, LocalDateTime submittedAfter, int limit);

  long save(ReliefRequest request);

  void update(ReliefRequest request);

  void updateState(long requestId, RequestStateType state, LocalDateTime updatedAt);

  void updateVerificationRound(
      long requestId, int verificationRound, LocalDateTime submittedAt, LocalDateTime updatedAt);
}
