package com.reliefsync.service;

import com.reliefsync.exception.BusinessRuleException;
import com.reliefsync.exception.NotFoundException;
import com.reliefsync.exception.ValidationException;
import com.reliefsync.model.Vehicle;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.enums.VehicleStatus;
import com.reliefsync.model.search.VehicleSearchCriteria;
import com.reliefsync.repository.ReliefCenterRepository;
import com.reliefsync.repository.SearchLimits;
import com.reliefsync.repository.VehicleRepository;
import com.reliefsync.security.AuthorizationService;
import com.reliefsync.security.UserSession;
import com.reliefsync.validation.VehicleValidator;
import java.util.List;

public final class VehicleService {
  private final VehicleRepository repository;
  private final ReliefCenterRepository centers;
  private final AuthorizationService authorization;
  private final VehicleValidator validator;

  public VehicleService(
      VehicleRepository repository,
      ReliefCenterRepository centers,
      AuthorizationService authorization,
      VehicleValidator validator) {
    this.repository = repository;
    this.centers = centers;
    this.authorization = authorization;
    this.validator = validator;
  }

  public Vehicle register(Vehicle input, UserSession session) {
    authorization.require(session, Permission.MANAGE_VEHICLES);
    Vehicle value = copy(0, input, VehicleStatus.AVAILABLE, true);
    validate(value, -1);
    long id = repository.save(value);
    return copy(id, value, value.status(), true);
  }

  public Vehicle update(long id, Vehicle input, UserSession session) {
    authorization.require(session, Permission.MANAGE_VEHICLES);
    Vehicle old = required(id);
    Vehicle value = copy(id, input, old.status(), old.active());
    validate(value, id);
    repository.update(value);
    return value;
  }

  public void assignHomeCenter(long id, Long centerId, UserSession session) {
    authorization.require(session, Permission.MANAGE_VEHICLES);
    Vehicle v = required(id);
    Vehicle changed =
        new Vehicle(
            v.id(), v.registrationNo(), v.type(), v.capacity(), v.status(), centerId, v.active());
    validate(changed, id);
    repository.update(changed);
  }

  public void changeManualAvailability(long id, VehicleStatus status, UserSession session) {
    authorization.require(session, Permission.MANAGE_VEHICLES);
    if (status != VehicleStatus.AVAILABLE && status != VehicleStatus.UNAVAILABLE)
      throw new BusinessRuleException("Manual status may only be AVAILABLE or UNAVAILABLE.");
    Vehicle v = required(id);
    if (!v.active()) throw new BusinessRuleException("Inactive vehicles cannot be operational.");
    repository.update(copy(id, v, status, true));
  }

  public void deactivate(long id, UserSession session) {
    authorization.require(session, Permission.MANAGE_VEHICLES);
    Vehicle v = required(id);
    if (v.status() == VehicleStatus.ASSIGNED || v.status() == VehicleStatus.IN_TRANSIT)
      throw new BusinessRuleException("An assigned or in-transit vehicle cannot be deactivated.");
    repository.update(copy(id, v, VehicleStatus.UNAVAILABLE, false));
  }

  public void activate(long id, UserSession session) {
    authorization.require(session, Permission.MANAGE_VEHICLES);
    Vehicle v = required(id);
    repository.update(copy(id, v, VehicleStatus.AVAILABLE, true));
  }

  public List<Vehicle> search(VehicleSearchCriteria criteria, UserSession session) {
    authorization.require(session, Permission.VIEW_VEHICLES);
    return repository.search(criteria, SearchLimits.DEFAULT);
  }

  private void validate(Vehicle v, long own) {
    validator.validate(v);
    if (v.reliefCenterId() != null && centers.findById(v.reliefCenterId()).isEmpty())
      throw new NotFoundException("Relief center was not found: " + v.reliefCenterId());
    repository
        .findByRegistrationNo(v.registrationNo())
        .filter(x -> x.id() != own)
        .ifPresent(
            x -> {
              throw new ValidationException("Registration number already exists.");
            });
  }

  private Vehicle required(long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Vehicle was not found: " + id));
  }

  private static Vehicle copy(long id, Vehicle v, VehicleStatus status, boolean active) {
    return new Vehicle(
        id,
        trim(v.registrationNo()),
        trim(v.type()),
        v.capacity(),
        status,
        v.reliefCenterId(),
        active);
  }

  private static String trim(String s) {
    return s == null ? null : s.trim();
  }
}
