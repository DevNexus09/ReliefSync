package com.reliefsync.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.*;
import com.reliefsync.model.Resource;
import com.reliefsync.model.enums.*;
import com.reliefsync.repository.sqlite.*;
import com.reliefsync.support.TestDatabase;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkflowRepositoryTest {
  @TempDir Path directory;
  private DatabaseManager manager;
  private Fixture fixture;

  @BeforeEach
  void setUp() {
    manager = TestDatabase.migrated(directory);
    fixture = createFixture();
  }

  @Test
  void givenValidWorkflowRows_whenSaved_thenEveryRepositoryRetrievesItsData() {
    SQLiteReliefRequestItemRepository requestItems = new SQLiteReliefRequestItemRepository(manager);
    long requestItemId =
        requestItems.save(
            new ReliefRequestItem(0, fixture.requestId, fixture.resourceId, 100, 60, 20));
    assertEquals(80, requestItems.findById(requestItemId).orElseThrow().getRemainingQuantity());
    assertEquals(
        20.0,
        requestItems.findByRequestId(fixture.requestId).getFirst().getFulfillmentPercentage());

    SQLiteVerificationRecordRepository verifications =
        new SQLiteVerificationRecordRepository(manager);
    long verificationId =
        verifications.save(
            new VerificationRecord(
                0,
                fixture.requestId,
                "AREA",
                fixture.userId,
                VerificationDecision.APPROVED,
                "Verified",
                1,
                LocalDateTime.now()));
    assertTrue(verifications.findById(verificationId).isPresent());

    SQLiteAllocationRepository allocations = new SQLiteAllocationRepository(manager);
    long allocationId =
        allocations.save(
            new Allocation(
                0,
                fixture.requestId,
                "PRIORITY",
                AllocationStatus.CONFIRMED,
                fixture.userId,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null));
    assertEquals(1, allocations.findByRequestId(fixture.requestId).size());

    SQLiteAllocationItemRepository allocationItems = new SQLiteAllocationItemRepository(manager);
    long allocationItemId =
        allocationItems.save(
            new AllocationItem(0, allocationId, fixture.centerId, fixture.resourceId, 60, 40, 20));
    assertTrue(allocationItems.findById(allocationItemId).isPresent());

    SQLiteDispatchRepository dispatches = new SQLiteDispatchRepository(manager);
    long dispatchId =
        dispatches.save(
            new Dispatch(
                0,
                allocationId,
                fixture.vehicleId,
                fixture.centerId,
                fixture.areaId,
                DispatchStatus.IN_TRANSIT,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null));
    assertEquals(1, dispatches.findByStatus(DispatchStatus.IN_TRANSIT).size());

    SQLiteDispatchItemRepository dispatchItems = new SQLiteDispatchItemRepository(manager);
    long dispatchItemId =
        dispatchItems.save(
            new DispatchItem(0, dispatchId, allocationItemId, fixture.resourceId, 40, 20));
    assertTrue(dispatchItems.findById(dispatchItemId).isPresent());
  }

  @Test
  void givenInvalidRequestItems_whenSaved_thenQuantityAndUniquenessChecksRejectThem() {
    SQLiteReliefRequestItemRepository repository = new SQLiteReliefRequestItemRepository(manager);
    assertThrows(
        PersistenceException.class,
        () ->
            repository.save(
                new ReliefRequestItem(0, fixture.requestId, fixture.resourceId, 0, 0, 0)));
    assertThrows(
        PersistenceException.class,
        () ->
            repository.save(
                new ReliefRequestItem(0, fixture.requestId, fixture.resourceId, 10, 11, 0)));
    assertThrows(
        PersistenceException.class,
        () ->
            repository.save(
                new ReliefRequestItem(0, fixture.requestId, fixture.resourceId, 10, 8, 9)));
    repository.save(new ReliefRequestItem(0, fixture.requestId, fixture.resourceId, 10, 0, 0));
    assertThrows(
        PersistenceException.class,
        () ->
            repository.save(
                new ReliefRequestItem(0, fixture.requestId, fixture.resourceId, 20, 0, 0)));
  }

  @Test
  void givenInvalidAllocationItem_whenSaved_thenTransportChecksRejectIt() {
    long allocationId =
        new SQLiteAllocationRepository(manager)
            .save(
                new Allocation(
                    0,
                    fixture.requestId,
                    "PRIORITY",
                    AllocationStatus.PREVIEW,
                    fixture.userId,
                    LocalDateTime.now(),
                    null,
                    null));
    SQLiteAllocationItemRepository repository = new SQLiteAllocationItemRepository(manager);
    assertThrows(
        PersistenceException.class,
        () ->
            repository.save(
                new AllocationItem(
                    0, allocationId, fixture.centerId, fixture.resourceId, 0, 0, 0)));
    assertThrows(
        PersistenceException.class,
        () ->
            repository.save(
                new AllocationItem(
                    0, allocationId, fixture.centerId, fixture.resourceId, 10, 11, 0)));
    assertThrows(
        PersistenceException.class,
        () ->
            repository.save(
                new AllocationItem(
                    0, allocationId, fixture.centerId, fixture.resourceId, 10, 8, 9)));
  }

  @Test
  void givenInvalidDispatchReferencesAndQuantities_whenSaved_thenDatabaseRejectsThem() {
    SQLiteDispatchRepository dispatches = new SQLiteDispatchRepository(manager);
    assertThrows(
        PersistenceException.class,
        () ->
            dispatches.save(
                new Dispatch(
                    0,
                    999_999,
                    fixture.vehicleId,
                    fixture.centerId,
                    fixture.areaId,
                    DispatchStatus.READY,
                    LocalDateTime.now(),
                    null,
                    null,
                    null)));

    long allocationId =
        new SQLiteAllocationRepository(manager)
            .save(
                new Allocation(
                    0,
                    fixture.requestId,
                    "PRIORITY",
                    AllocationStatus.CONFIRMED,
                    fixture.userId,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null));
    long allocationItemId =
        new SQLiteAllocationItemRepository(manager)
            .save(
                new AllocationItem(
                    0, allocationId, fixture.centerId, fixture.resourceId, 10, 0, 0));
    long dispatchId =
        dispatches.save(
            new Dispatch(
                0,
                allocationId,
                fixture.vehicleId,
                fixture.centerId,
                fixture.areaId,
                DispatchStatus.READY,
                LocalDateTime.now(),
                null,
                null,
                null));
    SQLiteDispatchItemRepository items = new SQLiteDispatchItemRepository(manager);
    assertThrows(
        PersistenceException.class,
        () ->
            items.save(
                new DispatchItem(0, dispatchId, allocationItemId, fixture.resourceId, 0, 0)));
    assertThrows(
        PersistenceException.class,
        () ->
            items.save(
                new DispatchItem(0, dispatchId, allocationItemId, fixture.resourceId, 5, 6)));
  }

  @Test
  void givenNotifications_whenUnreadQueriedAndMarkedRead_thenStateChanges() {
    SQLiteNotificationRepository repository = new SQLiteNotificationRepository(manager);
    long unreadId =
        repository.save(
            new Notification(
                0,
                fixture.userId,
                "REQUEST",
                "New request",
                "A request needs attention",
                false,
                LocalDateTime.now()));
    repository.save(
        new Notification(
            0, fixture.userId, "NOTICE", "Read", "Already read", true, LocalDateTime.now()));
    assertEquals(2, repository.findByUserId(fixture.userId).size());
    assertEquals(1, repository.findUnreadByUserId(fixture.userId).size());
    repository.markRead(unreadId);
    assertTrue(repository.findUnreadByUserId(fixture.userId).isEmpty());
  }

  @Test
  void givenSystemAndUserAuditEvents_whenSaved_thenNullableActorAndLookupWork() {
    SQLiteAuditEventRepository repository = new SQLiteAuditEventRepository(manager);
    repository.save(
        new AuditEvent(
            0,
            null,
            "LOW_STOCK",
            "RESOURCE",
            fixture.resourceId,
            "System detected low stock",
            LocalDateTime.now()));
    repository.save(
        new AuditEvent(
            0,
            fixture.userId,
            "LOGIN",
            "USER",
            fixture.userId,
            "User logged in",
            LocalDateTime.now()));
    assertEquals(1, repository.findByEntity("RESOURCE", fixture.resourceId).size());
    assertNull(repository.findByEntity("RESOURCE", fixture.resourceId).getFirst().actorUserId());
    assertEquals(1, repository.findByEntity("USER", fixture.userId).size());
  }

  private Fixture createFixture() {
    LocalDateTime now = LocalDateTime.now();
    long userId =
        new SQLiteUserRepository(manager)
            .save(new User(0, "Admin", "admin", "hash", "salt", Role.ADMINISTRATOR, true, now));
    long eventId =
        new SQLiteDisasterEventRepository(manager)
            .save(
                new DisasterEvent(
                    0,
                    "Flood 2026",
                    DisasterType.FLOOD,
                    "Test event",
                    LocalDate.now(),
                    null,
                    DisasterStatus.ACTIVE,
                    userId,
                    now));
    long areaId =
        new SQLiteAffectedAreaRepository(manager)
            .save(
                new AffectedArea(
                    0,
                    eventId,
                    "Area A",
                    "Dhaka",
                    23.7,
                    90.4,
                    1000,
                    250,
                    Severity.HIGH,
                    Accessibility.PARTIALLY_ACCESSIBLE,
                    MedicalUrgency.HIGH,
                    "Limited",
                    "ACTIVE",
                    null,
                    now,
                    now));
    long centerId =
        new SQLiteReliefCenterRepository(manager)
            .save(new ReliefCenter(0, "Center A", "Dhaka", 23.8, 90.4, "0123", true, now));
    long resourceId =
        new SQLiteResourceRepository(manager)
            .save(new Resource(0, "Food", "Food", "package", 10, true));
    long vehicleId =
        new SQLiteVehicleRepository(manager)
            .save(
                new Vehicle(0, "DHAKA-01", "Truck", 500, VehicleStatus.AVAILABLE, centerId, true));
    long requestId =
        new SQLiteReliefRequestRepository(manager)
            .save(
                new ReliefRequest(
                    0,
                    eventId,
                    areaId,
                    userId,
                    RequestPriority.HIGH,
                    RequestStateType.SUBMITTED,
                    "Need food",
                    now,
                    null,
                    1,
                    now,
                    now));
    return new Fixture(userId, areaId, centerId, resourceId, vehicleId, requestId);
  }

  private record Fixture(
      long userId, long areaId, long centerId, long resourceId, long vehicleId, long requestId) {}
}
