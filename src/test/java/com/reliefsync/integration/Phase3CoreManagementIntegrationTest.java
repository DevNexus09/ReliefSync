package com.reliefsync.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefsync.database.*;
import com.reliefsync.exception.*;
import com.reliefsync.model.*;
import com.reliefsync.model.enums.*;
import com.reliefsync.model.search.*;
import com.reliefsync.repository.*;
import com.reliefsync.repository.sqlite.*;
import com.reliefsync.security.*;
import com.reliefsync.service.*;
import com.reliefsync.service.dto.InventoryAdjustmentResult;
import com.reliefsync.validation.*;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Phase3CoreManagementIntegrationTest {
  @TempDir Path directory;
  DatabaseManager database;
  UserSession admin;
  AuthorizationService auth;
  DisasterEventService disasters;
  AffectedAreaService areas;
  ReliefCenterService centers;
  ResourceService resources;
  InventoryService inventory;
  VehicleService vehicles;
  DisasterEventRepository disasterRepo;
  ReliefCenterRepository centerRepo;
  ResourceRepository resourceRepo;
  CenterInventoryRepository inventoryRepo;

  @BeforeEach
  void setUp() {
    database = new DatabaseManager(new DatabaseConfig(directory.resolve("phase3.db")));
    new MigrationRunner(database).runMigrations();
    PasswordHasher hasher = new PasswordHasher();
    new DatabaseSeeder(new TransactionManager(database), hasher).seedDemoData();
    User user =
        new SQLiteUserRepository(database)
            .findByUsername(DatabaseSeeder.DEMO_USERNAME)
            .orElseThrow();
    admin = new UserSession(user.id(), user.fullName(), user.username(), user.role());
    auth = new AuthorizationService();
    disasterRepo = new SQLiteDisasterEventRepository(database);
    AffectedAreaRepository areaRepo = new SQLiteAffectedAreaRepository(database);
    centerRepo = new SQLiteReliefCenterRepository(database);
    resourceRepo = new SQLiteResourceRepository(database);
    inventoryRepo = new SQLiteCenterInventoryRepository(database);
    VehicleRepository vehicleRepo = new SQLiteVehicleRepository(database);
    disasters = new DisasterEventService(disasterRepo, auth, new DisasterEventValidator());
    areas = new AffectedAreaService(areaRepo, disasterRepo, auth, new AffectedAreaValidator());
    centers = new ReliefCenterService(centerRepo, auth, new ReliefCenterValidator());
    resources = new ResourceService(resourceRepo, auth, new ResourceValidator());
    inventory =
        new InventoryService(
            inventoryRepo,
            new SQLiteInventoryTransactionRepository(new TransactionManager(database)),
            new SQLiteInventoryQueryRepository(database),
            centerRepo,
            resourceRepo,
            auth);
    vehicles = new VehicleService(vehicleRepo, centerRepo, auth, new VehicleValidator());
  }

  @Test
  void disasterLifecycleValidationSearchAndPersistence() {
    assertThrows(
        ValidationException.class,
        () -> disasters.create(event(" ", LocalDate.now(), null), admin));
    assertThrows(
        ValidationException.class,
        () -> disasters.create(event("Bad", LocalDate.now(), LocalDate.now().minusDays(1)), admin));
    DisasterEvent created = disasters.create(event("River Flood", LocalDate.now(), null), admin);
    assertEquals(
        1,
        disasters
            .search(
                new DisasterEventSearchCriteria(
                    "river", DisasterType.FLOOD, DisasterStatus.ACTIVE, null, null),
                admin)
            .size());
    disasters.close(created.id(), admin);
    assertThrows(
        BusinessRuleException.class,
        () -> disasters.update(created.id(), event("River Flood", LocalDate.now(), null), admin));
    disasters.reactivate(created.id(), admin);
    assertEquals(
        DisasterStatus.ACTIVE,
        new SQLiteDisasterEventRepository(database).findById(created.id()).orElseThrow().status());
    assertThrows(AuthorizationException.class, () -> disasters.close(created.id(), viewer()));
  }

  @Test
  void affectedAreaValidatesCoordinatesAndSearches() {
    DisasterEvent event = disasters.create(event("Cyclone", LocalDate.now(), null), admin);
    AffectedArea input = area(event.id(), "Coastal Ward", "Khulna", 22.5, 89.5);
    AffectedArea saved = areas.create(input, admin);
    assertEquals(
        saved.id(),
        areas
            .search(
                new AffectedAreaSearchCriteria(
                    event.id(),
                    "khulna",
                    Severity.HIGH,
                    Accessibility.PARTIALLY_ACCESSIBLE,
                    "ACTIVE"),
                admin)
            .getFirst()
            .id());
    assertThrows(
        ValidationException.class,
        () -> areas.create(area(event.id(), "Bad", "X", 91.0, 20.0), admin));
    assertThrows(
        ValidationException.class,
        () -> areas.create(area(event.id(), "Bad", "X", 20.0, null), admin));
    assertThrows(
        NotFoundException.class, () -> areas.create(area(999, "Bad", "X", null, null), admin));
  }

  @Test
  void centerResourceAndVehicleLifecycle() {
    ReliefCenter center =
        centers.create(
            new ReliefCenter(0, "Central Hub", "Dhaka", 23.7, 90.4, "01700", true, null), admin);
    centers.deactivate(center.id(), admin);
    centers.activate(center.id(), admin);
    assertTrue(centerRepo.findById(center.id()).orElseThrow().active());
    Resource resource =
        resources.create(new Resource(0, "Purification Tablet", "WATER", "box", 10, true), admin);
    assertThrows(
        ValidationException.class,
        () ->
            resources.create(
                new Resource(0, "purification tablet", "WATER", "BOX", 10, true), admin));
    resources.deactivate(resource.id(), admin);
    assertEquals(
        1, resources.search(new ResourceSearchCriteria("purification", null, false), admin).size());
    Vehicle vehicle =
        vehicles.register(
            new Vehicle(0, "DHK-100", "Truck", 1000, VehicleStatus.ASSIGNED, center.id(), true),
            admin);
    assertEquals(VehicleStatus.AVAILABLE, vehicle.status());
    vehicles.changeManualAvailability(vehicle.id(), VehicleStatus.UNAVAILABLE, admin);
    assertThrows(
        BusinessRuleException.class,
        () -> vehicles.changeManualAvailability(vehicle.id(), VehicleStatus.ASSIGNED, admin));
    assertThrows(
        NotFoundException.class, () -> vehicles.assignHomeCenter(vehicle.id(), 999L, admin));
    vehicles.deactivate(vehicle.id(), admin);
    assertFalse(
        vehicles
            .search(new VehicleSearchCriteria("dhk", null, null, null, false), admin)
            .getFirst()
            .active());
  }

  @Test
  void inventoryAdjustmentIsAuditedLowStockAndRollsBackOnAuditFailure() throws Exception {
    ReliefCenter center =
        centers.create(
            new ReliefCenter(0, "Warehouse", "Dhaka", null, null, null, true, null), admin);
    Resource resource =
        resources.create(new Resource(0, "Medical Box", "MEDICAL", "box", 30, true), admin);
    CenterInventory initialized =
        inventory.initializeStock(center.id(), resource.id(), 100, "Opening balance", admin);
    assertEquals(100, initialized.getAvailableQuantity());
    InventoryAdjustmentResult result =
        inventory.adjustStock(center.id(), resource.id(), -70, "Issued after count", admin);
    assertTrue(result.lowStock());
    assertEquals(30, result.inventory().totalQuantity());
    assertFalse(
        new SQLiteAuditEventRepository(database)
            .findByEntity("CENTER_INVENTORY", initialized.id())
            .isEmpty());
    assertEquals(
        1,
        inventory
            .search(new InventorySearchCriteria(center.id(), resource.id(), "MEDICAL", true), admin)
            .size());
    try (Connection c = database.openConnection();
        Statement s = c.createStatement()) {
      s.execute(
          "CREATE TRIGGER fail_phase3_audit BEFORE INSERT ON audit_events BEGIN SELECT RAISE(FAIL,"
              + " 'forced audit failure'); END");
    }
    assertThrows(
        PersistenceException.class,
        () -> inventory.adjustStock(center.id(), resource.id(), 10, "Must rollback", admin));
    assertEquals(
        30,
        inventoryRepo
            .findByCenterAndResource(center.id(), resource.id())
            .orElseThrow()
            .totalQuantity());
    assertThrows(
        ValidationException.class,
        () -> inventory.initializeStock(center.id(), resource.id(), 1, "Duplicate", admin));
    assertThrows(
        AuthorizationException.class,
        () -> inventory.adjustStock(center.id(), resource.id(), 1, "No permission", viewer()));
  }

  @Test
  void migrationsContainPhase3IndexesAndDemoRoles() throws Exception {
    try (Connection c = database.openConnection();
        Statement s = c.createStatement()) {
      assertTrue(s.executeQuery("SELECT 1 FROM schema_migrations WHERE version='V003'").next());
      assertTrue(
          s.executeQuery(
                  "SELECT 1 FROM sqlite_master WHERE type='index' AND"
                      + " name='idx_vehicle_center_status'")
              .next());
    }
    UserRepository users = new SQLiteUserRepository(database);
    assertEquals(
        Role.RELIEF_CENTER_MANAGER,
        users.findByUsername(DatabaseSeeder.CENTER_MANAGER_USERNAME).orElseThrow().role());
    assertEquals(
        Role.TRANSPORT_COORDINATOR,
        users.findByUsername(DatabaseSeeder.TRANSPORT_USERNAME).orElseThrow().role());
    assertEquals(
        Role.AREA_COORDINATOR,
        users.findByUsername(DatabaseSeeder.AREA_COORDINATOR_USERNAME).orElseThrow().role());
  }

  private DisasterEvent event(String name, LocalDate start, LocalDate end) {
    return new DisasterEvent(
        0, name, DisasterType.FLOOD, "", start, end, DisasterStatus.ACTIVE, 0, null);
  }

  private AffectedArea area(long event, String name, String district, Double lat, Double lon) {
    return new AffectedArea(
        0,
        event,
        name,
        district,
        lat,
        lon,
        100,
        20,
        Severity.HIGH,
        Accessibility.PARTIALLY_ACCESSIBLE,
        MedicalUrgency.HIGH,
        "LIMITED",
        "ACTIVE",
        "",
        null,
        null);
  }

  private UserSession viewer() {
    return new UserSession(999, "Viewer", "viewer", Role.VOLUNTEER);
  }
}
