package com.reliefsync.model;

import com.reliefsync.model.enums.Accessibility;
import com.reliefsync.model.enums.MedicalUrgency;
import com.reliefsync.model.enums.Severity;
import java.time.LocalDateTime;

public record AffectedArea(
    long id,
    long disasterEventId,
    String name,
    String district,
    Double latitude,
    Double longitude,
    long populationAffected,
    long familiesAffected,
    Severity severity,
    Accessibility accessibility,
    MedicalUrgency medicalUrgency,
    String waterAccess,
    String status,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
