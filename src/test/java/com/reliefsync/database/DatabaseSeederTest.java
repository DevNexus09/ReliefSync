package com.reliefsync.database;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefsync.model.User;
import com.reliefsync.repository.sqlite.SQLiteResourceRepository;
import com.reliefsync.repository.sqlite.SQLiteUserRepository;
import com.reliefsync.security.PasswordHasher;
import com.reliefsync.security.SessionManager;
import com.reliefsync.service.AuthenticationService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseSeederTest {
  @TempDir Path directory;

  @Test
  void givenDemoMode_whenInitializedTwice_thenAdminAndResourcesAreSeededOnceAndLoginWorks() {
    DatabaseManager manager = manager();
    PasswordHasher hasher = new PasswordHasher();
    DatabaseSeeder seeder = new DatabaseSeeder(new TransactionManager(manager), hasher);
    SchemaInitializer initializer =
        new SchemaInitializer(new MigrationRunner(manager), seeder, () -> true);

    initializer.initialize();
    initializer.initialize();

    SQLiteUserRepository users = new SQLiteUserRepository(manager);
    User admin = users.findByUsername(DatabaseSeeder.DEMO_USERNAME).orElseThrow();
    assertNotEquals(DatabaseSeeder.DEMO_PASSWORD, admin.passwordHash());
    assertNotEquals(DatabaseSeeder.DEMO_PASSWORD, admin.passwordSalt());
    assertEquals(1, users.findAll().size());
    assertEquals(6, new SQLiteResourceRepository(manager).findAll().size());
    AuthenticationService authentication =
        new AuthenticationService(users, hasher, new SessionManager());
    assertDoesNotThrow(
        () ->
            authentication.login(
                DatabaseSeeder.DEMO_USERNAME, DatabaseSeeder.DEMO_PASSWORD.toCharArray()));
  }

  @Test
  void givenNormalMode_whenInitialized_thenMigrationsRunWithoutDemoData() {
    DatabaseManager manager = manager();
    PasswordHasher hasher = new PasswordHasher();
    DatabaseSeeder seeder = new DatabaseSeeder(new TransactionManager(manager), hasher);

    new SchemaInitializer(new MigrationRunner(manager), seeder, () -> false).initialize();

    assertTrue(new SQLiteUserRepository(manager).findAll().isEmpty());
    assertTrue(new SQLiteResourceRepository(manager).findAll().isEmpty());
  }

  private DatabaseManager manager() {
    return new DatabaseManager(new DatabaseConfig(directory.resolve("seed.db")));
  }
}
