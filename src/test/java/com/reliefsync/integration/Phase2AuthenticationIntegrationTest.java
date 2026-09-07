package com.reliefsync.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefsync.database.*;
import com.reliefsync.model.enums.Role;
import com.reliefsync.repository.sqlite.SQLiteUserRepository;
import com.reliefsync.security.PasswordHasher;
import com.reliefsync.security.SessionManager;
import com.reliefsync.security.UserSession;
import com.reliefsync.service.AuthenticationService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Phase2AuthenticationIntegrationTest {
  @TempDir Path directory;

  @Test
  void
      givenFreshDatabase_whenMigratedSeededAndAuthenticated_thenSessionCanLogoutAndLoginAfterRestart() {
    DatabaseManager manager =
        new DatabaseManager(new DatabaseConfig(directory.resolve("integration.db")));
    MigrationRunner migrations = new MigrationRunner(manager);
    migrations.runMigrations();
    PasswordHasher hasher = new PasswordHasher();
    new DatabaseSeeder(new TransactionManager(manager), hasher).seedDemoData();

    SessionManager firstSessions = new SessionManager();
    AuthenticationService firstAuthentication =
        new AuthenticationService(new SQLiteUserRepository(manager), hasher, firstSessions);
    UserSession session = firstAuthentication.login("admin", "ReliefSync@2026".toCharArray());
    assertEquals(Role.ADMINISTRATOR, session.role());
    assertTrue(firstSessions.isLoggedIn());
    firstAuthentication.logout();
    assertFalse(firstSessions.isLoggedIn());

    migrations.runMigrations();
    SessionManager restartedSessions = new SessionManager();
    AuthenticationService restartedAuthentication =
        new AuthenticationService(
            new SQLiteUserRepository(manager), new PasswordHasher(), restartedSessions);
    assertEquals(
        Role.ADMINISTRATOR,
        restartedAuthentication.login("admin", "ReliefSync@2026".toCharArray()).role());
  }
}
