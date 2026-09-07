package com.reliefsync.model;

import java.time.LocalDateTime;

public record AuditEvent(
    long id,
    Long actorUserId,
    String eventType,
    String entityType,
    Long entityId,
    String description,
    LocalDateTime createdAt) {}
