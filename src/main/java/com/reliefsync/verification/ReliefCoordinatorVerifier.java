package com.reliefsync.verification;

import com.reliefsync.model.Role;

/** Second round for HIGH and CRITICAL requests: operational feasibility check. */
public final class ReliefCoordinatorVerifier extends VerificationHandler {

    @Override
    public Role requiredRole() {
        return Role.RELIEF_COORDINATOR;
    }
}
