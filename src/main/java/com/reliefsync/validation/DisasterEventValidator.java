package com.reliefsync.validation;

import com.reliefsync.exception.ValidationException;
import com.reliefsync.model.DisasterEvent;

public final class DisasterEventValidator {
  public void validate(DisasterEvent event) {
    if (event == null) throw new ValidationException("Disaster event is required.");
    ValidationSupport.required(event.name(), "Name");
    if (event.name().trim().length() > 150)
      throw new ValidationException("Name must not exceed 150 characters.");
    if (event.type() == null) throw new ValidationException("Disaster type is required.");
    if (event.startDate() == null) throw new ValidationException("Start date is required.");
    if (event.status() == null) throw new ValidationException("Status is required.");
    if (event.endDate() != null && event.endDate().isBefore(event.startDate())) {
      throw new ValidationException("End date must not be before start date.");
    }
  }
}
