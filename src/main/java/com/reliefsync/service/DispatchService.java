package com.reliefsync.service;

import com.reliefsync.db.Database;
import com.reliefsync.model.Allocation;
import com.reliefsync.model.AllocationEventType;
import com.reliefsync.model.DeliveryFailure;
import com.reliefsync.model.DeliveryRecoveryAction;
import com.reliefsync.model.DispatchManifest;
import com.reliefsync.model.ManifestStatus;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.User;
import com.reliefsync.model.Vehicle;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.repository.AllocationRepository;
import com.reliefsync.repository.DeliveryFailureRepository;
import com.reliefsync.repository.DispatchManifestRepository;
import com.reliefsync.repository.InventoryRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.VehicleRepository;
import com.reliefsync.state.RequestStates;
import java.util.List;

/** Coordinates vehicle claiming, dispatch manifests, and delivery atomically. */
public class DispatchService {

    private final RequestService requestService = new RequestService();
    private final RequestRepository requests = new RequestRepository();
    private final AllocationRepository allocations = new AllocationRepository();
    private final InventoryRepository inventory = new InventoryRepository();
    private final VehicleRepository vehicles = new VehicleRepository();
    private final DispatchManifestRepository manifests = new DispatchManifestRepository();
    private final DeliveryFailureRepository failures = new DeliveryFailureRepository();

    public void dispatch(User actor, long requestId, long vehicleId, String driverName) {
        AccessControl.require(actor, Feature.TRANSPORT);
        ReliefRequest request = requestService.load(requestId);
        RequestStatus next = RequestStates.of(request.status()).dispatch();
        String driver = requireDriver(driverName);

        dispatchAttempt(actor, request, next, vehicleId, driver, null);
    }

    public void retryDelivery(User actor, long requestId, long vehicleId, String driverName) {
        AccessControl.require(actor, Feature.TRANSPORT);
        ReliefRequest request = requestService.load(requestId);
        RequestStatus next = RequestStates.of(request.status()).retryDelivery();
        String driver = requireDriver(driverName);
        DeliveryFailure failure = currentFailure(requestId, DeliveryRecoveryAction.RETRY);

        dispatchAttempt(actor, request, next, vehicleId, driver, failure);
    }

    private void dispatchAttempt(User actor, ReliefRequest request, RequestStatus next,
                                 long vehicleId, String driver, DeliveryFailure recovery) {
        long requestId = request.id();

        Database.getInstance().inTransaction(c -> {
            DispatchManifest previous = manifests.findByRequest(requestId).orElse(null);
            if (previous != null && previous.status() != ManifestStatus.DELIVERY_FAILED) {
                throw new IllegalStateException("Request #" + requestId + " already has an active dispatch result");
            }
            if (recovery != null && (previous == null || previous.id() != recovery.manifestId())) {
                throw new IllegalStateException("The failed dispatch attempt is no longer current");
            }
            List<Allocation> active = allocations.activeForRequest(requestId);
            if (active.isEmpty()) {
                throw new IllegalStateException("The request has no active reservations to dispatch");
            }
            int totalLoad = totalLoad(active);
            Vehicle vehicle = vehicles.findById(vehicleId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle #" + vehicleId + " does not exist"));
            if (vehicle.status() != VehicleStatus.AVAILABLE) {
                throw new IllegalStateException("Vehicle " + vehicle.registrationNumber() + " is not available");
            }
            if (totalLoad > vehicle.capacity()) {
                throw new IllegalStateException("Request load " + totalLoad + " exceeds vehicle capacity "
                        + vehicle.capacity());
            }
            if (!vehicles.claim(vehicleId)) {
                throw new IllegalStateException("The vehicle is no longer available");
            }
            String timestamp = RequestService.now();
            long manifestId = manifests.insert(requestId, vehicleId, driver, timestamp);
            int manifestLoad = 0;
            for (Allocation allocation : active) {
                manifests.insertItem(manifestId, requestId, allocation);
                manifestLoad = Math.addExact(manifestLoad, allocation.quantity());
            }
            if (manifestLoad != totalLoad) {
                throw new IllegalStateException("Dispatch manifest load does not match active reservations");
            }
            requests.updateStatus(requestId, next);
            requests.addHistory(requestId, request.status().name(), next.name(), actor.fullName(), timestamp);
            if (recovery != null && !failures.markResolved(recovery.id(), timestamp, null)) {
                throw new IllegalStateException("The delivery failure was already recovered");
            }
            return null;
        });
    }

    public void deliver(User actor, long requestId) {
        AccessControl.require(actor, Feature.TRANSPORT);
        ReliefRequest request = requestService.load(requestId);
        RequestStatus next = RequestStates.of(request.status()).deliver();

        Database.getInstance().inTransaction(c -> {
            DispatchManifest manifest = manifests.findByRequest(requestId)
                    .orElseThrow(() -> new IllegalStateException("The request has no dispatch manifest"));
            if (manifest.status() != ManifestStatus.DISPATCHED) {
                throw new IllegalStateException("The dispatch manifest is already " + manifest.status());
            }
            String timestamp = RequestService.now();
            if (!manifests.markDelivered(manifest.id(), timestamp)) {
                throw new IllegalStateException("The dispatch manifest could not be completed");
            }
            if (!vehicles.release(manifest.vehicleId())) {
                throw new IllegalStateException("The dispatched vehicle is not marked in transit");
            }
            requests.updateStatus(requestId, next);
            requests.addHistory(requestId, request.status().name(), next.name(), actor.fullName(), timestamp);
            return null;
        });
    }

    public void reportDeliveryFailure(User actor, long requestId, String reason,
                                      DeliveryRecoveryAction recoveryAction, String recoveryNotes) {
        AccessControl.require(actor, Feature.TRANSPORT);
        ReliefRequest request = requestService.load(requestId);
        RequestStatus next = RequestStates.of(request.status()).deliveryFailed();
        String normalizedReason = requireText(reason, "Failure reason", 500);
        if (recoveryAction == null) {
            throw new IllegalArgumentException("Select a recovery action");
        }
        String notes = optionalText(recoveryNotes, "Recovery notes", 500);

        Database.getInstance().inTransaction(c -> {
            DispatchManifest manifest = manifests.findByRequest(requestId)
                    .orElseThrow(() -> new IllegalStateException("The request has no dispatch manifest"));
            if (manifest.status() != ManifestStatus.DISPATCHED) {
                throw new IllegalStateException("The latest dispatch attempt is already " + manifest.status());
            }
            String timestamp = RequestService.now();
            if (!manifests.markDeliveryFailed(manifest.id(), timestamp)) {
                throw new IllegalStateException("The dispatch attempt could not be marked as failed");
            }
            if (!vehicles.release(manifest.vehicleId())) {
                throw new IllegalStateException("The dispatched vehicle is not marked in transit");
            }
            failures.insert(requestId, manifest.id(), normalizedReason, actor.id(), timestamp,
                    recoveryAction, notes);
            requests.updateStatus(requestId, next);
            requests.addHistory(requestId, request.status().name(), next.name(), actor.fullName(), timestamp);
            return null;
        });
    }

    public ReallocationRecoveryResult returnForReallocation(User actor, long requestId, String recoveryNotes) {
        AccessControl.require(actor, Feature.ALLOCATE);
        ReliefRequest request = requestService.load(requestId);
        RequestStatus next = RequestStates.of(request.status()).returnForReallocation();
        DeliveryFailure failure = currentFailure(requestId, DeliveryRecoveryAction.REALLOCATE);
        String notes = optionalText(recoveryNotes, "Recovery notes", 500);

        return Database.getInstance().inTransaction(c -> {
            DispatchManifest manifest = manifests.findByRequest(requestId)
                    .orElseThrow(() -> new IllegalStateException("The request has no failed dispatch attempt"));
            if (manifest.id() != failure.manifestId() || manifest.status() != ManifestStatus.DELIVERY_FAILED) {
                throw new IllegalStateException("The latest dispatch attempt is not awaiting reallocation");
            }
            Vehicle vehicle = vehicles.findById(manifest.vehicleId())
                    .orElseThrow(() -> new IllegalStateException("The failed dispatch vehicle no longer exists"));
            if (vehicle.status() != VehicleStatus.AVAILABLE) {
                throw new IllegalStateException("The failed dispatch vehicle must be available before reallocation");
            }
            List<Allocation> active = allocations.activeForRequest(requestId);
            if (active.isEmpty()) {
                throw new IllegalStateException("The failed request has no active reservations to release");
            }
            String timestamp = RequestService.now();
            int releasedCount = 0;
            long releasedQuantity = 0;
            for (Allocation allocation : active) {
                if (!allocations.markReleased(allocation.id(), actor.id(), timestamp)) {
                    throw new IllegalStateException("Allocation #" + allocation.id() + " was already released");
                }
                inventory.adjust(allocation.centerId(), allocation.resourceId(), allocation.quantity());
                requests.addToItemAllocated(requestId, allocation.resourceId(), -allocation.quantity());
                allocations.addEvent(requestId, allocation.id(), AllocationEventType.RELEASED,
                        allocation.centerId(), allocation.resourceId(), allocation.quantity(), actor.id(), timestamp);
                releasedCount++;
                releasedQuantity += allocation.quantity();
            }
            if (!failures.markResolved(failure.id(), timestamp, notes)) {
                throw new IllegalStateException("The delivery failure was already recovered");
            }
            requests.updateStatus(requestId, next);
            requests.addHistory(requestId, request.status().name(), next.name(), actor.fullName(), timestamp);
            return new ReallocationRecoveryResult(releasedCount, releasedQuantity);
        });
    }

    private DeliveryFailure currentFailure(long requestId, DeliveryRecoveryAction requiredAction) {
        DeliveryFailure failure = failures.latestForRequest(requestId)
                .orElseThrow(() -> new IllegalStateException("The request has no delivery failure record"));
        if (failure.resolved()) {
            throw new IllegalStateException("The latest delivery failure is already resolved");
        }
        if (failure.recoveryAction() != requiredAction) {
            throw new IllegalStateException("The selected recovery action is " + failure.recoveryAction().label());
        }
        return failure;
    }

    private static int totalLoad(List<Allocation> allocations) {
        try {
            int total = 0;
            for (Allocation allocation : allocations) {
                total = Math.addExact(total, allocation.quantity());
            }
            return total;
        } catch (ArithmeticException e) {
            throw new IllegalStateException("Dispatch load is too large", e);
        }
    }

    private static String requireDriver(String driverName) {
        return requireText(driverName, "Driver name", 100);
    }

    private static String requireText(String value, String label, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    private static String optionalText(String value, String label, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return requireText(value, label, maxLength);
    }
}
