package com.reliefsync.verification;

import com.reliefsync.model.Role;

/**
 * Result of one verification decision: which round it belongs to, whether it
 * was approved, and whether the whole chain is finished (a rejection always
 * finishes the chain; an approval finishes it only on the last round).
 */
public record VerificationOutcome(Role roundRole, boolean approved, boolean chainComplete) {
}
