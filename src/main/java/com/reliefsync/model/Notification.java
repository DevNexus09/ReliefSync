package com.reliefsync.model;

import java.time.LocalDateTime;

public record Notification(
    long id,
    long userId,
    String eventType,
    String title,
    String message,
    boolean read,
    LocalDateTime createdAt) {}
