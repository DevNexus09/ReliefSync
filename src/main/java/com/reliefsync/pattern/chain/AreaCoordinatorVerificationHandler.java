package com.reliefsync.pattern.chain;

import com.reliefsync.model.enums.Role;
import com.reliefsync.model.verification.VerificationLevel;

public final class AreaCoordinatorVerificationHandler extends VerificationHandler {
  public AreaCoordinatorVerificationHandler() {
    super(VerificationLevel.AREA_COORDINATOR, Role.AREA_COORDINATOR);
  }
}
