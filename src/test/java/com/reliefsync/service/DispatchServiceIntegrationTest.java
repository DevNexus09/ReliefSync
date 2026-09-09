package com.reliefsync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.db.Database;
import com.reliefsync.model.DispatchManifest;
import com.reliefsync.model.DraftItem;
import com.reliefsync.model.ManifestStatus;
import com.reliefsync.model.Priority;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.Role;
import com.reliefsync.model.User;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.repository.AreaRepository;
import com.reliefsync.repository.CenterRepository;
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
}
