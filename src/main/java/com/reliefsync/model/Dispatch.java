package com.reliefsync.model;

import com.reliefsync.model.enums.DispatchStatus;
import java.time.LocalDateTime;

public record Dispatch(
    long id,
    long allocationId,
    long vehicleId,
    long sourceCenterId,
    long destinationAreaId,
    DispatchStatus status,
    LocalDateTime createdAt,
    LocalDateTime dispatchedAt,
    LocalDateTime deliveredAt,
    String failureReason) {}
