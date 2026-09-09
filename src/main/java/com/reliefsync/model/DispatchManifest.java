package com.reliefsync.model;

public record DispatchManifest(long id, long requestId, long vehicleId,
                               String vehicleRegistration, String vehicleType, int vehicleCapacity,
                               String driverName, ManifestStatus status,
                               String assignedAt, String dispatchedAt, String deliveredAt,
                               int totalLoad) {
}
