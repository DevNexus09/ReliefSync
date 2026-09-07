package com.reliefsync.validation;

import com.reliefsync.exception.ValidationException;
import com.reliefsync.model.ReliefCenter;

public final class ReliefCenterValidator {
  public void validate(ReliefCenter center) {
    if (center == null) throw new ValidationException("Relief center is required.");
    ValidationSupport.required(center.name(), "Name");
    ValidationSupport.required(center.district(), "District");
    ValidationSupport.coordinates(center.latitude(), center.longitude());
  }
}
