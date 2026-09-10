package com.reliefsync.service;

import com.reliefsync.db.Database;
import com.reliefsync.model.PlannedAllocation;
import com.reliefsync.model.AllocationEventType;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.Shortage;
import com.reliefsync.model.User;
import com.reliefsync.repository.AllocationRepository;
import com.reliefsync.repository.InventoryRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.state.RequestStates;
import com.reliefsync.strategy.AllocationStrategies;
import com.reliefsync.strategy.AllocationStrategy;
import java.util.ArrayList;
import java.util.List;

/**
 * The allocation-to-delivery workflow: plans stock distribution with a chosen
 * Strategy, reserves inventory transactionally, then tracks dispatch and
 * delivery through the State classes.
 */
public class AllocationService {

    private final RequestRepository requests = new RequestRepository();
    private final InventoryRepository inventory = new InventoryRepository();
    private final AllocationRepository allocations = new AllocationRepository();
    private final RequestService requestService = new RequestService();
    private final NotificationService notifications = new NotificationService();

    /** Pure planning — nothing is written, so the UI can preview strategies. */
    public AllocationResult preview(long requestId, String strategyName) {
        ReliefRequest request = requestService.load(requestId);
        AllocationStrategy strategy = AllocationStrategies.byName(strategyName);
        List<RequestItem> items = requests.items(request.id());
        List<PlannedAllocation> lines = strategy.plan(items, inventory.availableStock());
        return new AllocationResult(lines, shortages(items, lines));
    }

    public AllocationResult allocate(User actor, long requestId, String strategyName) {
        AccessControl.require(actor, Feature.ALLOCATE);
        ReliefRequest request = requestService.load(requestId);
        RequestStatus next = RequestStates.of(request.status()).allocate();
        return reserve(actor, request, strategyName, AllocationEventType.ALLOCATED, next, true);
    }

    public AllocationResult reallocate(User actor, long requestId, String strategyName) {
        AccessControl.require(actor, Feature.ALLOCATE);
        ReliefRequest request = requestService.load(requestId);
        RequestStatus next = RequestStates.of(request.status()).reallocate();
        if (requests.items(requestId).stream().noneMatch(item -> item.outstanding() > 0)) {
            throw new IllegalStateException("Request #" + requestId + " is already fully allocated");
        }
        return reserve(actor, request, strategyName, AllocationEventType.REALLOCATED, next, false);
    }

    private AllocationResult reserve(User actor, ReliefRequest request, String strategyName,
                                     AllocationEventType eventType, RequestStatus next,
                                     boolean recordStatusChange) {
        AllocationStrategy strategy = AllocationStrategies.byName(strategyName);
        return Database.getInstance().inTransaction(c -> {
            List<RequestItem> items = requests.items(request.id());
            List<PlannedAllocation> lines = strategy.plan(items, inventory.availableStock());
            if (lines.isEmpty()) {
                throw new IllegalStateException("No stock is available for any requested item");
            }
            String timestamp = RequestService.now();
            long firstEventId = 0;
            for (PlannedAllocation line : lines) {
                if (line.quantity() <= 0) {
                    throw new IllegalStateException("Allocation quantities must be greater than zero");
                }
                int previous = inventory.find(line.centerId(), line.resourceId()).orElseThrow().quantity();
                inventory.decrement(line.centerId(), line.resourceId(), line.quantity());
                long allocationId = allocations.insert(request.id(), line.centerId(), line.resourceId(),
                        line.quantity(), strategy.name());
                requests.addToItemAllocated(request.id(), line.resourceId(), line.quantity());
                long eventId = allocations.addEvent(request.id(), allocationId, eventType,
                        line.centerId(), line.resourceId(), line.quantity(), actor.id(), timestamp);
                if (firstEventId == 0) firstEventId = eventId;
                notifications.inventoryChanged(actor, line.centerId(), line.resourceId(), previous, timestamp);
            }
            if (recordStatusChange) {
                requests.updateStatus(request.id(), next);
                requests.addHistory(request.id(), request.status().name(), next.name(),
                        actor.fullName(), timestamp);
            }
            List<Shortage> shortageList = shortages(items, lines);
            int total = lines.stream().mapToInt(PlannedAllocation::quantity).sum();
            notifications.requestAllocated(actor, request, eventType, firstEventId, strategy.name(), total,
                    shortageList, timestamp);
            return new AllocationResult(lines, shortageList);
        });
    }

    private static List<Shortage> shortages(List<RequestItem> items, List<PlannedAllocation> lines) {
        List<Shortage> out = new ArrayList<>();
        for (RequestItem item : items) {
            int planned = lines.stream()
                    .filter(l -> l.resourceId() == item.resourceId())
                    .mapToInt(PlannedAllocation::quantity)
                    .sum();
            int missing = item.outstanding() - planned;
            if (missing > 0) {
                out.add(new Shortage(item.resourceName(), missing));
            }
        }
        return out;
    }
}
