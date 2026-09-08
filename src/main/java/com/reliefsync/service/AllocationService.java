package com.reliefsync.service;

import com.reliefsync.db.Database;
import com.reliefsync.model.PlannedAllocation;
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
        AllocationStrategy strategy = AllocationStrategies.byName(strategyName);
        return Database.getInstance().inTransaction(c -> {
            List<RequestItem> items = requests.items(request.id());
            List<PlannedAllocation> lines = strategy.plan(items, inventory.availableStock());
            if (lines.isEmpty()) {
                throw new IllegalStateException("No stock is available for any requested item");
            }
            for (PlannedAllocation line : lines) {
                inventory.decrement(line.centerId(), line.resourceId(), line.quantity());
                allocations.insert(request.id(), line.centerId(), line.resourceId(),
                        line.quantity(), strategy.name());
                requests.addToItemAllocated(request.id(), line.resourceId(), line.quantity());
            }
            requests.updateStatus(request.id(), next);
            requests.addHistory(request.id(), request.status().name(), next.name(),
                    actor.fullName(), RequestService.now());
            return new AllocationResult(lines, shortages(items, lines));
        });
    }

    public void dispatch(User actor, long requestId) {
        AccessControl.require(actor, Feature.TRANSPORT);
        ReliefRequest request = requestService.load(requestId);
        RequestStatus next = RequestStates.of(request.status()).dispatch();
        requestService.transition(request, next, actor);
    }

    public void deliver(User actor, long requestId) {
        AccessControl.require(actor, Feature.TRANSPORT);
        ReliefRequest request = requestService.load(requestId);
        RequestStatus next = RequestStates.of(request.status()).deliver();
        requestService.transition(request, next, actor);
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
