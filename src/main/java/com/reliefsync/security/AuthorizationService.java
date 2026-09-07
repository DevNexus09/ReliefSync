package com.reliefsync.security;

import com.reliefsync.exception.AuthorizationException;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.enums.Role;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public final class AuthorizationService {
  private final Map<Role, EnumSet<Permission>> policy = new EnumMap<>(Role.class);

  public AuthorizationService() {
    policy.put(Role.ADMINISTRATOR, EnumSet.allOf(Permission.class));
    policy.put(
        Role.RELIEF_COORDINATOR,
        EnumSet.of(
            Permission.VIEW_DISASTERS, Permission.VIEW_AFFECTED_AREAS,
            Permission.VIEW_RELIEF_CENTERS, Permission.VIEW_RESOURCES,
            Permission.VIEW_INVENTORY, Permission.VERIFY_RELIEF_REQUEST,
            Permission.VIEW_ALLOCATIONS, Permission.MANAGE_ALLOCATIONS,
            Permission.VIEW_DISPATCHES, Permission.VIEW_REPORTS));
    policy.put(
        Role.AREA_COORDINATOR,
        EnumSet.of(
            Permission.VIEW_DISASTERS, Permission.VIEW_AFFECTED_AREAS,
            Permission.MANAGE_AFFECTED_AREAS, Permission.CREATE_RELIEF_REQUEST,
            Permission.VIEW_ALLOCATIONS, Permission.VIEW_DISPATCHES));
    policy.put(
        Role.VOLUNTEER,
        EnumSet.of(
            Permission.VIEW_DISASTERS,
            Permission.VIEW_AFFECTED_AREAS,
            Permission.CREATE_RELIEF_REQUEST));
    policy.put(
        Role.RELIEF_CENTER_MANAGER,
        EnumSet.of(
            Permission.VIEW_DISASTERS,
            Permission.VIEW_RELIEF_CENTERS,
            Permission.VIEW_RESOURCES,
            Permission.VIEW_INVENTORY,
            Permission.MANAGE_INVENTORY,
            Permission.VIEW_ALLOCATIONS,
            Permission.VIEW_DISPATCHES));
    policy.put(
        Role.TRANSPORT_COORDINATOR,
        EnumSet.of(
            Permission.VIEW_DISASTERS, Permission.VIEW_AFFECTED_AREAS,
            Permission.VIEW_VEHICLES, Permission.MANAGE_VEHICLES,
            Permission.VIEW_DISPATCHES, Permission.MANAGE_DISPATCHES));
  }

  public boolean can(UserSession session, Permission permission) {
    if (session == null || permission == null) return false;
    return policy
        .getOrDefault(session.role(), EnumSet.noneOf(Permission.class))
        .contains(permission);
  }

  public void require(UserSession session, Permission permission) {
    if (!can(session, permission)) {
      throw new AuthorizationException("User is not allowed to perform this operation.");
    }
  }
}
