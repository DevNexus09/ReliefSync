package com.reliefsync.model;

import com.reliefsync.model.enums.RequestPriority;
import com.reliefsync.model.enums.RequestStateType;
import java.time.LocalDateTime;

public record ReliefRequest(
    long id,
    long disasterEventId,
    long affectedAreaId,
    long requestedBy,
    RequestPriority priority,
    RequestStateType state,
    String description,
    LocalDateTime submittedAt,
    LocalDateTime verifiedAt,
    int verificationRound,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
