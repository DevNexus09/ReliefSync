package com.reliefsync.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.model.AllocationEventType;
import com.reliefsync.model.ManifestStatus;
import com.reliefsync.model.NotificationEventType;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.notification.NotificationBootstrap;
import com.reliefsync.repository.AllocationRepository;
import com.reliefsync.repository.DispatchManifestRepository;
import com.reliefsync.repository.InventoryRepository;
import com.reliefsync.repository.NotificationRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.VehicleRepository;
import com.reliefsync.repository.VerificationRepository;
import com.reliefsync.service.AllocationService;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeederTest {
    @TempDir Path tempDir;

    @BeforeEach void setUp() {
        Database.init("jdbc:sqlite:" + tempDir.resolve("seed.db"));
        NotificationBootstrap.initialize();
    }

    @AfterEach void tearDown() {
        Database.reset();
    }

    @Test void realisticDemoSeedIsIdempotentAndHasExpectedScale() throws Exception {
        Seeder.seedDemo();
        int users = scalar("SELECT COUNT(*) FROM users");
        int requests = scalar("SELECT COUNT(*) FROM relief_requests");
        int allocations = scalar("SELECT COUNT(*) FROM allocations");
        int events = scalar("SELECT COUNT(*) FROM allocation_events");
        int manifests = scalar("SELECT COUNT(*) FROM dispatch_manifests");
        int notifications = scalar("SELECT COUNT(*) FROM notifications");
        Seeder.seedDemo();

        assertEquals(6, users);
        assertEquals(users, scalar("SELECT COUNT(*) FROM users"));
        assertEquals(9, scalar("SELECT COUNT(*) FROM affected_areas"));
        assertEquals(4, scalar("SELECT COUNT(*) FROM relief_centers"));
        assertEquals(8, scalar("SELECT COUNT(*) FROM resources"));
        assertEquals(32, scalar("SELECT COUNT(*) FROM inventory"));
        assertEquals(8, scalar("SELECT COUNT(*) FROM vehicles"));
        assertEquals(16, requests);
        assertEquals(requests, scalar("SELECT COUNT(*) FROM relief_requests"));
        assertEquals(35, scalar("SELECT COUNT(*) FROM request_items"));
        assertEquals(allocations, scalar("SELECT COUNT(*) FROM allocations"));
        assertEquals(events, scalar("SELECT COUNT(*) FROM allocation_events"));
        assertEquals(5, manifests);
        assertEquals(manifests, scalar("SELECT COUNT(*) FROM dispatch_manifests"));
        assertEquals(notifications, scalar("SELECT COUNT(*) FROM notifications"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM pragma_foreign_key_check"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM inventory WHERE quantity < 0"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM request_items"
                + " WHERE quantity_allocated < 0 OR quantity_allocated > quantity_requested"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM (SELECT recipient_id,event_key FROM notifications"
                + " GROUP BY recipient_id,event_key HAVING COUNT(*) > 1)"));
    }

    @Test void seededScenariosCoverEveryLifecycleAndPreserveHistories() {
        Seeder.seedDemo();
        RequestRepository requests = new RequestRepository();
        assertStatus(requests, "[DEMO-R01]", RequestStatus.DRAFT);
        assertStatus(requests, "[DEMO-R02]", RequestStatus.SUBMITTED);
        assertStatus(requests, "[DEMO-R03]", RequestStatus.SUBMITTED);
        assertStatus(requests, "[DEMO-R04]", RequestStatus.SUBMITTED);
        assertStatus(requests, "[DEMO-R05]", RequestStatus.VERIFIED);
        assertStatus(requests, "[DEMO-R06]", RequestStatus.VERIFIED);
        assertStatus(requests, "[DEMO-R07]", RequestStatus.ALLOCATED);
        assertStatus(requests, "[DEMO-R08]", RequestStatus.DISPATCHED);
        assertStatus(requests, "[DEMO-R09]", RequestStatus.DELIVERED);
        assertStatus(requests, "[DEMO-R10]", RequestStatus.REJECTED);
        assertStatus(requests, "[DEMO-R11]", RequestStatus.CANCELLED);
        assertStatus(requests, "[DEMO-R12]", RequestStatus.DELIVERY_FAILED);
        assertStatus(requests, "[DEMO-R13]", RequestStatus.DISPATCHED);
        assertStatus(requests, "[DEMO-R14]", RequestStatus.ALLOCATED);
        assertStatus(requests, "[DEMO-R15]", RequestStatus.ALLOCATED);
        assertStatus(requests, "[DEMO-R16]", RequestStatus.SUBMITTED);

        assertEquals(1, countStatus(RequestStatus.DRAFT));
        assertEquals(4, countStatus(RequestStatus.SUBMITTED));
        assertEquals(2, countStatus(RequestStatus.VERIFIED));
        assertEquals(3, countStatus(RequestStatus.ALLOCATED));
        assertEquals(2, countStatus(RequestStatus.DISPATCHED));
        assertEquals(1, countStatus(RequestStatus.DELIVERED));
        assertEquals(1, countStatus(RequestStatus.REJECTED));
        assertEquals(1, countStatus(RequestStatus.CANCELLED));
        assertEquals(1, countStatus(RequestStatus.DELIVERY_FAILED));

        VerificationRepository verifications = new VerificationRepository();
        assertEquals(1, verifications.forRequest(id(requests, "[DEMO-R03]")).size());
        assertEquals(2, verifications.forRequest(id(requests, "[DEMO-R04]")).size());
        assertEquals(3, verifications.forRequest(id(requests, "[DEMO-R09]")).size());

        DispatchManifestRepository manifests = new DispatchManifestRepository();
        long delivered = id(requests, "[DEMO-R09]");
        assertEquals(ManifestStatus.DELIVERED, manifests.findByRequest(delivered).orElseThrow().status());
        assertTrue(manifests.findByRequest(delivered).orElseThrow().totalLoad() > 0);
        long retry = id(requests, "[DEMO-R13]");
        assertEquals(2, manifests.allForRequest(retry).size());
        assertEquals(ManifestStatus.DELIVERY_FAILED, manifests.allForRequest(retry).get(0).status());
        assertEquals(ManifestStatus.DISPATCHED, manifests.allForRequest(retry).get(1).status());

        AllocationRepository allocationRepository = new AllocationRepository();
        long cancelled = id(requests, "[DEMO-R11]");
        assertTrue(allocationRepository.forRequest(cancelled).stream().noneMatch(a -> a.active()));
        assertTrue(allocationRepository.eventsForRequest(cancelled).stream()
                .anyMatch(event -> event.eventType() == AllocationEventType.RELEASED));
        long reallocated = id(requests, "[DEMO-R15]");
        assertTrue(allocationRepository.eventsForRequest(reallocated).stream()
                .anyMatch(event -> event.eventType() == AllocationEventType.REALLOCATED));
        assertTrue(requests.items(id(requests, "[DEMO-R14]")).stream()
                .anyMatch(item -> item.outstanding() > 0));
        long duplicateArea = requests.findById(id(requests, "[DEMO-R06]")).orElseThrow().areaId();
        assertTrue(requests.openRequestCountForArea(duplicateArea) >= 2);

        VehicleRepository vehicles = new VehicleRepository();
        assertEquals(VehicleStatus.IN_TRANSIT,
                vehicles.findByRegistration("SYLHET-TA-12-3102").orElseThrow().status());
        assertEquals(VehicleStatus.IN_TRANSIT,
                vehicles.findByRegistration("DHAKA-METRO-NA-15-4506").orElseThrow().status());
        assertEquals(VehicleStatus.MAINTENANCE,
                vehicles.findByRegistration("SYLHET-NA-11-0907").orElseThrow().status());
        assertEquals(VehicleStatus.INACTIVE,
                vehicles.findByRegistration("KHULNA-NA-12-0808").orElseThrow().status());
    }

    @Test void seededInventoryStrategiesReportsAndNotificationsAreMeaningful() {
        Seeder.seedDemo();
        RequestRepository requests = new RequestRepository();
        long strategyRequest = id(requests, "[DEMO-R06]");
        AllocationService allocations = new AllocationService();
        var concentrated = allocations.preview(strategyRequest, "Fewest Centers");
        var balanced = allocations.preview(strategyRequest, "Balanced Across Centers");
        assertTrue(balanced.lines().size() > concentrated.lines().size());
        assertTrue(new InventoryRepository().lowStock().size() >= 4);

        NotificationRepository notifications = new NotificationRepository();
        long adminId = scalarLong("SELECT id FROM users WHERE username='admin'");
        for (NotificationEventType type : NotificationEventType.values()) {
            assertTrue(notifications.byEventType(adminId, type).size() > 0,
                    () -> "Missing seeded notification type " + type);
        }
        assertTrue(scalarUnchecked("SELECT COUNT(*) FROM status_history") > 16);
        assertTrue(scalarUnchecked("SELECT COUNT(*) FROM verifications") > 10);
        assertTrue(scalarUnchecked("SELECT COUNT(*) FROM allocation_events") >= 10);
        assertEquals(2, scalarUnchecked("SELECT COUNT(*) FROM delivery_failures"));
    }

    private static void assertStatus(RequestRepository requests, String key, RequestStatus status) {
        assertEquals(status, requests.findByNotePrefix(key).orElseThrow().status(), key);
    }

    private static long id(RequestRepository requests, String key) {
        return requests.findByNotePrefix(key).orElseThrow().id();
    }

    private int countStatus(RequestStatus status) {
        return scalarUnchecked("SELECT COUNT(*) FROM relief_requests WHERE status='" + status.name() + "'");
    }

    private int scalar(String sql) throws Exception {
        try (Statement statement = Database.getInstance().connection().createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private int scalarUnchecked(String sql) {
        try {
            return scalar(sql);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private long scalarLong(String sql) {
        try (Statement statement = Database.getInstance().connection().createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getLong(1) : 0;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
