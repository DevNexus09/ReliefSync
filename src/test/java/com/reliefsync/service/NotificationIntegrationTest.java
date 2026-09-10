package com.reliefsync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.db.Database;
import com.reliefsync.db.Seeder;
import com.reliefsync.facade.ReliefOperationFacade;
import com.reliefsync.model.AffectedArea;
import com.reliefsync.model.DeliveryRecoveryAction;
import com.reliefsync.model.DraftItem;
import com.reliefsync.model.Notification;
import com.reliefsync.model.NotificationEventType;
import com.reliefsync.model.ManifestStatus;
import com.reliefsync.model.Priority;
import com.reliefsync.model.ReliefCenter;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.Resource;
import com.reliefsync.model.User;
import com.reliefsync.model.Vehicle;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.notification.NotificationBootstrap;
import com.reliefsync.repository.NotificationRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.UserRepository;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NotificationIntegrationTest {
    @TempDir Path tempDir;
    private String url;
    private final UserRepository users = new UserRepository();
    private final MasterDataService master = new MasterDataService();
    private final ReliefOperationFacade facade = new ReliefOperationFacade();
    private final NotificationService notificationService = new NotificationService();

    private User admin;
    private User areaCoordinator;
    private User centerManager;
    private User transport;
    private User volunteer;
    private User reliefCoordinator;
    private AffectedArea area;
    private Resource resource;

    @BeforeEach void setUp() {
        url = "jdbc:sqlite:" + tempDir.resolve("notifications.db");
        Database.init(url);
        Seeder.seedDemo();
        NotificationBootstrap.initialize();
        admin = user("admin");
        areaCoordinator = user("area_coordinator");
        centerManager = user("center_manager");
        transport = user("transport");
        volunteer = user("volunteer");
        reliefCoordinator = user("relief_coordinator");
        area = master.activeAreas().get(0);
        resource = master.activeResources().get(0);
    }

    @AfterEach void tearDown() {
        NotificationBootstrap.reset();
        Database.reset();
    }

    @Test void submissionNotifiesOnlyNextVerifierAndAdminAndDraftDoesNotNotify() {
        long requestId = draft(Priority.NORMAL, 2);
        assertEquals(0, notificationService.notifications(areaCoordinator).size());
        facade.submit(volunteer, requestId);
        assertEquals(1, notificationService.byEventType(areaCoordinator,
                NotificationEventType.REQUEST_AWAITING_VERIFICATION).size());
        assertEquals(1, notificationService.byEventType(admin,
                NotificationEventType.REQUEST_AWAITING_VERIFICATION).size());
        assertEquals(0, notificationService.notifications(centerManager).size());
        assertEquals(0, notificationService.notifications(volunteer).size());
    }

    @Test void verificationAllocationFailureRetryAndDeliveryNotifyCorrectRecipients() {
        long requestId = draft(Priority.NORMAL, 2);
        facade.submit(volunteer, requestId);
        facade.verify(areaCoordinator, requestId, true, "confirmed");
        assertEquals(1, notificationService.byEventType(reliefCoordinator,
                NotificationEventType.VERIFICATION_ROUND_COMPLETED).size());
        assertEquals(1, notificationService.byEventType(volunteer,
                NotificationEventType.VERIFICATION_ROUND_COMPLETED).size());

        facade.allocate(reliefCoordinator, requestId, "Fewest Centers");
        assertEquals(1, notificationService.byEventType(transport,
                NotificationEventType.REQUEST_ALLOCATED).size());
        assertEquals(1, notificationService.byEventType(volunteer,
                NotificationEventType.REQUEST_ALLOCATED).size());

        Vehicle vehicle = facade.availableVehicles(transport).stream()
                .filter(v -> v.capacity() >= 2).findFirst().orElseThrow();
        facade.dispatch(transport, requestId, vehicle.id(), "Test Driver");
        facade.reportDeliveryFailure(transport, requestId, "Bridge closed",
                DeliveryRecoveryAction.RETRY, "Use alternate road");
        Notification failure = notificationService.byEventType(reliefCoordinator,
                NotificationEventType.DELIVERY_FAILURE).get(0);
        assertTrue(failure.message().contains("Bridge closed"));
        assertTrue(failure.message().contains("attempt 1"));
        assertTrue(failure.message().contains("Retry delivery"));
        assertEquals(0, notificationService.byEventType(transport,
                NotificationEventType.DELIVERY_FAILURE).size());

        facade.retryDelivery(transport, requestId, vehicle.id(), "Recovery Driver");
        facade.deliver(transport, requestId);
        Notification delivered = notificationService.byEventType(volunteer,
                NotificationEventType.REQUEST_DELIVERED).get(0);
        assertTrue(delivered.message().contains("attempt 2"));
        assertTrue(delivered.message().contains(vehicle.registrationNumber()));
    }

    @Test void lowStockWarningDoesNotSpamAndRearmsAfterRestock() {
        ReliefCenter center = master.activeCenters().get(0);
        InventoryService inventory = new InventoryService();
        int threshold = resource.lowStockThreshold();
        inventory.setQuantity(centerManager, center.id(), resource.id(), threshold + 2);
        inventory.adjust(centerManager, center.id(), resource.id(), -2);
        assertEquals(1, notificationService.byEventType(admin, NotificationEventType.LOW_STOCK_WARNING).size());
        inventory.adjust(centerManager, center.id(), resource.id(), -1);
        assertEquals(1, notificationService.byEventType(admin, NotificationEventType.LOW_STOCK_WARNING).size());
        notificationService.notifications(admin); // read-only queries must not publish
        assertEquals(1, notificationService.byEventType(admin, NotificationEventType.LOW_STOCK_WARNING).size());
        inventory.setQuantity(centerManager, center.id(), resource.id(), threshold + 3);
        inventory.adjust(centerManager, center.id(), resource.id(), -3);
        assertEquals(2, notificationService.byEventType(admin, NotificationEventType.LOW_STOCK_WARNING).size());
    }

    @Test void partialAllocationAndLaterReallocationCreateSeparateDetailedEvents() {
        long requestId = draft(Priority.NORMAL, 1_000_000);
        facade.submit(volunteer, requestId);
        facade.verify(areaCoordinator, requestId, true, "confirmed");
        facade.allocate(reliefCoordinator, requestId, "Fewest Centers");
        Notification initial = notificationService.byEventType(transport,
                NotificationEventType.REQUEST_ALLOCATED).get(0);
        assertTrue(initial.message().contains("partial allocation"));
        assertTrue(initial.message().contains("Shortages:"));

        ReliefCenter center = master.activeCenters().get(0);
        new InventoryService().setQuantity(centerManager, center.id(), resource.id(), 4);
        facade.reallocate(reliefCoordinator, requestId, "Fewest Centers");
        List<Notification> events = notificationService.byEventType(transport,
                NotificationEventType.REQUEST_ALLOCATED);
        assertEquals(2, events.size());
        assertTrue(events.get(0).title().contains("reallocated"));
        assertFalse(events.get(0).eventKey().equals(events.get(1).eventKey()));
    }

    @Test void unreadOwnershipIdempotencyAndPersistenceAreEnforced() {
        long requestId = draft(Priority.NORMAL, 1);
        facade.submit(volunteer, requestId);
        Notification notification = notificationService.notifications(areaCoordinator).get(0);
        assertEquals(1, notificationService.unreadCount(areaCoordinator));
        facade.markNotificationRead(centerManager, notification.id());
        assertEquals(1, notificationService.unreadCount(areaCoordinator));
        facade.markNotificationRead(areaCoordinator, notification.id());
        assertEquals(0, notificationService.unreadCount(areaCoordinator));

        long secondRequest = draft(Priority.NORMAL, 1);
        facade.submit(volunteer, secondRequest);
        assertEquals(1, notificationService.unreadCount(areaCoordinator));
        assertEquals(1, facade.markAllNotificationsRead(areaCoordinator));
        assertEquals(0, notificationService.unreadCount(areaCoordinator));

        NotificationRepository repository = new NotificationRepository();
        assertFalse(repository.insert(areaCoordinator.id(), notification.eventType(), notification.eventKey(),
                notification.title(), notification.message(), requestId, notification.createdAt()));
        NotificationBootstrap.reset();
        Database.reset();
        Database.init(url);
        NotificationBootstrap.initialize();
        assertEquals(2, notificationService.notifications(areaCoordinator).size());
        assertEquals(0, notificationService.unreadCount(areaCoordinator));
    }

    @Test void notificationInsertFailureRollsBackSubmission() throws Exception {
        long requestId = draft(Priority.NORMAL, 1);
        try (Statement statement = Database.getInstance().connection().createStatement()) {
            statement.execute("CREATE TRIGGER force_notification_failure BEFORE INSERT ON notifications "
                    + "BEGIN SELECT RAISE(ABORT, 'forced notification failure'); END");
        }
        assertThrows(IllegalStateException.class, () -> facade.submit(volunteer, requestId));
        assertEquals(RequestStatus.DRAFT, new RequestRepository().findById(requestId).orElseThrow().status());
        assertEquals(0, notificationService.notifications(areaCoordinator).size());
        assertEquals(1, new RequestRepository().history(requestId).size());
    }

    @Test void notificationFailureRollsBackVerificationAndAllocation() throws Exception {
        long verificationRequest = draft(Priority.NORMAL, 1);
        facade.submit(volunteer, verificationRequest);
        installFailureTrigger();
        assertThrows(IllegalStateException.class,
                () -> facade.verify(areaCoordinator, verificationRequest, true, "confirmed"));
        assertEquals(RequestStatus.SUBMITTED,
                new RequestRepository().findById(verificationRequest).orElseThrow().status());
        assertEquals(0, facade.verifications(verificationRequest).size());

        removeFailureTrigger();
        facade.verify(areaCoordinator, verificationRequest, true, "confirmed");
        installFailureTrigger();
        assertThrows(IllegalStateException.class,
                () -> facade.allocate(reliefCoordinator, verificationRequest, "Fewest Centers"));
        assertEquals(RequestStatus.VERIFIED,
                new RequestRepository().findById(verificationRequest).orElseThrow().status());
        assertEquals(0, facade.allocations(verificationRequest).size());
    }

    @Test void notificationFailureRollsBackDeliveryFailureAndDelivery() throws Exception {
        long requestId = verifiedAndAllocatedRequest();
        Vehicle vehicle = facade.availableVehicles(transport).stream()
                .filter(v -> v.capacity() >= 2).findFirst().orElseThrow();
        facade.dispatch(transport, requestId, vehicle.id(), "Driver");
        installFailureTrigger();
        assertThrows(IllegalStateException.class, () -> facade.reportDeliveryFailure(transport, requestId,
                "Flooded road", DeliveryRecoveryAction.RETRY, null));
        assertEquals(RequestStatus.DISPATCHED, new RequestRepository().findById(requestId).orElseThrow().status());
        assertEquals(ManifestStatus.DISPATCHED, facade.manifest(requestId).orElseThrow().status());
        assertEquals(VehicleStatus.IN_TRANSIT,
                new com.reliefsync.repository.VehicleRepository().findById(vehicle.id()).orElseThrow().status());
        assertEquals(0, facade.deliveryFailures(requestId).size());

        assertThrows(IllegalStateException.class, () -> facade.deliver(transport, requestId));
        assertEquals(RequestStatus.DISPATCHED, new RequestRepository().findById(requestId).orElseThrow().status());
        assertEquals(ManifestStatus.DISPATCHED, facade.manifest(requestId).orElseThrow().status());
    }

    private long draft(Priority priority, int quantity) {
        return facade.createDraft(volunteer, area.id(), priority, "notification test",
                List.of(new DraftItem(resource.id(), resource.name(), quantity)));
    }

    private long verifiedAndAllocatedRequest() {
        long requestId = draft(Priority.NORMAL, 2);
        facade.submit(volunteer, requestId);
        facade.verify(areaCoordinator, requestId, true, "confirmed");
        facade.allocate(reliefCoordinator, requestId, "Fewest Centers");
        return requestId;
    }

    private void installFailureTrigger() throws Exception {
        try (Statement statement = Database.getInstance().connection().createStatement()) {
            statement.execute("CREATE TRIGGER force_notification_failure BEFORE INSERT ON notifications "
                    + "BEGIN SELECT RAISE(ABORT, 'forced notification failure'); END");
        }
    }

    private void removeFailureTrigger() throws Exception {
        try (Statement statement = Database.getInstance().connection().createStatement()) {
            statement.execute("DROP TRIGGER force_notification_failure");
        }
    }

    private User user(String username) {
        return users.findByUsername(username).orElseThrow();
    }
}
