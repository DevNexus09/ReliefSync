package com.reliefsync.service;

import com.reliefsync.db.Database;
import com.reliefsync.model.Allocation;
import com.reliefsync.model.DispatchManifest;
import com.reliefsync.model.ManifestStatus;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.User;
import com.reliefsync.model.Vehicle;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.repository.AllocationRepository;
import com.reliefsync.repository.DispatchManifestRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.VehicleRepository;
import com.reliefsync.state.RequestStates;
import java.util.List;

/** Coordinates vehicle claiming, dispatch manifests, and delivery atomically. */
public class DispatchService {

    private final RequestService requestService = new RequestService();
    private final RequestRepository requests = new RequestRepository();
    private final AllocationRepository allocations = new AllocationRepository();
    private final VehicleRepository vehicles = new VehicleRepository();
    private final DispatchManifestRepository manifests = new DispatchManifestRepository();

    public void dispatch(User actor, long requestId, long vehicleId, String driverName) {
        AccessControl.require(actor, Feature.TRANSPORT);
        ReliefRequest request = requestService.load(requestId);
        RequestStatus next = RequestStates.of(request.status()).dispatch();
        String driver = requireDriver(driverName);

        Database.getInstance().inTransaction(c -> {
            if (manifests.findByRequest(requestId).isPresent()) {
                throw new IllegalStateException("Request #" + requestId + " already has a dispatch manifest");
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
        if (driverName == null || driverName.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver name must not be empty");
        }
        return driverName.trim();
    }
}
