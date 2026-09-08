package com.reliefsync.service;

import com.reliefsync.db.Database;
import com.reliefsync.model.DraftItem;
import com.reliefsync.model.Priority;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.User;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.VerificationRepository;
import com.reliefsync.state.RequestState;
import com.reliefsync.state.RequestStates;
import com.reliefsync.verification.VerificationChains;
import com.reliefsync.verification.VerificationOutcome;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The relief-request workflow: draft creation, submission, the verification
 * chain, and cancellation. State transitions are delegated to the State
 * classes; round depth is delegated to the verification chain.
 */
public class RequestService {

    private final RequestRepository requests = new RequestRepository();
    private final VerificationRepository verifications = new VerificationRepository();

    /** True when the area already has an open request (probable duplicate). */
    public boolean hasOpenRequestForArea(long areaId) {
        return requests.openRequestCountForArea(areaId) > 0;
    }

    public long createDraft(User actor, long areaId, Priority priority, String note, List<DraftItem> items) {
        AccessControl.require(actor, Feature.REQUESTS);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A request needs at least one item");
        }
        Set<Long> seen = new HashSet<>();
        for (DraftItem item : items) {
            if (item.quantity() <= 0) {
                throw new IllegalArgumentException("Item quantities must be greater than zero");
            }
            if (!seen.add(item.resourceId())) {
                throw new IllegalArgumentException(
                        "Duplicate item: " + item.resourceName() + " appears more than once");
            }
        }
        return Database.getInstance().inTransaction(c -> {
            long id = requests.insert(areaId, priority, RequestStatus.DRAFT, note == null ? "" : note.trim(),
                    actor.id(), now());
            for (DraftItem item : items) {
                requests.insertItem(id, item.resourceId(), item.quantity());
            }
            requests.addHistory(id, "NEW", RequestStatus.DRAFT.name(), actor.fullName(), now());
            return id;
        });
    }

    public void submit(User actor, long requestId) {
        AccessControl.require(actor, Feature.REQUESTS);
        ReliefRequest request = load(requestId);
        RequestStatus next = RequestStates.of(request.status()).submit();
        transition(request, next, actor);
    }

    /** One human decision for the current verification round of the chain. */
    public VerificationOutcome decideVerification(User actor, long requestId, boolean approve, String comment) {
        AccessControl.require(actor, Feature.VERIFY);
        ReliefRequest request = load(requestId);
        if (request.status() != RequestStatus.SUBMITTED) {
            throw new IllegalStateException("Cannot verify a request in state " + request.status());
        }
        VerificationOutcome outcome = VerificationChains.forPriority(request.priority())
                .handle(verifications.forRequest(requestId), actor, approve);
        RequestState state = RequestStates.of(request.status());
        RequestStatus next = state.verdict(outcome.approved(), outcome.chainComplete());
        return Database.getInstance().inTransaction(c -> {
            verifications.insert(requestId, outcome.roundRole(), actor.id(), outcome.approved(),
                    comment == null ? "" : comment.trim());
            if (next != request.status()) {
                requests.updateStatus(requestId, next);
                requests.addHistory(requestId, request.status().name(), next.name(), actor.fullName(), now());
            }
            return outcome;
        });
    }

    public void cancel(User actor, long requestId) {
        ReliefRequest request = load(requestId);
        if (request.createdBy() != actor.id() && actor.role() != com.reliefsync.model.Role.ADMIN) {
            throw new IllegalStateException("Only the creator or an administrator can cancel a request");
        }
        RequestStatus next = RequestStates.of(request.status()).cancel();
        transition(request, next, actor);
    }

    ReliefRequest load(long requestId) {
        return requests.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request #" + requestId + " does not exist"));
    }

    void transition(ReliefRequest request, RequestStatus next, User actor) {
        Database.getInstance().inTransaction(c -> {
            requests.updateStatus(request.id(), next);
            requests.addHistory(request.id(), request.status().name(), next.name(), actor.fullName(), now());
            request.setStatus(next);
            return null;
        });
    }

    static String now() {
        return LocalDateTime.now().withNano(0).toString();
    }
}
