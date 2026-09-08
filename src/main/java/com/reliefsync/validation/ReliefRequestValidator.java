package com.reliefsync.validation;

import com.reliefsync.exception.*;
import com.reliefsync.model.*;
import com.reliefsync.model.enums.DisasterStatus;
import com.reliefsync.model.request.*;
import com.reliefsync.repository.*;
import java.util.HashSet;
import java.util.Set;

public final class ReliefRequestValidator {
  private final DisasterEventRepository disasters;
  private final AffectedAreaRepository areas;
  private final ResourceRepository resources;

  public ReliefRequestValidator(
      DisasterEventRepository disasters,
      AffectedAreaRepository areas,
      ResourceRepository resources) {
    this.disasters = disasters;
    this.areas = areas;
    this.resources = resources;
  }

  public void validate(ReliefRequestDraft draft) {
    if (draft == null) throw new ValidationException("Relief request is required.");
    if (draft.priority() == null) throw new ValidationException("Request priority is required.");
    DisasterEvent event =
        disasters
            .findById(draft.disasterEventId())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Disaster event was not found: " + draft.disasterEventId()));
    if (event.status() != DisasterStatus.ACTIVE)
      throw new BusinessRuleException("The selected disaster is closed.");
    AffectedArea area =
        areas
            .findById(draft.affectedAreaId())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Affected area was not found: " + draft.affectedAreaId()));
    if (area.disasterEventId() != event.id())
      throw new ValidationException("The affected area does not belong to the selected disaster.");
    if (draft.items().isEmpty())
      throw new ValidationException("At least one relief resource is required.");
    Set<Long> seen = new HashSet<>();
    for (ReliefRequestDraftItem item : draft.items()) {
      if (item.requestedQuantity() <= 0)
        throw new ValidationException("Requested quantity must be greater than zero.");
      if (!seen.add(item.resourceId()))
        throw new ValidationException("The same resource cannot be requested twice.");
      Resource resource =
          resources
              .findById(item.resourceId())
              .orElseThrow(
                  () -> new NotFoundException("Resource was not found: " + item.resourceId()));
      if (!resource.active())
        throw new BusinessRuleException("Requested resource is inactive: " + resource.name());
    }
  }
}
