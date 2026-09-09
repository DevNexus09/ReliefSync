package com.reliefsync.model;

/** An immutable audit event for a stock reservation. */
public record AllocationEvent(long id, long requestId, Long allocationId,
                              AllocationEventType eventType,
                              String centerName, String resourceName, int quantity,
                              String actorName, String occurredAt) {
}
