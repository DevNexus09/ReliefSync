package com.reliefsync.model;

public record DispatchManifest(long id, long requestId, int attemptNumber, long vehicleId,
                               String vehicleRegistration, String vehicleType, int vehicleCapacity,
                               String driverName, ManifestStatus status,
                               String assignedAt, String dispatchedAt, String deliveredAt, String failedAt,
                               int totalLoad) {
}
