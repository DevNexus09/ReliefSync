package com.reliefsync.verification;

import com.reliefsync.model.Role;
import com.reliefsync.model.User;
import com.reliefsync.model.Verification;
import java.util.List;

/**
 * Chain of Responsibility: one handler per required verification round.
 *
 * A request's priority decides how deep the chain is. Each handler either
 * forwards to its successor (its round is already approved) or takes the
 * current human decision itself, enforcing that the actor holds the round's
 * role. Adding a new approval level means adding one handler class and linking
 * it in {@link VerificationChains} — no service code changes.
 */
public abstract class VerificationHandler {

    private VerificationHandler next;

    public abstract Role requiredRole();

    public final VerificationHandler setNext(VerificationHandler next) {
        this.next = next;
        return next;
    }

    public final VerificationOutcome handle(List<Verification> history, User actor, boolean approve) {
        boolean roundDone = history.stream()
                .anyMatch(v -> v.approved() && v.roundRole() == requiredRole());
        if (roundDone) {
            if (next == null) {
                throw new IllegalStateException("Verification is already complete for this request");
            }
            return next.handle(history, actor, approve);
        }
        if (actor.role() != requiredRole() && actor.role() != Role.ADMIN) {
            throw new IllegalStateException(
                    "The current verification round requires role: " + requiredRole().label());
        }
        boolean complete = !approve || next == null;
        return new VerificationOutcome(requiredRole(), approve, complete);
    }
}
