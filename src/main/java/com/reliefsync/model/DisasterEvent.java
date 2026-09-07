package com.reliefsync.model;

import com.reliefsync.model.enums.DisasterStatus;
import com.reliefsync.model.enums.DisasterType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DisasterEvent(
    long id,
    String name,
    DisasterType type,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    DisasterStatus status,
    long createdBy,
    LocalDateTime createdAt) {}
