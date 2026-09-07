package com.reliefsync.service;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.AuthenticationException;
import com.reliefsync.model.User;
import com.reliefsync.model.enums.Role;
import com.reliefsync.repository.sqlite.SQLiteUserRepository;
import com.reliefsync.security.PasswordHash;
import com.reliefsync.security.PasswordHasher;
import com.reliefsync.security.SessionManager;
import com.reliefsync.security.UserSession;
import com.reliefsync.support.TestDatabase;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthenticationServiceTest {
  @TempDir Path directory;
  private AuthenticationService authentication;
  private SessionManager sessions;

  @BeforeEach
  void setUp() {
    DatabaseManager manager = TestDatabase.migrated(directory);
    SQLiteUserRepository users = new SQLiteUserRepository(manager);
    PasswordHasher hasher = new PasswordHasher();
    PasswordHash credentials = hasher.hash("valid-password".toCharArray());
    users.save(
        new User(
            0,
            "Active User",
            "active",
            credentials.hash(),
            credentials.salt(),
            Role.RELIEF_COORDINATOR,
            true,
            LocalDateTime.now()));
    users.save(
        new User(
            0,
            "Inactive User",
            "inactive",
            credentials.hash(),
            credentials.salt(),
            Role.VOLUNTEER,
            false,
            LocalDateTime.now()));
    sessions = new SessionManager();
    authentication = new AuthenticationService(users, hasher, sessions);
  }

  @Test
  void givenValidActiveUser_whenLoginAndLogout_thenSessionLifecycleIsCorrect() {
    UserSession session = authentication.login("ACTIVE", "valid-password".toCharArray());
    assertEquals(Role.RELIEF_COORDINATOR, session.role());
    assertTrue(sessions.isLoggedIn());
    assertEquals(session, sessions.requireCurrentSession());
    authentication.logout();
    assertFalse(sessions.isLoggedIn());
    assertTrue(sessions.getCurrentSession().isEmpty());
  }

  @Test
  void givenInvalidCredentials_whenLoginAttempted_thenGenericFailureAndNoSessionResult() {
    assertThrows(
        AuthenticationException.class,
        () -> authentication.login("active", "wrong-password".toCharArray()));
    assertThrows(
        AuthenticationException.class,
        () -> authentication.login("unknown", "valid-password".toCharArray()));
    assertThrows(
        AuthenticationException.class,
        () -> authentication.login("inactive", "valid-password".toCharArray()));
    assertFalse(sessions.isLoggedIn());
  }
}
