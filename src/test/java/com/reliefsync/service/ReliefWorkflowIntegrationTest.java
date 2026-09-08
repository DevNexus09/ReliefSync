package com.reliefsync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.db.Database;
import com.reliefsync.model.DraftItem;
import com.reliefsync.model.Priority;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.Role;
import com.reliefsync.model.StockView;
import com.reliefsync.model.User;
import com.reliefsync.repository.AreaRepository;
import com.reliefsync.repository.CenterRepository;
import com.reliefsync.repository.InventoryRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.ResourceRepository;
import com.reliefsync.repository.UserRepository;
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
    private final RequestRepository requests = new RequestRepository();
    private final InventoryRepository inventory = new InventoryRepository();

    private User volunteer;
    private User areaCoordinator;
    private User reliefCoordinator;
    private User transport;

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
        volunteer = users.findByUsername("vol").orElseThrow();
        areaCoordinator = users.findByUsername("ac").orElseThrow();
        reliefCoordinator = users.findByUsername("rc").orElseThrow();
        transport = users.findByUsername("tr").orElseThrow();

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

        allocationService.dispatch(transport, id);
        assertEquals(RequestStatus.DISPATCHED, requestService.load(id).status());
        allocationService.deliver(transport, id);
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
        // Cannot cancel once stock is reserved
        requestService.submit(volunteer, id);
        requestService.decideVerification(areaCoordinator, id, true, "");
        requestService.decideVerification(reliefCoordinator, id, true, "");
        allocationService.allocate(reliefCoordinator, id, "Balanced Across Centers");
        assertThrows(IllegalStateException.class, () -> requestService.cancel(volunteer, id));
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
                () -> allocationService.dispatch(reliefCoordinator, id));
        allocationService.dispatch(transport, id);
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

    private int quantityAt(long centerId, long resourceId) {
        return inventory.stockForCenter(centerId).stream()
                .filter(s -> s.resourceId() == resourceId)
                .mapToInt(StockView::quantity)
                .findFirst()
                .orElse(-1);
    }
}
