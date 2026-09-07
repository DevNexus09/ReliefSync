package com.reliefsync.model;

import java.time.LocalDateTime;

public record ReliefCenter(
    long id,
    String name,
    String district,
    Double latitude,
    Double longitude,
    String contactInfo,
    boolean active,
    LocalDateTime createdAt) {}
