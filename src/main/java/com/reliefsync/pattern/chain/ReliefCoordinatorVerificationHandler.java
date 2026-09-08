package com.reliefsync.pattern.chain;

import com.reliefsync.model.enums.Role;
import com.reliefsync.model.verification.VerificationLevel;

public final class ReliefCoordinatorVerificationHandler extends VerificationHandler {
  public ReliefCoordinatorVerificationHandler() {
    super(VerificationLevel.RELIEF_COORDINATOR, Role.RELIEF_COORDINATOR);
  }
}
