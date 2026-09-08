package com.reliefsync.model;

/** One line of an allocation plan produced by a strategy (not yet persisted). */
public record PlannedAllocation(long centerId, String centerName, long resourceId,
                                String resourceName, int quantity) {
}
