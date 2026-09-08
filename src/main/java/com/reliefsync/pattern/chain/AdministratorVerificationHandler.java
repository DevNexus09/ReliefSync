package com.reliefsync.pattern.chain;

import com.reliefsync.model.enums.Role;
import com.reliefsync.model.verification.VerificationLevel;

public final class AdministratorVerificationHandler extends VerificationHandler {
  public AdministratorVerificationHandler() {
    super(VerificationLevel.ADMINISTRATOR, Role.ADMINISTRATOR);
  }
}
