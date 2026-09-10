package com.reliefsync.facade;

import com.reliefsync.model.Allocation;
import com.reliefsync.model.AllocationEvent;
import com.reliefsync.model.DraftItem;
import com.reliefsync.model.Notification;
import com.reliefsync.model.DeliveryFailure;
import com.reliefsync.model.DeliveryRecoveryAction;
import com.reliefsync.model.DispatchManifest;
import com.reliefsync.model.DispatchManifestItem;
import com.reliefsync.model.Priority;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.RequestRow;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.StatusChange;
import com.reliefsync.model.User;
import com.reliefsync.model.Verification;
import com.reliefsync.model.Vehicle;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.repository.AllocationRepository;
import com.reliefsync.repository.DeliveryFailureRepository;
import com.reliefsync.repository.DispatchManifestRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.VerificationRepository;
import com.reliefsync.service.AllocationResult;
import com.reliefsync.service.AllocationService;
import com.reliefsync.service.CancellationResult;
import com.reliefsync.service.DispatchService;
import com.reliefsync.service.NotificationService;
import com.reliefsync.service.RequestService;
import com.reliefsync.service.ReallocationRecoveryResult;
import com.reliefsync.service.VehicleService;
import com.reliefsync.strategy.AllocationStrategies;
import com.reliefsync.verification.VerificationOutcome;
import java.util.List;
import java.util.Optional;

/**
 * Facade over the cross-service relief workflow. The UI talks to this single
 * API for every step from draft to delivery, so controllers never need to know
 * which service, repository, or pattern implements a step.
 */
public class ReliefOperationFacade {

    private final RequestService requestService = new RequestService();
    private final AllocationService allocationService = new AllocationService();
    private final DispatchService dispatchService = new DispatchService();
    private final VehicleService vehicleService = new VehicleService();
    private final NotificationService notificationService = new NotificationService();
    private final RequestRepository requests = new RequestRepository();
    private final VerificationRepository verifications = new VerificationRepository();
    private final AllocationRepository allocations = new AllocationRepository();
    private final DeliveryFailureRepository deliveryFailures = new DeliveryFailureRepository();
    private final DispatchManifestRepository manifests = new DispatchManifestRepository();

    // ---- Request workflow ----

    public boolean hasOpenRequestForArea(long areaId) {
        return requestService.hasOpenRequestForArea(areaId);
    }

    public long createDraft(User actor, long areaId, Priority priority, String note, List<DraftItem> items) {
        return requestService.createDraft(actor, areaId, priority, note, items);
    }

    public void submit(User actor, long requestId) {
        requestService.submit(actor, requestId);
    }

    public VerificationOutcome verify(User actor, long requestId, boolean approve, String comment) {
        return requestService.decideVerification(actor, requestId, approve, comment);
    }

    public boolean canCancel(User actor, long requestId) {
        return requestService.canCancel(actor, requestId);
    }

    public CancellationResult cancel(User actor, long requestId) {
        return requestService.cancel(actor, requestId);
    }

    // ---- Allocation workflow ----

    public List<String> strategyNames() {
        return AllocationStrategies.names();
    }

    public AllocationResult previewAllocation(long requestId, String strategyName) {
        return allocationService.preview(requestId, strategyName);
    }

    public AllocationResult allocate(User actor, long requestId, String strategyName) {
        return allocationService.allocate(actor, requestId, strategyName);
    }

    public AllocationResult reallocate(User actor, long requestId, String strategyName) {
        return allocationService.reallocate(actor, requestId, strategyName);
    }

    public void dispatch(User actor, long requestId, long vehicleId, String driverName) {
        dispatchService.dispatch(actor, requestId, vehicleId, driverName);
    }

    public void deliver(User actor, long requestId) {
        dispatchService.deliver(actor, requestId);
    }

    public void reportDeliveryFailure(User actor, long requestId, String reason,
                                      DeliveryRecoveryAction action, String recoveryNotes) {
        dispatchService.reportDeliveryFailure(actor, requestId, reason, action, recoveryNotes);
    }

    public void retryDelivery(User actor, long requestId, long vehicleId, String driverName) {
        dispatchService.retryDelivery(actor, requestId, vehicleId, driverName);
    }

    public ReallocationRecoveryResult returnForReallocation(User actor, long requestId, String recoveryNotes) {
        return dispatchService.returnForReallocation(actor, requestId, recoveryNotes);
    }

    // ---- Vehicles and dispatch manifests ----

    public List<Vehicle> vehicles(User actor, String search) {
        return vehicleService.search(actor, search);
    }

    public List<Vehicle> availableVehicles(User actor) {
        return vehicleService.available(actor);
    }

    public long saveVehicle(User actor, Long idOrNull, String registrationNumber,
                            String vehicleType, int capacity) {
        return vehicleService.save(actor, idOrNull, registrationNumber, vehicleType, capacity);
    }

    public void setVehicleStatus(User actor, long vehicleId, VehicleStatus status) {
        vehicleService.setStatus(actor, vehicleId, status);
    }

    public Optional<DispatchManifest> manifest(long requestId) {
        return manifests.findByRequest(requestId);
    }

    public List<DispatchManifest> dispatchAttempts(long requestId) {
        return manifests.allForRequest(requestId);
    }

    public List<DispatchManifestItem> manifestItems(long manifestId) {
        return manifests.items(manifestId);
    }

    public Optional<DeliveryFailure> latestDeliveryFailure(long requestId) {
        return deliveryFailures.latestForRequest(requestId);
    }

    public List<DeliveryFailure> deliveryFailures(long requestId) {
        return deliveryFailures.forRequest(requestId);
    }

    // ---- Read models for the UI ----

    public List<RequestRow> requests(String search, RequestStatus statusOrNull) {
        return requests.rows(search == null ? "" : search, statusOrNull);
    }

    public List<RequestRow> requestsByStatuses(List<RequestStatus> statuses) {
        return requests.rowsByStatuses(statuses);
    }

    public List<RequestItem> items(long requestId) {
        return requests.items(requestId);
    }

    public List<Verification> verifications(long requestId) {
        return verifications.forRequest(requestId);
    }

    public List<StatusChange> history(long requestId) {
        return requests.history(requestId);
    }

    public List<Allocation> allocations(long requestId) {
        return allocations.forRequest(requestId);
    }

    public List<AllocationEvent> allocationEvents(long requestId) {
        return allocations.eventsForRequest(requestId);
    }

    // ---- Current user's in-application notifications ----

    public List<Notification> notifications(User actor) {
        return notificationService.notifications(actor);
    }

    public int unreadNotificationCount(User actor) {
        return notificationService.unreadCount(actor);
    }

    public void markNotificationRead(User actor, long notificationId) {
        notificationService.markRead(actor, notificationId);
    }

    public int markAllNotificationsRead(User actor) {
        return notificationService.markAllRead(actor);
    }

    public List<Notification> notificationsForRequest(User actor, long requestId) {
        return notificationService.forRequest(actor, requestId);
    }
}
