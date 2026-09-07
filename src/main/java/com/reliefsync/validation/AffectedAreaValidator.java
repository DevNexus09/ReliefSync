package com.reliefsync.validation;

import com.reliefsync.exception.ValidationException;
import com.reliefsync.model.AffectedArea;

public final class AffectedAreaValidator {
  public void validate(AffectedArea area) {
    if (area == null) throw new ValidationException("Affected area is required.");
    if (area.disasterEventId() <= 0) throw new ValidationException("Disaster event is required.");
    ValidationSupport.required(area.name(), "Name");
    ValidationSupport.required(area.district(), "District");
    if (area.populationAffected() < 0)
      throw new ValidationException("Population cannot be negative.");
    if (area.familiesAffected() < 0) throw new ValidationException("Families cannot be negative.");
    if (area.severity() == null) throw new ValidationException("Severity is required.");
    if (area.accessibility() == null) throw new ValidationException("Accessibility is required.");
    if (area.medicalUrgency() == null)
      throw new ValidationException("Medical urgency is required.");
    ValidationSupport.required(area.waterAccess(), "Water access");
    ValidationSupport.required(area.status(), "Status");
    ValidationSupport.coordinates(area.latitude(), area.longitude());
  }
}
