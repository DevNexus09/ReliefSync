package com.reliefsync.validation;

import com.reliefsync.exception.ValidationException;
import com.reliefsync.model.Vehicle;

public final class VehicleValidator {
  public void validate(Vehicle vehicle) {
    if (vehicle == null) throw new ValidationException("Vehicle is required.");
    ValidationSupport.required(vehicle.registrationNo(), "Registration number");
    ValidationSupport.required(vehicle.type(), "Vehicle type");
    if (vehicle.capacity() <= 0)
      throw new ValidationException("Capacity must be greater than zero.");
    if (vehicle.status() == null) throw new ValidationException("Vehicle status is required.");
  }
}
