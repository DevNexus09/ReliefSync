package com.reliefsync.model;

/** One immutable verification decision for one round of a request's chain. */
public record Verification(long id, long requestId, Role roundRole, long verifierId,
                           String verifierName, boolean approved, String comment, String decidedAt) {
}
