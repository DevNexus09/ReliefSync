package com.reliefsync.model.verification;

import com.reliefsync.model.enums.VerificationDecision;
import com.reliefsync.security.UserSession;
import java.util.Set;

public record VerificationContext(
    UserSession session,
    VerificationDecision decision,
    String reason,
    Set<VerificationLevel> approvedLevels) {
  public VerificationContext {
    approvedLevels = Set.copyOf(approvedLevels);
  }
}
