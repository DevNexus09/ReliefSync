package com.reliefsync.facade;

import com.reliefsync.model.Allocation;
import com.reliefsync.model.DraftItem;
import com.reliefsync.model.Priority;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.RequestRow;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.StatusChange;
import com.reliefsync.model.User;
import com.reliefsync.model.Verification;
import com.reliefsync.repository.AllocationRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.VerificationRepository;
import com.reliefsync.service.AllocationResult;
import com.reliefsync.service.AllocationService;
import com.reliefsync.service.RequestService;
import com.reliefsync.strategy.AllocationStrategies;
import com.reliefsync.verification.VerificationOutcome;
import java.util.List;

/**
 * Facade over the cross-service relief workflow. The UI talks to this single
 * API for every step from draft to delivery, so controllers never need to know
 * which service, repository, or pattern implements a step.
 */
public class ReliefOperationFacade {

    private final RequestService requestService = new RequestService();
    private final AllocationService allocationService = new AllocationService();
    private final RequestRepository requests = new RequestRepository();
    private final VerificationRepository verifications = new VerificationRepository();
    private final AllocationRepository allocations = new AllocationRepository();

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

    public void cancel(User actor, long requestId) {
        requestService.cancel(actor, requestId);
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

    public void dispatch(User actor, long requestId) {
        allocationService.dispatch(actor, requestId);
    }

    public void deliver(User actor, long requestId) {
        allocationService.deliver(actor, requestId);
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
}
