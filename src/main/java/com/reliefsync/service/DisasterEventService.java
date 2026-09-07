package com.reliefsync.service;

import com.reliefsync.exception.BusinessRuleException;
import com.reliefsync.exception.NotFoundException;
import com.reliefsync.model.DisasterEvent;
import com.reliefsync.model.enums.DisasterStatus;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.search.DisasterEventSearchCriteria;
import com.reliefsync.repository.DisasterEventRepository;
import com.reliefsync.repository.SearchLimits;
import com.reliefsync.security.AuthorizationService;
import com.reliefsync.security.UserSession;
import com.reliefsync.validation.DisasterEventValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class DisasterEventService {
  private final DisasterEventRepository repository;
  private final AuthorizationService authorization;
  private final DisasterEventValidator validator;

  public DisasterEventService(
      DisasterEventRepository repository,
      AuthorizationService authorization,
      DisasterEventValidator validator) {
    this.repository = repository;
    this.authorization = authorization;
    this.validator = validator;
  }

  public DisasterEvent create(DisasterEvent input, UserSession session) {
    authorization.require(session, Permission.MANAGE_DISASTERS);
    DisasterEvent value =
        new DisasterEvent(
            0,
            trim(input.name()),
            input.type(),
            trimNullable(input.description()),
            input.startDate(),
            input.endDate(),
            DisasterStatus.ACTIVE,
            session.userId(),
            LocalDateTime.now());
    validator.validate(value);
    long id = repository.save(value);
    return new DisasterEvent(
        id,
        value.name(),
        value.type(),
        value.description(),
        value.startDate(),
        value.endDate(),
        value.status(),
        value.createdBy(),
        value.createdAt());
  }

  public DisasterEvent update(long id, DisasterEvent input, UserSession session) {
    authorization.require(session, Permission.MANAGE_DISASTERS);
    DisasterEvent current = required(id);
    if (current.status() == DisasterStatus.CLOSED && input.status() == DisasterStatus.ACTIVE)
      throw new BusinessRuleException(
          "Use the explicit reactivate action to reopen a closed disaster.");
    DisasterEvent value =
        new DisasterEvent(
            id,
            trim(input.name()),
            input.type(),
            trimNullable(input.description()),
            input.startDate(),
            input.endDate(),
            current.status(),
            current.createdBy(),
            current.createdAt());
    validator.validate(value);
    repository.update(value);
    return value;
  }

  public void close(long id, UserSession session) {
    authorization.require(session, Permission.MANAGE_DISASTERS);
    changeStatus(required(id), DisasterStatus.CLOSED);
  }

  public void reactivate(long id, UserSession session) {
    authorization.require(session, Permission.MANAGE_DISASTERS);
    DisasterEvent current = required(id);
    if (current.status() != DisasterStatus.CLOSED)
      throw new BusinessRuleException("Only a closed disaster can be reactivated.");
    changeStatus(current, DisasterStatus.ACTIVE);
  }

  public Optional<DisasterEvent> findById(long id, UserSession session) {
    authorization.require(session, Permission.VIEW_DISASTERS);
    return repository.findById(id);
  }

  public List<DisasterEvent> search(DisasterEventSearchCriteria criteria, UserSession session) {
    authorization.require(session, Permission.VIEW_DISASTERS);
    return repository.search(criteria, SearchLimits.DEFAULT);
  }

  private DisasterEvent required(long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Disaster event was not found: " + id));
  }

  private void changeStatus(DisasterEvent e, DisasterStatus status) {
    repository.update(
        new DisasterEvent(
            e.id(),
            e.name(),
            e.type(),
            e.description(),
            e.startDate(),
            e.endDate(),
            status,
            e.createdBy(),
            e.createdAt()));
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }

  private static String trimNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
