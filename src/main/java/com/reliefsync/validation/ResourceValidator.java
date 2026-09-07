package com.reliefsync.validation;

import com.reliefsync.exception.ValidationException;
import com.reliefsync.model.Resource;

public final class ResourceValidator {
  public void validate(Resource resource) {
    if (resource == null) throw new ValidationException("Resource is required.");
    ValidationSupport.required(resource.name(), "Name");
    ValidationSupport.required(resource.category(), "Category");
    ValidationSupport.required(resource.unit(), "Unit");
    if (resource.minimumStockThreshold() < 0)
      throw new ValidationException("Minimum stock threshold cannot be negative.");
  }
}
