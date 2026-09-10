package com.reliefsync.model;

public record DeliveryFailure(long id, long requestId, long manifestId, int attemptNumber,
                              String reason, long reportedBy, String reporterName,
                              String reportedAt, DeliveryRecoveryAction recoveryAction,
                              String resolvedAt, String recoveryNotes) {

    public boolean resolved() {
        return resolvedAt != null;
    }
}
