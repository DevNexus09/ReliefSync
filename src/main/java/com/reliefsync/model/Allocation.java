package com.reliefsync.model;

import com.reliefsync.model.enums.AllocationStatus;
import java.time.LocalDateTime;

public record Allocation(
    long id,
    long requestId,
    String strategyType,
    AllocationStatus status,
    long createdBy,
    LocalDateTime createdAt,
    LocalDateTime confirmedAt,
    LocalDateTime cancelledAt) {}
