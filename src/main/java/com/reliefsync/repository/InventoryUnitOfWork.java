package com.reliefsync.repository;

import com.reliefsync.model.AuditEvent;
import com.reliefsync.model.CenterInventory;
import java.util.Optional;

public interface InventoryUnitOfWork {
  Optional<CenterInventory> find(long centerId, long resourceId);

  long save(CenterInventory inventory);

  void update(CenterInventory inventory);

  long saveAudit(AuditEvent auditEvent);
}
