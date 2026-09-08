package com.reliefsync.verification;

import com.reliefsync.model.Role;

/** First round: field-level confirmation that the need is genuine. */
public final class AreaCoordinatorVerifier extends VerificationHandler {

    @Override
    public Role requiredRole() {
        return Role.AREA_COORDINATOR;
    }
}
