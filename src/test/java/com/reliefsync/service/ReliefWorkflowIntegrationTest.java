package com.reliefsync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.db.Database;
import com.reliefsync.model.DraftItem;
import com.reliefsync.model.AllocationEventType;
import com.reliefsync.model.Priority;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.Role;
import com.reliefsync.model.StockView;
import com.reliefsync.model.User;
import com.reliefsync.repository.AreaRepository;
import com.reliefsync.repository.AllocationRepository;
import com.reliefsync.repository.CenterRepository;
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

/** End-to-end workflow test against a real (temporary) SQLite database. */
class ReliefWorkflowIntegrationTest {

    @TempDir
    Path tempDir;

    private final RequestService requestService = new RequestService();
    private final AllocationService allocationService = new AllocationService();
    private final DispatchService dispatchService = new DispatchService();
    private final RequestRepository requests = new RequestRepository();
    private final InventoryRepository inventory = new InventoryRepository();
    private final AllocationRepository allocations = new AllocationRepository();

    private User volunteer;
    private User areaCoordinator;
    private User reliefCoordinator;
    private User transport;
    private User admin;

    private static final long AREA_ID = 1;
    private static final long CENTER_A = 1;
    private static final long CENTER_B = 2;
    private static final long WATER = 1;
    private static final long RICE = 2;

    @BeforeEach
    void setUp() {
        Database.init("jdbc:sqlite:" + tempDir.resolve("test.db"));

        UserRepository users = new UserRepository();
        users.insert("vol", "Volunteer", Role.VOLUNTEER, "x");
        users.insert("ac", "Area Coord", Role.AREA_COORDINATOR, "x");
        users.insert("rc", "Relief Coord", Role.RELIEF_COORDINATOR, "x");
        users.insert("tr", "Transport", Role.TRANSPORT_COORDINATOR, "x");
        users.insert("admin", "Administrator", Role.ADMIN, "x");
        volunteer = users.findByUsername("vol").orElseThrow();
        areaCoordinator = users.findByUsername("ac").orElseThrow();
        reliefCoordinator = users.findByUsername("rc").orElseThrow();
        transport = users.findByUsername("tr").orElseThrow();
        admin = users.findByUsername("admin").orElseThrow();

        new AreaRepository().insert("Test Area", "Test District", 1000, 5);
        CenterRepository centers = new CenterRepository();
        centers.insert("Center A", "Location A", 1000);
        centers.insert("Center B", "Location B", 1000);
        ResourceRepository resources = new ResourceRepository();
        resources.insert("Water", "litre", 10);
        resources.insert("Rice", "kg", 10);
        inventory.upsertQuantity(CENTER_A, WATER, 100);
        inventory.upsertQuantity(CENTER_B, WATER, 50);
        inventory.upsertQuantity(CENTER_A, RICE, 30);
        new VehicleRepository().insert("TEST-TRUCK-1", "Cargo Truck", 1000);
    }

    @AfterEach
    void tearDown() {
        Database.reset();
    }

    private long createHighPriorityDraft() {
        return requestService.createDraft(volunteer, AREA_ID, Priority.HIGH, "urgent",
                List.of(new DraftItem(WATER, "Water", 120), new DraftItem(RICE, "Rice", 40)));
    }

    @Test
    void fullWorkflowFromDraftToDelivered() {
        long id = createHighPriorityDraft();
        assertEquals(RequestStatus.DRAFT, requestService.load(id).status());
        assertTrue(requestService.hasOpenRequestForArea(AREA_ID));

        requestService.submit(volunteer, id);
        assertEquals(RequestStatus.SUBMITTED, requestService.load(id).status());

        // HIGH priority: two verification rounds in order
        requestService.decideVerification(areaCoordinator, id, true, "confirmed on the ground");
        assertEquals(RequestStatus.SUBMITTED, requestService.load(id).status());
        requestService.decideVerification(reliefCoordinator, id, true, "stock looks feasible");
        assertEquals(RequestStatus.VERIFIED, requestService.load(id).status());

        AllocationResult result = allocationService.allocate(reliefCoordinator, id, "Fewest Centers");
        assertEquals(RequestStatus.ALLOCATED, requestService.load(id).status());
        // 120 water: 100 from A, 20 from B; 40 rice requested but only 30 exist
        assertFalse(result.fullyCovered());
        assertEquals("Rice", result.shortages().get(0).resourceName());
        assertEquals(10, result.shortages().get(0).missing());
        assertEquals(0, quantityAt(CENTER_A, WATER));
        assertEquals(30, quantityAt(CENTER_B, WATER));
        assertEquals(0, quantityAt(CENTER_A, RICE));
        List<RequestItem> items = requests.items(id);
        assertEquals(120, items.stream().filter(i -> i.resourceId() == WATER)
                .findFirst().orElseThrow().quantityAllocated());

        dispatchService.dispatch(transport, id, 1, "Test Driver");
        assertEquals(RequestStatus.DISPATCHED, requestService.load(id).status());
        dispatchService.deliver(transport, id);
        assertEquals(RequestStatus.DELIVERED, requestService.load(id).status());
        assertFalse(requestService.hasOpenRequestForArea(AREA_ID));

        // Full audit trail: NEW->DRAFT->SUBMITTED->VERIFIED->ALLOCATED->DISPATCHED->DELIVERED
        assertEquals(6, requests.history(id).size());
    }

    @Test
    void verificationEnforcesRoundOrderAndRoles() {
        long id = createHighPriorityDraft();
        requestService.submit(volunteer, id);

        // Relief coordinator cannot take the first (area coordinator) round
        assertThrows(IllegalStateException.class,
                () -> requestService.decideVerification(reliefCoordinator, id, true, ""));
        // Volunteers have no VERIFY permission at all
        assertThrows(IllegalStateException.class,
                () -> requestService.decideVerification(volunteer, id, true, ""));
    }

    @Test
    void rejectionStopsTheWorkflow() {
        long id = createHighPriorityDraft();
        requestService.submit(volunteer, id);
        requestService.decideVerification(areaCoordinator, id, false, "not a genuine need");
        assertEquals(RequestStatus.REJECTED, requestService.load(id).status());
        assertThrows(IllegalStateException.class, () -> requestService.submit(volunteer, id));
    }

    @Test
    void stateRulesBlockIllegalActions() {
        long id = createHighPriorityDraft();
        // Cannot allocate or verify a draft
        assertThrows(IllegalStateException.class,
                () -> allocationService.allocate(reliefCoordinator, id, "Fewest Centers"));
        assertThrows(IllegalStateException.class,
                () -> requestService.decideVerification(areaCoordinator, id, true, ""));
        // Reallocation is legal only after the first reservation
        requestService.submit(volunteer, id);
        requestService.decideVerification(areaCoordinator, id, true, "");
        requestService.decideVerification(reliefCoordinator, id, true, "");
        assertThrows(IllegalStateException.class,
                () -> allocationService.reallocate(reliefCoordinator, id, "Balanced Across Centers"));
        allocationService.allocate(reliefCoordinator, id, "Balanced Across Centers");
        assertThrows(IllegalStateException.class,
                () -> allocationService.reallocate(volunteer, id, "Balanced Across Centers"));
    }

    @Test
    void draftValidationRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> requestService.createDraft(
                volunteer, AREA_ID, Priority.NORMAL, "", List.of()));
        assertThrows(IllegalArgumentException.class, () -> requestService.createDraft(
                volunteer, AREA_ID, Priority.NORMAL, "",
                List.of(new DraftItem(WATER, "Water", 0))));
        assertThrows(IllegalArgumentException.class, () -> requestService.createDraft(
                volunteer, AREA_ID, Priority.NORMAL, "",
                List.of(new DraftItem(WATER, "Water", 5), new DraftItem(WATER, "Water", 3))));
    }

    @Test
    void onlyPermittedRolesMayAllocateAndTransport() {
        long id = createHighPriorityDraft();
        requestService.submit(volunteer, id);
        requestService.decideVerification(areaCoordinator, id, true, "");
        requestService.decideVerification(reliefCoordinator, id, true, "");
        assertThrows(IllegalStateException.class,
                () -> allocationService.allocate(volunteer, id, "Fewest Centers"));
        allocationService.allocate(reliefCoordinator, id, "Fewest Centers");
        assertThrows(IllegalStateException.class,
                () -> dispatchService.dispatch(reliefCoordinator, id, 1, "Test Driver"));
        dispatchService.dispatch(transport, id, 1, "Test Driver");
    }

    @Test
    void dataSurvivesReopeningTheDatabase() {
        long id = createHighPriorityDraft();
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        Database.reset();
        Database.init(url);
        assertEquals(RequestStatus.DRAFT, requestService.load(id).status());
        assertEquals(2, requests.items(id).size());
    }

    @Test
    void reallocationAddsReservationsAndCancellationReleasesAllStock() {
        long id = createHighPriorityDraft();
        requestService.submit(volunteer, id);
        requestService.decideVerification(areaCoordinator, id, true, "");
        requestService.decideVerification(reliefCoordinator, id, true, "");

        AllocationResult first = allocationService.allocate(reliefCoordinator, id, "Fewest Centers");
        assertFalse(first.fullyCovered());
        assertEquals(3, allocations.forRequest(id).size());

        inventory.adjust(CENTER_B, RICE, 20);
        AllocationResult second = allocationService.reallocate(reliefCoordinator, id, "Fewest Centers");
        assertTrue(second.fullyCovered());
        assertEquals(1, second.lines().size());
        assertEquals(10, second.lines().getFirst().quantity());
        assertEquals(4, allocations.forRequest(id).size());
        assertEquals(10, quantityAt(CENTER_B, RICE));
        assertEquals(1, allocations.eventsForRequest(id).stream()
                .filter(e -> e.eventType() == AllocationEventType.REALLOCATED).count());

        CancellationResult cancelled = requestService.cancel(volunteer, id);
        assertEquals(4, cancelled.releasedAllocations());
        assertEquals(160, cancelled.releasedQuantity());
        assertEquals(RequestStatus.CANCELLED, requestService.load(id).status());
        assertEquals(100, quantityAt(CENTER_A, WATER));
        assertEquals(50, quantityAt(CENTER_B, WATER));
        assertEquals(30, quantityAt(CENTER_A, RICE));
        assertEquals(20, quantityAt(CENTER_B, RICE));
        assertTrue(requests.items(id).stream().allMatch(item -> item.quantityAllocated() == 0));
        assertTrue(allocations.forRequest(id).stream().noneMatch(a -> a.active()));
        assertFalse(allocations.markReleased(allocations.forRequest(id).getFirst().id(),
                volunteer.id(), RequestService.now()));
        assertEquals(4, allocations.eventsForRequest(id).stream()
                .filter(e -> e.eventType() == AllocationEventType.RELEASED).count());
        assertEquals(5, requests.history(id).size());

        assertThrows(IllegalStateException.class, () -> requestService.cancel(volunteer, id));
        assertEquals(100, quantityAt(CENTER_A, WATER));
    }

    @Test
    void reallocationCanRemainPartialAndRejectsFullyAllocatedRequests() {
        long id = createHighPriorityDraft();
        requestService.submit(volunteer, id);
        requestService.decideVerification(areaCoordinator, id, true, "");
        requestService.decideVerification(reliefCoordinator, id, true, "");
        allocationService.allocate(reliefCoordinator, id, "Fewest Centers");

        inventory.adjust(CENTER_B, RICE, 5);
        AllocationResult partial = allocationService.reallocate(reliefCoordinator, id, "Fewest Centers");
        assertFalse(partial.fullyCovered());
        assertEquals(5, partial.shortages().getFirst().missing());

        inventory.adjust(CENTER_B, RICE, 5);
        assertTrue(allocationService.reallocate(reliefCoordinator, id, "Fewest Centers").fullyCovered());
        assertThrows(IllegalStateException.class,
                () -> allocationService.reallocate(reliefCoordinator, id, "Fewest Centers"));
    }

    @Test
    void cancellationRollbackLeavesReservationAndInventoryUnchanged() throws Exception {
        long id = createHighPriorityDraft();
        requestService.submit(volunteer, id);
        requestService.decideVerification(areaCoordinator, id, true, "");
        requestService.decideVerification(reliefCoordinator, id, true, "");
        allocationService.allocate(reliefCoordinator, id, "Fewest Centers");

        try (var statement = Database.getInstance().connection().createStatement()) {
            statement.execute("CREATE TRIGGER fail_release_event BEFORE INSERT ON allocation_events "
                    + "WHEN NEW.event_type='RELEASED' BEGIN SELECT RAISE(ABORT, 'forced failure'); END");
        }
        assertThrows(IllegalStateException.class, () -> requestService.cancel(volunteer, id));
        assertEquals(RequestStatus.ALLOCATED, requestService.load(id).status());
        assertEquals(0, quantityAt(CENTER_A, WATER));
        assertEquals(30, quantityAt(CENTER_B, WATER));
        assertEquals(0, quantityAt(CENTER_A, RICE));
        assertEquals(3, allocations.activeForRequest(id).size());
        assertEquals(0, allocations.eventsForRequest(id).stream()
                .filter(e -> e.eventType() == AllocationEventType.RELEASED).count());
    }

    @Test
    void cancellationRequiresCreatorOrAdministratorAndIsBlockedAfterDispatch() {
        long id = createHighPriorityDraft();
        requestService.submit(volunteer, id);
        requestService.decideVerification(areaCoordinator, id, true, "");
        requestService.decideVerification(reliefCoordinator, id, true, "");
        allocationService.allocate(reliefCoordinator, id, "Fewest Centers");

        assertThrows(IllegalStateException.class, () -> requestService.cancel(areaCoordinator, id));
        dispatchService.dispatch(transport, id, 1, "Test Driver");
        assertThrows(IllegalStateException.class, () -> requestService.cancel(volunteer, id));

        long adminCancelled = createHighPriorityDraft();
        requestService.submit(volunteer, adminCancelled);
        requestService.decideVerification(areaCoordinator, adminCancelled, true, "");
        requestService.decideVerification(reliefCoordinator, adminCancelled, true, "");
        allocationService.allocate(reliefCoordinator, adminCancelled, "Fewest Centers");
        assertTrue(requestService.canCancel(admin, adminCancelled));
        assertTrue(requestService.cancel(admin, adminCancelled).releasedAllocations() > 0);
        assertEquals(RequestStatus.CANCELLED, requestService.load(adminCancelled).status());
    }

    private int quantityAt(long centerId, long resourceId) {
        return inventory.stockForCenter(centerId).stream()
                .filter(s -> s.resourceId() == resourceId)
                .mapToInt(StockView::quantity)
                .findFirst()
                .orElse(-1);
    }
}
