package com.reliefsync.repository;

import com.reliefsync.model.AuditEvent;
import java.util.List;

public interface AuditEventRepository {
  long save(AuditEvent event);

  List<AuditEvent> findByEntity(String entityType, long entityId);
}
