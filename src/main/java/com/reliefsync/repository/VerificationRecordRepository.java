package com.reliefsync.repository;

import com.reliefsync.model.VerificationRecord;
import com.reliefsync.model.verification.VerificationLevel;
import java.util.List;
import java.util.Optional;

public interface VerificationRecordRepository {
  Optional<VerificationRecord> findById(long id);

  List<VerificationRecord> findByRequestId(long requestId);

  List<VerificationRecord> findByRequestAndRound(long requestId, int verificationRound);

  boolean existsForLevel(long requestId, int verificationRound, VerificationLevel level);

  long save(VerificationRecord record);
}
