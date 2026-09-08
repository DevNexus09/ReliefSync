package com.reliefsync.pattern.chain;

import com.reliefsync.model.enums.Role;
import com.reliefsync.model.verification.VerificationLevel;

public final class VolunteerVerificationHandler extends VerificationHandler {
  public VolunteerVerificationHandler() {
    super(VerificationLevel.VOLUNTEER, Role.VOLUNTEER);
  }
}
