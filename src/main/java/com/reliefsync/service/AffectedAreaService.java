package com.reliefsync.service;

import com.reliefsync.exception.NotFoundException;
import com.reliefsync.model.AffectedArea;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.search.AffectedAreaSearchCriteria;
import com.reliefsync.repository.AffectedAreaRepository;
import com.reliefsync.repository.DisasterEventRepository;
import com.reliefsync.repository.SearchLimits;
import com.reliefsync.security.AuthorizationService;
import com.reliefsync.security.UserSession;
import com.reliefsync.validation.AffectedAreaValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class AffectedAreaService {
  private final AffectedAreaRepository repository;
  private final DisasterEventRepository disasters;
  private final AuthorizationService authorization;
  private final AffectedAreaValidator validator;

  public AffectedAreaService(
      AffectedAreaRepository repository,
      DisasterEventRepository disasters,
      AuthorizationService authorization,
      AffectedAreaValidator validator) {
    this.repository = repository;
    this.disasters = disasters;
    this.authorization = authorization;
    this.validator = validator;
  }

  public AffectedArea create(AffectedArea input, UserSession session) {
    authorization.require(session, Permission.MANAGE_AFFECTED_AREAS);
    ensureDisaster(input.disasterEventId());
    LocalDateTime now = LocalDateTime.now();
    AffectedArea value = copy(0, input, now, now);
    validator.validate(value);
    long id = repository.save(value);
    return copy(id, value, value.createdAt(), value.updatedAt());
  }

  public AffectedArea update(long id, AffectedArea input, UserSession session) {
    authorization.require(session, Permission.MANAGE_AFFECTED_AREAS);
    AffectedArea current = required(id);
    ensureDisaster(input.disasterEventId());
    AffectedArea value = copy(id, input, current.createdAt(), LocalDateTime.now());
    validator.validate(value);
    repository.update(value);
    return value;
  }

  public Optional<AffectedArea> findById(long id, UserSession session) {
    authorization.require(session, Permission.VIEW_AFFECTED_AREAS);
    return repository.findById(id);
  }

  public List<AffectedArea> search(AffectedAreaSearchCriteria criteria, UserSession session) {
    authorization.require(session, Permission.VIEW_AFFECTED_AREAS);
    return repository.search(criteria, SearchLimits.DEFAULT);
  }

  private void ensureDisaster(long id) {
    if (disasters.findById(id).isEmpty())
      throw new NotFoundException("Disaster event was not found: " + id);
  }

  private AffectedArea required(long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Affected area was not found: " + id));
  }

  private static AffectedArea copy(
      long id, AffectedArea a, LocalDateTime created, LocalDateTime updated) {
    return new AffectedArea(
        id,
        a.disasterEventId(),
        trim(a.name()),
        trim(a.district()),
        a.latitude(),
        a.longitude(),
        a.populationAffected(),
        a.familiesAffected(),
        a.severity(),
        a.accessibility(),
        a.medicalUrgency(),
        trim(a.waterAccess()),
        a.status() == null ? "ACTIVE" : trim(a.status()),
        nullable(a.notes()),
        created,
        updated);
  }

  private static String trim(String s) {
    return s == null ? null : s.trim();
  }

  private static String nullable(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
