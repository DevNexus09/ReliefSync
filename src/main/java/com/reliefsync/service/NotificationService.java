package com.reliefsync.service;

import com.reliefsync.model.Notification;
import com.reliefsync.model.NotificationEventType;
import com.reliefsync.model.AllocationEventType;
import com.reliefsync.model.DeliveryRecoveryAction;
import com.reliefsync.model.DispatchManifest;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.Role;
import com.reliefsync.model.Shortage;
import com.reliefsync.model.StockView;
import com.reliefsync.model.User;
import com.reliefsync.notification.NotificationBootstrap;
import com.reliefsync.notification.ReliefEvent;
import com.reliefsync.repository.InventoryRepository;
import com.reliefsync.repository.NotificationRepository;
import com.reliefsync.repository.RequestRepository;
import java.time.LocalDateTime;
import java.util.List;

/** Notification reads, ownership-safe read state, and low-stock transition detection. */
public class NotificationService {
    private final NotificationRepository notifications = new NotificationRepository();
    private final InventoryRepository inventory = new InventoryRepository();
    private final RequestRepository requests = new RequestRepository();

    public List<Notification> notifications(User actor) {
        requireActor(actor);
        return notifications.forUser(actor.id());
    }

    public List<Notification> unread(User actor) {
        requireActor(actor);
        return notifications.unreadForUser(actor.id());
    }

    public int unreadCount(User actor) {
        requireActor(actor);
        return notifications.unreadCount(actor.id());
    }

    public void markRead(User actor, long notificationId) {
        requireActor(actor);
        notifications.markRead(actor.id(), notificationId, now());
    }

    public int markAllRead(User actor) {
        requireActor(actor);
        return notifications.markAllRead(actor.id(), now());
    }

    public List<Notification> forRequest(User actor, long requestId) {
        requireActor(actor);
        return notifications.forRequest(actor.id(), requestId);
    }

    public List<Notification> byEventType(User actor, NotificationEventType type) {
        requireActor(actor);
        return notifications.byEventType(actor.id(), type);
    }

    public void publish(ReliefEvent event) {
        NotificationBootstrap.publish(event);
    }

    public void requestAwaitingVerification(User actor, ReliefRequest request, long historyId,
                                            Role nextRole, String timestamp) {
        publish(new ReliefEvent(NotificationEventType.REQUEST_AWAITING_VERIFICATION,
                "REQUEST_AWAITING_VERIFICATION:" + request.id() + ":" + historyId,
                actor.id(), actor.fullName(), request.id(), nextRole, "Request awaiting verification",
                "Request #" + request.id() + " for " + requests.areaName(request.id()) + " ("
                        + request.priority() + ") is awaiting " + nextRole.label() + " verification.", timestamp));
    }

    public void verificationCompleted(User actor, ReliefRequest request, long verificationId,
                                      Role completedRole, boolean approved, boolean chainComplete,
                                      Role nextRole, String timestamp) {
        String result = approved ? (chainComplete ? "Final approval completed; request is VERIFIED."
                : "Approved; next round is " + nextRole.label() + ".") : "Rejected; request is REJECTED.";
        publish(new ReliefEvent(NotificationEventType.VERIFICATION_ROUND_COMPLETED,
                "VERIFICATION_ROUND_COMPLETED:" + verificationId, actor.id(), actor.fullName(), request.id(),
                nextRole, "Verification round completed",
                "Request #" + request.id() + " for " + requests.areaName(request.id()) + " ("
                        + request.priority() + "): " + completedRole.label() + " round completed. " + result,
                timestamp));
    }

    public void requestAllocated(User actor, ReliefRequest request, AllocationEventType eventType,
                                 long allocationEventId, String strategy, int total,
                                 List<Shortage> shortages, String timestamp) {
        String shortageText = shortages.isEmpty() ? "No shortages." : "Shortages: " + shortages.stream()
                .map(s -> s.resourceName() + " " + s.missing())
                .collect(java.util.stream.Collectors.joining(", ")) + ".";
        publish(new ReliefEvent(NotificationEventType.REQUEST_ALLOCATED,
                "REQUEST_ALLOCATED:" + allocationEventId, actor.id(), actor.fullName(), request.id(), null,
                eventType == AllocationEventType.REALLOCATED ? "Request reallocated" : "Request allocated",
                "Request #" + request.id() + " for " + requests.areaName(request.id()) + " received "
                        + (shortages.isEmpty() ? "complete" : "partial") + " allocation of " + total
                        + " units using " + strategy + ". " + shortageText, timestamp));
    }

    public void deliveryFailed(User actor, ReliefRequest request, DispatchManifest manifest,
                               String reason, DeliveryRecoveryAction action, String timestamp) {
        publish(new ReliefEvent(NotificationEventType.DELIVERY_FAILURE,
                "DELIVERY_FAILURE:" + manifest.id(), actor.id(), actor.fullName(), request.id(), null,
                "Delivery failed", "Request #" + request.id() + " attempt " + manifest.attemptNumber()
                        + " with vehicle " + manifest.vehicleRegistration() + ", driver "
                        + manifest.driverName() + ", failed: " + reason
                        + ". Reported by " + actor.fullName() + " at " + timestamp + ". Recovery: "
                        + action.label() + ".", timestamp));
    }

    public void requestDelivered(User actor, ReliefRequest request, DispatchManifest manifest, String timestamp) {
        publish(new ReliefEvent(NotificationEventType.REQUEST_DELIVERED,
                "REQUEST_DELIVERED:" + request.id() + ":" + manifest.id(), actor.id(), actor.fullName(),
                request.id(), null, "Request delivered", "Request #" + request.id() + " for "
                        + requests.areaName(request.id()) + " was delivered by " + manifest.vehicleRegistration()
                        + ", driver " + manifest.driverName() + ", at " + timestamp + " (attempt "
                        + manifest.attemptNumber() + ").", timestamp));
    }

    /** Must be called after a stock mutation and within its surrounding transaction. */
    public void inventoryChanged(User actor, long centerId, long resourceId,
                                 Integer previousQuantity, String timestamp) {
        StockView stock = inventory.find(centerId, resourceId)
                .orElseThrow(() -> new IllegalStateException("Inventory row was not saved"));
        if (stock.quantity() <= stock.lowStockThreshold()
                && (previousQuantity == null || previousQuantity > stock.lowStockThreshold())) {
            Integer transition = notifications.activateLowStock(centerId, resourceId, timestamp);
            if (transition != null) {
                publish(new ReliefEvent(NotificationEventType.LOW_STOCK_WARNING,
                        "LOW_STOCK_WARNING:" + centerId + ":" + resourceId + ":" + transition,
                        actor.id(), actor.fullName(), null, null, "Low stock warning",
                        stock.centerName() + " has " + stock.quantity() + " " + stock.resourceName()
                                + " remaining (threshold " + stock.lowStockThreshold() + ").",
                        timestamp));
            }
        } else if (stock.quantity() > stock.lowStockThreshold()) {
            notifications.clearLowStock(centerId, resourceId, timestamp);
        } else {
            notifications.rememberLowStockActive(centerId, resourceId, timestamp);
        }
    }

    private static void requireActor(User actor) {
        if (actor == null) throw new IllegalStateException("Login is required");
    }

    private static String now() {
        return LocalDateTime.now().withNano(0).toString();
    }
}
