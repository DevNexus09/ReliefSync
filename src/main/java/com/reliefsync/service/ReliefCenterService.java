package com.reliefsync.service;

import com.reliefsync.exception.NotFoundException;
import com.reliefsync.model.ReliefCenter;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.search.ReliefCenterSearchCriteria;
import com.reliefsync.repository.ReliefCenterRepository;
import com.reliefsync.repository.SearchLimits;
import com.reliefsync.security.AuthorizationService;
import com.reliefsync.security.UserSession;
import com.reliefsync.validation.ReliefCenterValidator;
import java.time.LocalDateTime;
import java.util.List;

public final class ReliefCenterService {
  private final ReliefCenterRepository repository;
  private final AuthorizationService authorization;
  private final ReliefCenterValidator validator;

  public ReliefCenterService(
      ReliefCenterRepository repository,
      AuthorizationService authorization,
      ReliefCenterValidator validator) {
    this.repository = repository;
    this.authorization = authorization;
    this.validator = validator;
  }

  public ReliefCenter create(ReliefCenter input, UserSession session) {
    authorization.require(session, Permission.MANAGE_RELIEF_CENTERS);
    ReliefCenter value = copy(0, input, true, LocalDateTime.now());
    validator.validate(value);
    long id = repository.save(value);
    return copy(id, value, true, value.createdAt());
  }

  public ReliefCenter update(long id, ReliefCenter input, UserSession session) {
    authorization.require(session, Permission.MANAGE_RELIEF_CENTERS);
    ReliefCenter old = required(id);
    ReliefCenter value = copy(id, input, old.active(), old.createdAt());
    validator.validate(value);
    repository.update(value);
    return value;
  }

  public void activate(long id, UserSession session) {
    setActive(id, true, session);
  }

  public void deactivate(long id, UserSession session) {
    setActive(id, false, session);
  }

  public List<ReliefCenter> search(ReliefCenterSearchCriteria criteria, UserSession session) {
    authorization.require(session, Permission.VIEW_RELIEF_CENTERS);
    return repository.search(criteria, SearchLimits.DEFAULT);
  }

  private void setActive(long id, boolean active, UserSession session) {
    authorization.require(session, Permission.MANAGE_RELIEF_CENTERS);
    ReliefCenter c = required(id);
    repository.update(copy(id, c, active, c.createdAt()));
  }

  private ReliefCenter required(long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Relief center was not found: " + id));
  }

  private static ReliefCenter copy(long id, ReliefCenter c, boolean active, LocalDateTime created) {
    return new ReliefCenter(
        id,
        trim(c.name()),
        trim(c.district()),
        c.latitude(),
        c.longitude(),
        nullable(c.contactInfo()),
        active,
        created);
  }

  private static String trim(String s) {
    return s == null ? null : s.trim();
  }

  private static String nullable(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
