package com.reliefsync.repository;

import com.reliefsync.model.*;
import java.util.List;
import java.util.Optional;

public interface RequestWorkflowUnitOfWork {
  Optional<ReliefRequest> findRequest(long id);

  Optional<AffectedArea> findArea(long id);

  List<ReliefRequestItem> findItems(long requestId);

  List<VerificationRecord> findVerifications(long requestId, int round);

  long saveRequest(ReliefRequest request);

  void updateRequest(ReliefRequest request);

  void saveItems(long requestId, List<ReliefRequestItem> items);

  void replaceItems(long requestId, List<ReliefRequestItem> items);

  long saveVerification(VerificationRecord record);
}
