package com.reliefsync.application;

import com.reliefsync.database.DatabaseHealthCheck;
import com.reliefsync.database.DatabaseManager;
import com.reliefsync.database.DatabaseSeeder;
import com.reliefsync.database.MigrationRunner;
import com.reliefsync.database.SchemaInitializer;
import com.reliefsync.database.TransactionManager;
import com.reliefsync.repository.*;
import com.reliefsync.repository.sqlite.*;
import com.reliefsync.security.AuthorizationService;
import com.reliefsync.security.PasswordHasher;
import com.reliefsync.security.SessionManager;
import com.reliefsync.service.*;
import com.reliefsync.service.AuthenticationService;
import com.reliefsync.validation.*;
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
  private final DisasterEventService disasterEventService;
  private final AffectedAreaService affectedAreaService;
  private final ReliefCenterService reliefCenterService;
  private final ResourceService resourceService;
  private final InventoryService inventoryService;
  private final VehicleService vehicleService;
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
    DisasterEventRepository disasters = new SQLiteDisasterEventRepository(databaseManager);
    AffectedAreaRepository areas = new SQLiteAffectedAreaRepository(databaseManager);
    ReliefCenterRepository centers = new SQLiteReliefCenterRepository(databaseManager);
    ResourceRepository resources = new SQLiteResourceRepository(databaseManager);
    CenterInventoryRepository inventory = new SQLiteCenterInventoryRepository(databaseManager);
    VehicleRepository vehicles = new SQLiteVehicleRepository(databaseManager);
    this.disasterEventService =
        new DisasterEventService(disasters, authorizationService, new DisasterEventValidator());
    this.affectedAreaService =
        new AffectedAreaService(
            areas, disasters, authorizationService, new AffectedAreaValidator());
    this.reliefCenterService =
        new ReliefCenterService(centers, authorizationService, new ReliefCenterValidator());
    this.resourceService =
        new ResourceService(resources, authorizationService, new ResourceValidator());
    this.inventoryService =
        new InventoryService(
            inventory,
            new SQLiteInventoryTransactionRepository(transactionManager),
            new SQLiteInventoryQueryRepository(databaseManager),
            centers,
            resources,
            authorizationService);
    this.vehicleService =
        new VehicleService(vehicles, centers, authorizationService, new VehicleValidator());
    MigrationRunner migrationRunner = new MigrationRunner(databaseManager);
    DatabaseSeeder databaseSeeder = new DatabaseSeeder(transactionManager, passwordHasher);
    this.schemaInitializer = new SchemaInitializer(migrationRunner, databaseSeeder);
    this.databaseHealthCheck = new DatabaseHealthCheck(databaseManager);
    this.controllerFactory = new ControllerFactory(this);
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

  public DisasterEventService disasterEventService() {
    return disasterEventService;
  }

  public AffectedAreaService affectedAreaService() {
    return affectedAreaService;
  }

  public ReliefCenterService reliefCenterService() {
    return reliefCenterService;
  }

  public ResourceService resourceService() {
    return resourceService;
  }

  public InventoryService inventoryService() {
    return inventoryService;
  }

  public VehicleService vehicleService() {
    return vehicleService;
  }
}
