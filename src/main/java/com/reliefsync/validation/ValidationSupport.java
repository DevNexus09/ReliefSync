package com.reliefsync.validation;

import com.reliefsync.exception.ValidationException;

final class ValidationSupport {
  private ValidationSupport() {}

  static void required(String value, String field) {
    if (value == null || value.isBlank()) throw new ValidationException(field + " is required.");
  }

  static void coordinates(Double latitude, Double longitude) {
    if ((latitude == null) != (longitude == null)) {
      throw new ValidationException("Latitude and longitude must be provided together.");
    }
    if (latitude != null && (latitude < -90 || latitude > 90)) {
      throw new ValidationException("Latitude must be between -90 and 90.");
    }
    if (longitude != null && (longitude < -180 || longitude > 180)) {
      throw new ValidationException("Longitude must be between -180 and 180.");
    }
  }
}
