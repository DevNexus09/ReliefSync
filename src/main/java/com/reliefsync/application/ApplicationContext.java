package com.reliefsync.application;

import com.reliefsync.database.DatabaseHealthCheck;
import com.reliefsync.database.DatabaseManager;
import com.reliefsync.database.DatabaseSeeder;
import com.reliefsync.database.MigrationRunner;
import com.reliefsync.database.SchemaInitializer;
import com.reliefsync.database.TransactionManager;
import com.reliefsync.repository.UserRepository;
import com.reliefsync.repository.sqlite.SQLiteUserRepository;
import com.reliefsync.security.AuthorizationService;
import com.reliefsync.security.PasswordHasher;
import com.reliefsync.security.SessionManager;
import com.reliefsync.service.AuthenticationService;
import java.util.Objects;

public final class ApplicationContext {
  private final DatabaseManager databaseManager;
  private final SchemaInitializer schemaInitializer;
  private final DatabaseHealthCheck databaseHealthCheck;
  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final SessionManager sessionManager;
  private final AuthenticationService authenticationService;
  private final AuthorizationService authorizationService;
  private final ControllerFactory controllerFactory;

  public ApplicationContext() {
    this(new DatabaseManager());
  }

  public ApplicationContext(DatabaseManager databaseManager) {
    this.databaseManager = Objects.requireNonNull(databaseManager);
    TransactionManager transactionManager = new TransactionManager(databaseManager);
    this.passwordHasher = new PasswordHasher();
    this.userRepository = new SQLiteUserRepository(databaseManager);
    this.sessionManager = new SessionManager();
    this.authenticationService =
        new AuthenticationService(userRepository, passwordHasher, sessionManager);
    this.authorizationService = new AuthorizationService();
    MigrationRunner migrationRunner = new MigrationRunner(databaseManager);
    DatabaseSeeder databaseSeeder = new DatabaseSeeder(transactionManager, passwordHasher);
    this.schemaInitializer = new SchemaInitializer(migrationRunner, databaseSeeder);
    this.databaseHealthCheck = new DatabaseHealthCheck(databaseManager);
    this.controllerFactory = new ControllerFactory(authenticationService, sessionManager);
  }

  public void initializeDatabase() {
    schemaInitializer.initialize();
  }

  public DatabaseHealthCheck.HealthStatus databaseHealth() {
    return databaseHealthCheck.check();
  }

  public ControllerFactory controllerFactory() {
    return controllerFactory;
  }

  public UserRepository userRepository() {
    return userRepository;
  }

  public PasswordHasher passwordHasher() {
    return passwordHasher;
  }

  public SessionManager sessionManager() {
    return sessionManager;
  }

  public AuthenticationService authenticationService() {
    return authenticationService;
  }

  public AuthorizationService authorizationService() {
    return authorizationService;
  }

  public DatabaseManager databaseManager() {
    return databaseManager;
  }
}
