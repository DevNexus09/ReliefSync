package com.reliefsync.service.dto;

import com.reliefsync.model.*;
import com.reliefsync.model.verification.*;
import java.util.List;

public record VerificationDetails(
    ReliefRequest request,
    AffectedArea area,
    List<ReliefRequestItem> items,
    List<VerificationRecord> history,
    VerificationTier tier,
    VerificationLevel nextRequiredLevel) {
  public VerificationDetails {
    items = List.copyOf(items);
    history = List.copyOf(history);
  }
}
