package com.reliefsync.model;

import com.reliefsync.model.enums.VerificationDecision;
import java.time.LocalDateTime;

public record VerificationRecord(
    long id,
    long requestId,
    String level,
    long reviewerId,
    VerificationDecision decision,
    String reason,
    LocalDateTime createdAt) {}
