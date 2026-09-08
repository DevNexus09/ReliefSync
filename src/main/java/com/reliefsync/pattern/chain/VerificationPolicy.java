package com.reliefsync.pattern.chain;

import com.reliefsync.model.AffectedArea;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.ReliefRequestItem;
import com.reliefsync.model.enums.*;
import com.reliefsync.model.verification.*;
import java.util.List;

public final class VerificationPolicy {
  private final VerificationPolicyConfig config;

  public VerificationPolicy(VerificationPolicyConfig config) {
    this.config = config;
  }

  public VerificationTier determine(
      ReliefRequest request, AffectedArea area, List<ReliefRequestItem> items) {
    long max = items.stream().mapToLong(ReliefRequestItem::requestedQuantity).max().orElse(0);
    if (request.priority() == RequestPriority.CRITICAL
        || area.severity() == Severity.CRITICAL
        || area.medicalUrgency() == MedicalUrgency.CRITICAL
        || max >= config.criticalQuantityThreshold()) return VerificationTier.CRITICAL;
    if (request.priority() == RequestPriority.HIGH
        || area.severity() == Severity.HIGH
        || area.medicalUrgency() == MedicalUrgency.HIGH
        || max >= config.highQuantityThreshold()) return VerificationTier.HIGH;
    return VerificationTier.NORMAL;
  }
}
