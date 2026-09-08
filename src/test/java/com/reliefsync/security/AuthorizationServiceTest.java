package com.reliefsync.security;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefsync.exception.AuthorizationException;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.enums.Role;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {
  private final AuthorizationService authorization = new AuthorizationService();

  @Test
  void givenAdministrator_whenAnyPermissionChecked_thenAllAreAllowed() {
    UserSession admin = session(Role.ADMINISTRATOR);
    for (Permission permission : Permission.values())
      assertTrue(authorization.can(admin, permission));
  }

  @Test
  void givenRolePolicy_whenAllowedAndForbiddenActionsChecked_thenPolicyIsCentralized() {
    UserSession volunteer = session(Role.VOLUNTEER);
    assertTrue(authorization.can(volunteer, Permission.VIEW_RELIEF_REQUESTS));
    assertTrue(authorization.can(volunteer, Permission.VERIFY_RELIEF_REQUEST));
    assertFalse(authorization.can(volunteer, Permission.CREATE_RELIEF_REQUEST));
    assertFalse(authorization.can(volunteer, Permission.MANAGE_USERS));
    assertDoesNotThrow(() -> authorization.require(volunteer, Permission.VIEW_AFFECTED_AREAS));
    assertThrows(
        AuthorizationException.class,
        () -> authorization.require(volunteer, Permission.MANAGE_USERS));
  }

  @Test
  void givenNoSession_whenPermissionChecked_thenAccessIsDenied() {
    assertFalse(authorization.can(null, Permission.VIEW_DISASTERS));
    assertThrows(
        AuthorizationException.class, () -> authorization.require(null, Permission.VIEW_DISASTERS));
  }

  private UserSession session(Role role) {
    return new UserSession(1, "Test User", "test", role);
  }
}
