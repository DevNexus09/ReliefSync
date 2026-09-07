package com.reliefsync.service;

import com.reliefsync.exception.NotFoundException;
import com.reliefsync.exception.ValidationException;
import com.reliefsync.model.Resource;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.search.ResourceSearchCriteria;
import com.reliefsync.repository.ResourceRepository;
import com.reliefsync.repository.SearchLimits;
import com.reliefsync.security.AuthorizationService;
import com.reliefsync.security.UserSession;
import com.reliefsync.validation.ResourceValidator;
import java.util.List;

public final class ResourceService {
  private final ResourceRepository repository;
  private final AuthorizationService authorization;
  private final ResourceValidator validator;

  public ResourceService(
      ResourceRepository repository,
      AuthorizationService authorization,
      ResourceValidator validator) {
    this.repository = repository;
    this.authorization = authorization;
    this.validator = validator;
  }

  public Resource create(Resource input, UserSession session) {
    authorization.require(session, Permission.MANAGE_RESOURCES);
    Resource value = copy(0, input, true);
    validator.validate(value);
    ensureUnique(value, -1);
    long id = repository.save(value);
    return copy(id, value, true);
  }

  public Resource update(long id, Resource input, UserSession session) {
    authorization.require(session, Permission.MANAGE_RESOURCES);
    Resource old = required(id);
    Resource value = copy(id, input, old.active());
    validator.validate(value);
    ensureUnique(value, id);
    repository.update(value);
    return value;
  }

  public void activate(long id, UserSession session) {
    setActive(id, true, session);
  }

  public void deactivate(long id, UserSession session) {
    setActive(id, false, session);
  }

  public List<Resource> search(ResourceSearchCriteria criteria, UserSession session) {
    authorization.require(session, Permission.VIEW_RESOURCES);
    return repository.search(criteria, SearchLimits.DEFAULT);
  }

  private void ensureUnique(Resource r, long own) {
    repository
        .findByNameAndUnit(r.name(), r.unit())
        .filter(x -> x.id() != own)
        .ifPresent(
            x -> {
              throw new ValidationException("A resource with this name and unit already exists.");
            });
  }

  private void setActive(long id, boolean active, UserSession s) {
    authorization.require(s, Permission.MANAGE_RESOURCES);
    Resource r = required(id);
    repository.update(copy(id, r, active));
  }

  private Resource required(long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Resource was not found: " + id));
  }

  private static Resource copy(long id, Resource r, boolean active) {
    return new Resource(
        id, trim(r.name()), trim(r.category()), trim(r.unit()), r.minimumStockThreshold(), active);
  }

  private static String trim(String s) {
    return s == null ? null : s.trim();
  }
}
