package com.reliefsync.repository;

import com.reliefsync.model.VerificationRecord;
import java.util.List;
import java.util.Optional;

public interface VerificationRecordRepository {
  Optional<VerificationRecord> findById(long id);

  List<VerificationRecord> findByRequestId(long requestId);

  long save(VerificationRecord record);
}
