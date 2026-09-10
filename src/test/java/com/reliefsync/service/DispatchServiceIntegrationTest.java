package com.reliefsync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.db.Database;
import com.reliefsync.model.DispatchManifest;
import com.reliefsync.model.DeliveryFailure;
import com.reliefsync.model.DeliveryRecoveryAction;
import com.reliefsync.model.DraftItem;
import com.reliefsync.model.ManifestStatus;
import com.reliefsync.model.Priority;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.Role;
import com.reliefsync.model.User;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.repository.AreaRepository;
import com.reliefsync.repository.AllocationRepository;
import com.reliefsync.repository.CenterRepository;
import com.reliefsync.repository.DeliveryFailureRepository;
import com.reliefsync.repository.DispatchManifestRepository;
import com.reliefsync.repository.InventoryRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.ResourceRepository;
import com.reliefsync.repository.UserRepository;
import com.reliefsync.repository.VehicleRepository;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DispatchServiceIntegrationTest {

    @TempDir
    Path tempDir;

    private final RequestService requestService = new RequestService();
    private final AllocationService allocationService = new AllocationService();
    private final DispatchService dispatchService = new DispatchService();
    private final VehicleService vehicleService = new VehicleService();
    private final VehicleRepository vehicles = new VehicleRepository();
    private final DispatchManifestRepository manifests = new DispatchManifestRepository();
    private final DeliveryFailureRepository failures = new DeliveryFailureRepository();
    private final AllocationRepository allocations = new AllocationRepository();
    private final InventoryRepository inventory = new InventoryRepository();
    private final RequestRepository requests = new RequestRepository();

    private User volunteer;
    private User areaCoordinator;
    private User reliefCoordinator;
    private User transport;

    @BeforeEach
    void setUp() {
        Database.init("jdbc:sqlite:" + tempDir.resolve("dispatch.db"));
        UserRepository users = new UserRepository();
        users.insert("vol", "Volunteer", Role.VOLUNTEER, "x");
        users.insert("area", "Area Coordinator", Role.AREA_COORDINATOR, "x");
        users.insert("relief", "Relief Coordinator", Role.RELIEF_COORDINATOR, "x");
        users.insert("transport", "Transport Coordinator", Role.TRANSPORT_COORDINATOR, "x");
        volunteer = users.findByUsername("vol").orElseThrow();
        areaCoordinator = users.findByUsername("area").orElseThrow();
        reliefCoordinator = users.findByUsername("relief").orElseThrow();
        transport = users.findByUsername("transport").orElseThrow();

        new AreaRepository().insert("Flood Area", "District", 1000, 5);
        CenterRepository centers = new CenterRepository();
        centers.insert("Center A", "A", 1000);
        centers.insert("Center B", "B", 1000);
        new ResourceRepository().insert("Water", "litre", 10);
        InventoryRepository inventory = new InventoryRepository();
        inventory.upsertQuantity(1, 1, 100);
        inventory.upsertQuantity(2, 1, 50);
    }

    @AfterEach
    void tearDown() {
        Database.reset();
    }

    private long allocatedRequest(int quantity) {
        long id = requestService.createDraft(volunteer, 1, Priority.HIGH, "transport test",
                List.of(new DraftItem(1, "Water", quantity)));
        requestService.submit(volunteer, id);
        requestService.decideVerification(areaCoordinator, id, true, "");
        requestService.decideVerification(reliefCoordinator, id, true, "");
        allocationService.allocate(reliefCoordinator, id, "Fewest Centers");
        return id;
    }

    private long dispatchedRequest(int quantity, long vehicleId) {
        long requestId = allocatedRequest(quantity);
        dispatchService.dispatch(transport, requestId, vehicleId, "Initial Driver");
        return requestId;
    }

    private int totalInventory() {
        return inventory.availableStock().stream().mapToInt(stock -> stock.quantity()).sum();
    }

    @Test
    void dispatchCreatesCompleteMultiCenterManifestAndDeliveryReleasesVehicle() {
        long requestId = allocatedRequest(120);
        long vehicleId = vehicleService.save(transport, null, "trk-100", "Cargo Truck", 200);

        dispatchService.dispatch(transport, requestId, vehicleId, "  Amina Begum  ");
        assertEquals(RequestStatus.DISPATCHED, requestService.load(requestId).status());
        DispatchManifest manifest = manifests.findByRequest(requestId).orElseThrow();
        assertEquals(ManifestStatus.DISPATCHED, manifest.status());
        assertEquals("TRK-100", manifest.vehicleRegistration());
        assertEquals("Amina Begum", manifest.driverName());
        assertEquals(120, manifest.totalLoad());
        assertEquals(2, manifests.items(manifest.id()).size());
        assertEquals(VehicleStatus.IN_TRANSIT, vehicles.findById(vehicleId).orElseThrow().status());
        assertNotNull(manifest.dispatchedAt());

        dispatchService.deliver(transport, requestId);
        DispatchManifest delivered = manifests.findByRequest(requestId).orElseThrow();
        assertEquals(RequestStatus.DELIVERED, requestService.load(requestId).status());
        assertEquals(ManifestStatus.DELIVERED, delivered.status());
        assertNotNull(delivered.deliveredAt());
        assertEquals(VehicleStatus.AVAILABLE, vehicles.findById(vehicleId).orElseThrow().status());
        assertThrows(IllegalStateException.class, () -> dispatchService.deliver(transport, requestId));
    }

    @Test
    void capacityFailureLeavesRequestVehicleAndManifestUnchanged() {
        long requestId = allocatedRequest(120);
        long vehicleId = vehicleService.save(transport, null, "SMALL-1", "Van", 119);

        assertThrows(IllegalStateException.class,
                () -> dispatchService.dispatch(transport, requestId, vehicleId, "Driver"));
        assertEquals(RequestStatus.ALLOCATED, requestService.load(requestId).status());
        assertEquals(VehicleStatus.AVAILABLE, vehicles.findById(vehicleId).orElseThrow().status());
        assertTrue(manifests.findByRequest(requestId).isEmpty());
    }

    @Test
    void unavailableVehicleAndUnauthorizedActorAreRejected() {
        long requestId = allocatedRequest(50);
        long vehicleId = vehicleService.save(transport, null, "VAN-2", "Van", 100);
        vehicleService.setStatus(transport, vehicleId, VehicleStatus.MAINTENANCE);

        assertThrows(IllegalStateException.class,
                () -> dispatchService.dispatch(transport, requestId, vehicleId, "Driver"));
        assertThrows(IllegalStateException.class,
                () -> dispatchService.dispatch(volunteer, requestId, vehicleId, "Driver"));
        vehicleService.setStatus(transport, vehicleId, VehicleStatus.INACTIVE);
        assertThrows(IllegalStateException.class,
                () -> dispatchService.dispatch(transport, requestId, vehicleId, "Driver"));
    }

    @Test
    void oneInTransitVehicleCannotDispatchTwoRequests() {
        long first = allocatedRequest(50);
        long second = allocatedRequest(50);
        long vehicleId = vehicleService.save(transport, null, "TRK-2", "Truck", 100);
        dispatchService.dispatch(transport, first, vehicleId, "Driver One");

        assertThrows(IllegalStateException.class,
                () -> dispatchService.dispatch(transport, second, vehicleId, "Driver Two"));
        assertEquals(RequestStatus.ALLOCATED, requestService.load(second).status());
        assertTrue(manifests.findByRequest(second).isEmpty());
    }

    @Test
    void vehicleValidationAndManualInTransitChangesAreEnforced() {
        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.save(transport, null, "", "Truck", 100));
        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.save(transport, null, "TRK-3", "", 100));
        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.save(transport, null, "TRK-3", "Truck", 0));
        long vehicleId = vehicleService.save(transport, null, "TRK-3", "Truck", 100);
        assertThrows(IllegalStateException.class,
                () -> vehicleService.save(transport, null, "TRK-3", "Van", 100));
        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.setStatus(transport, vehicleId, VehicleStatus.IN_TRANSIT));
    }

    @Test
    void blankDriverAndRepeatedDispatchAreRejected() {
        long requestId = allocatedRequest(50);
        long vehicleId = vehicleService.save(transport, null, "TRK-4", "Truck", 100);
        assertThrows(IllegalArgumentException.class,
                () -> dispatchService.dispatch(transport, requestId, vehicleId, "  "));
        dispatchService.dispatch(transport, requestId, vehicleId, "Driver");
        assertThrows(IllegalStateException.class,
                () -> dispatchService.dispatch(transport, requestId, vehicleId, "Driver"));
    }

    @Test
    void dispatchFailureRollsBackVehicleManifestAndRequest() throws Exception {
        long requestId = allocatedRequest(50);
        long vehicleId = vehicleService.save(transport, null, "TRK-5", "Truck", 100);
        try (var statement = Database.getInstance().connection().createStatement()) {
            statement.execute("CREATE TRIGGER fail_manifest_item BEFORE INSERT ON dispatch_manifest_items "
                    + "BEGIN SELECT RAISE(ABORT, 'forced dispatch failure'); END");
        }

        assertThrows(IllegalStateException.class,
                () -> dispatchService.dispatch(transport, requestId, vehicleId, "Driver"));
        assertEquals(RequestStatus.ALLOCATED, requestService.load(requestId).status());
        assertEquals(VehicleStatus.AVAILABLE, vehicles.findById(vehicleId).orElseThrow().status());
        assertTrue(manifests.findByRequest(requestId).isEmpty());
        assertFalse(requests.history(requestId).stream()
                .anyMatch(change -> RequestStatus.DISPATCHED.name().equals(change.toStatus())));
    }

    @Test
    void deliveryFailureRollsBackManifestVehicleAndRequest() throws Exception {
        long requestId = allocatedRequest(50);
        long vehicleId = vehicleService.save(transport, null, "TRK-6", "Truck", 100);
        dispatchService.dispatch(transport, requestId, vehicleId, "Driver");
        try (var statement = Database.getInstance().connection().createStatement()) {
            statement.execute("CREATE TRIGGER fail_delivery BEFORE UPDATE ON dispatch_manifests "
                    + "WHEN NEW.status='DELIVERED' BEGIN SELECT RAISE(ABORT, 'forced delivery failure'); END");
        }

        assertThrows(IllegalStateException.class, () -> dispatchService.deliver(transport, requestId));
        assertEquals(RequestStatus.DISPATCHED, requestService.load(requestId).status());
        assertEquals(ManifestStatus.DISPATCHED, manifests.findByRequest(requestId).orElseThrow().status());
        assertEquals(VehicleStatus.IN_TRANSIT, vehicles.findById(vehicleId).orElseThrow().status());
    }

    @Test
    void deliveryWithoutManifestIsRejected() {
        long requestId = allocatedRequest(50);
        requests.updateStatus(requestId, RequestStatus.DISPATCHED);

        assertThrows(IllegalStateException.class, () -> dispatchService.deliver(transport, requestId));
        assertEquals(RequestStatus.DISPATCHED, requestService.load(requestId).status());
    }

    @Test
    void reportFailureStoresAuditAndReleasesVehicleButNotInventory() {
        long vehicleId = vehicleService.save(transport, null, "FAIL-1", "Truck", 100);
        long requestId = dispatchedRequest(50, vehicleId);
        int stockAfterAllocation = totalInventory();

        dispatchService.reportDeliveryFailure(transport, requestId, "  Bridge   became impassable  ",
                DeliveryRecoveryAction.RETRY, "Wait for clearance");

        assertEquals(RequestStatus.DELIVERY_FAILED, requestService.load(requestId).status());
        DispatchManifest manifest = manifests.findByRequest(requestId).orElseThrow();
        assertEquals(ManifestStatus.DELIVERY_FAILED, manifest.status());
        assertNotNull(manifest.failedAt());
        assertEquals(VehicleStatus.AVAILABLE, vehicles.findById(vehicleId).orElseThrow().status());
        assertEquals(stockAfterAllocation, totalInventory());
        DeliveryFailure failure = failures.latestForRequest(requestId).orElseThrow();
        assertEquals("Bridge became impassable", failure.reason());
        assertEquals(transport.id(), failure.reportedBy());
        assertEquals("Transport Coordinator", failure.reporterName());
        assertEquals(DeliveryRecoveryAction.RETRY, failure.recoveryAction());
        assertEquals("Wait for clearance", failure.recoveryNotes());
        assertFalse(failure.resolved());
        assertNotNull(failure.reportedAt());
        assertTrue(requests.history(requestId).stream().anyMatch(change ->
                RequestStatus.DISPATCHED.name().equals(change.fromStatus())
                        && RequestStatus.DELIVERY_FAILED.name().equals(change.toStatus())));

        assertThrows(IllegalStateException.class, () -> dispatchService.reportDeliveryFailure(
                transport, requestId, "Duplicate", DeliveryRecoveryAction.RETRY, null));
        assertThrows(IllegalStateException.class, () -> dispatchService.deliver(transport, requestId));
    }

    @Test
    void retryPreservesFailedAttemptAndCreatesNewAttemptWithSameVehicle() {
        long vehicleId = vehicleService.save(transport, null, "RETRY-1", "Truck", 100);
        long requestId = dispatchedRequest(50, vehicleId);
        long failedManifestId = manifests.findByRequest(requestId).orElseThrow().id();
        dispatchService.reportDeliveryFailure(transport, requestId, "Road blocked",
                DeliveryRecoveryAction.RETRY, null);

        dispatchService.retryDelivery(transport, requestId, vehicleId, "Retry Driver");

        assertEquals(RequestStatus.DISPATCHED, requestService.load(requestId).status());
        List<DispatchManifest> attempts = manifests.allForRequest(requestId);
        assertEquals(2, attempts.size());
        assertEquals(failedManifestId, attempts.get(0).id());
        assertEquals(ManifestStatus.DELIVERY_FAILED, attempts.get(0).status());
        assertEquals(1, attempts.get(0).attemptNumber());
        assertEquals(ManifestStatus.DISPATCHED, attempts.get(1).status());
        assertEquals(2, attempts.get(1).attemptNumber());
        assertEquals("Retry Driver", attempts.get(1).driverName());
        assertEquals(1, manifests.items(attempts.get(1).id()).size());
        assertEquals(VehicleStatus.IN_TRANSIT, vehicles.findById(vehicleId).orElseThrow().status());
        assertTrue(failures.latestForRequest(requestId).orElseThrow().resolved());
    }

    @Test
    void retryWithReplacementVehicleValidatesCapacityAndRollsBackCleanly() {
        long original = vehicleService.save(transport, null, "ORIGINAL-1", "Truck", 200);
        long tooSmall = vehicleService.save(transport, null, "SMALL-RETRY", "Van", 49);
        long replacement = vehicleService.save(transport, null, "REPLACE-1", "Truck", 100);
        long requestId = dispatchedRequest(50, original);
        dispatchService.reportDeliveryFailure(transport, requestId, "Engine problem",
                DeliveryRecoveryAction.RETRY, null);

        assertThrows(IllegalStateException.class,
                () -> dispatchService.retryDelivery(transport, requestId, tooSmall, "Driver"));
        assertEquals(RequestStatus.DELIVERY_FAILED, requestService.load(requestId).status());
        assertEquals(1, manifests.allForRequest(requestId).size());
        assertEquals(VehicleStatus.AVAILABLE, vehicles.findById(tooSmall).orElseThrow().status());
        assertFalse(failures.latestForRequest(requestId).orElseThrow().resolved());

        dispatchService.retryDelivery(transport, requestId, replacement, "Replacement Driver");
        DispatchManifest latest = manifests.findByRequest(requestId).orElseThrow();
        assertEquals(replacement, latest.vehicleId());
        assertEquals(ManifestStatus.DISPATCHED, latest.status());
        assertEquals(VehicleStatus.AVAILABLE, vehicles.findById(original).orElseThrow().status());
        assertEquals(VehicleStatus.IN_TRANSIT, vehicles.findById(replacement).orElseThrow().status());
    }

    @Test
    void retryDatabaseFailureLeavesFailedRequestAndVehicleUnchanged() throws Exception {
        long original = vehicleService.save(transport, null, "RETRY-ROLLBACK-OLD", "Truck", 100);
        long replacement = vehicleService.save(transport, null, "RETRY-ROLLBACK-NEW", "Truck", 100);
        long requestId = dispatchedRequest(50, original);
        dispatchService.reportDeliveryFailure(transport, requestId, "Vehicle unavailable",
                DeliveryRecoveryAction.RETRY, null);
        try (var statement = Database.getInstance().connection().createStatement()) {
            statement.execute("CREATE TRIGGER reject_retry_item BEFORE INSERT ON dispatch_manifest_items "
                    + "BEGIN SELECT RAISE(ABORT, 'forced retry failure'); END");
        }

        assertThrows(IllegalStateException.class,
                () -> dispatchService.retryDelivery(transport, requestId, replacement, "New Driver"));
        assertEquals(RequestStatus.DELIVERY_FAILED, requestService.load(requestId).status());
        assertEquals(1, manifests.allForRequest(requestId).size());
        assertEquals(ManifestStatus.DELIVERY_FAILED, manifests.findByRequest(requestId).orElseThrow().status());
        assertEquals(VehicleStatus.AVAILABLE, vehicles.findById(replacement).orElseThrow().status());
        assertFalse(failures.latestForRequest(requestId).orElseThrow().resolved());
    }

    @Test
    void returnForReallocationRestoresStockOnceAndLeavesAuditTrail() {
        long vehicleId = vehicleService.save(transport, null, "REALLOC-1", "Truck", 100);
        long requestId = dispatchedRequest(50, vehicleId);
        assertEquals(100, totalInventory());
        dispatchService.reportDeliveryFailure(transport, requestId, "Destination inaccessible",
                DeliveryRecoveryAction.REALLOCATE, "Cargo returned to the center");

        ReallocationRecoveryResult result = dispatchService.returnForReallocation(
                reliefCoordinator, requestId, "Stock checked and returned");

        assertEquals(1, result.releasedAllocations());
        assertEquals(50, result.releasedQuantity());
        assertEquals(150, totalInventory());
        assertEquals(RequestStatus.ALLOCATED, requestService.load(requestId).status());
        assertTrue(allocations.activeForRequest(requestId).isEmpty());
        assertEquals(0, requests.items(requestId).getFirst().quantityAllocated());
        assertEquals(1, allocations.eventsForRequest(requestId).stream()
                .filter(event -> event.eventType() == com.reliefsync.model.AllocationEventType.RELEASED).count());
        assertTrue(failures.latestForRequest(requestId).orElseThrow().resolved());
        assertEquals("Stock checked and returned",
                failures.latestForRequest(requestId).orElseThrow().recoveryNotes());
        assertThrows(IllegalStateException.class, () -> dispatchService.returnForReallocation(
                reliefCoordinator, requestId, null));
        assertEquals(150, totalInventory());

        allocationService.reallocate(reliefCoordinator, requestId, "Fewest Centers");
        assertEquals(50, requests.items(requestId).getFirst().quantityAllocated());
    }

    @Test
    void recoveryActionAndPermissionsAreEnforced() {
        long vehicleId = vehicleService.save(transport, null, "AUTH-FAIL", "Truck", 100);
        long requestId = dispatchedRequest(50, vehicleId);
        assertThrows(IllegalStateException.class,
                () -> dispatchService.retryDelivery(transport, requestId, vehicleId, "Driver"));
        assertThrows(IllegalStateException.class, () -> dispatchService.reportDeliveryFailure(
                volunteer, requestId, "No access", DeliveryRecoveryAction.RETRY, null));
        assertThrows(IllegalArgumentException.class, () -> dispatchService.reportDeliveryFailure(
                transport, requestId, " ", DeliveryRecoveryAction.RETRY, null));
        dispatchService.reportDeliveryFailure(transport, requestId, "Route unavailable",
                DeliveryRecoveryAction.REALLOCATE, null);
        assertThrows(IllegalStateException.class,
                () -> dispatchService.retryDelivery(transport, requestId, vehicleId, "Driver"));
        assertThrows(IllegalStateException.class,
                () -> dispatchService.returnForReallocation(transport, requestId, null));
    }

    @Test
    void failureReportingRollbackRestoresManifestVehicleAndRequest() throws Exception {
        long vehicleId = vehicleService.save(transport, null, "ROLLBACK-FAIL", "Truck", 100);
        long requestId = dispatchedRequest(50, vehicleId);
        try (var statement = Database.getInstance().connection().createStatement()) {
            statement.execute("CREATE TRIGGER reject_failure BEFORE INSERT ON delivery_failures "
                    + "BEGIN SELECT RAISE(ABORT, 'forced failure audit error'); END");
        }

        assertThrows(IllegalStateException.class, () -> dispatchService.reportDeliveryFailure(
                transport, requestId, "Road closed", DeliveryRecoveryAction.RETRY, null));
        assertEquals(RequestStatus.DISPATCHED, requestService.load(requestId).status());
        assertEquals(ManifestStatus.DISPATCHED, manifests.findByRequest(requestId).orElseThrow().status());
        assertEquals(VehicleStatus.IN_TRANSIT, vehicles.findById(vehicleId).orElseThrow().status());
        assertTrue(failures.forRequest(requestId).isEmpty());
    }

    @Test
    void reallocationRollbackPreventsPartialStockReturn() throws Exception {
        long vehicleId = vehicleService.save(transport, null, "ROLLBACK-REALLOC", "Truck", 100);
        long requestId = dispatchedRequest(50, vehicleId);
        dispatchService.reportDeliveryFailure(transport, requestId, "Area evacuated",
                DeliveryRecoveryAction.REALLOCATE, null);
        try (var statement = Database.getInstance().connection().createStatement()) {
            statement.execute("CREATE TRIGGER reject_release_event BEFORE INSERT ON allocation_events "
                    + "WHEN NEW.event_type='RELEASED' "
                    + "BEGIN SELECT RAISE(ABORT, 'forced release audit error'); END");
        }

        assertThrows(IllegalStateException.class,
                () -> dispatchService.returnForReallocation(reliefCoordinator, requestId, null));
        assertEquals(RequestStatus.DELIVERY_FAILED, requestService.load(requestId).status());
        assertEquals(100, totalInventory());
        assertEquals(1, allocations.activeForRequest(requestId).size());
        assertFalse(failures.latestForRequest(requestId).orElseThrow().resolved());
    }
}
