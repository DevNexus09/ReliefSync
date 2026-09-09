package com.reliefsync.model;

/** A persisted stock reservation: quantity of one resource taken from one center. */
public record Allocation(long id, long requestId, long centerId, String centerName,
                         long resourceId, String resourceName, int quantity,
                         String strategy, String createdAt, boolean active,
                         String releasedAt, String releasedByName) {
}
