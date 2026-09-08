package com.reliefsync.verification;

import com.reliefsync.model.Role;

/** Final round for CRITICAL requests: administrative sign-off. */
public final class AdminVerifier extends VerificationHandler {

    @Override
    public Role requiredRole() {
        return Role.ADMIN;
    }
}
