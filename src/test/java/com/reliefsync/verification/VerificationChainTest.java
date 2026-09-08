package com.reliefsync.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.model.Priority;
import com.reliefsync.model.Role;
import com.reliefsync.model.User;
import com.reliefsync.model.Verification;
import java.util.List;
import org.junit.jupiter.api.Test;

class VerificationChainTest {

    private static final User AREA_COORD = new User(1, "ac", "Area Coord", Role.AREA_COORDINATOR);
    private static final User RELIEF_COORD = new User(2, "rc", "Relief Coord", Role.RELIEF_COORDINATOR);
    private static final User ADMIN = new User(3, "admin", "Admin", Role.ADMIN);
    private static final User VOLUNTEER = new User(4, "vol", "Volunteer", Role.VOLUNTEER);

    private static Verification approval(Role role) {
        return new Verification(0, 1, role, 1, "someone", true, "", "2026-01-01T00:00");
    }

    @Test
    void normalPriorityNeedsOneRound() {
        VerificationOutcome outcome = VerificationChains.forPriority(Priority.NORMAL)
                .handle(List.of(), AREA_COORD, true);
        assertEquals(Role.AREA_COORDINATOR, outcome.roundRole());
        assertTrue(outcome.approved());
        assertTrue(outcome.chainComplete());
    }

    @Test
    void highPriorityNeedsTwoRoundsInOrder() {
        VerificationOutcome first = VerificationChains.forPriority(Priority.HIGH)
                .handle(List.of(), AREA_COORD, true);
        assertFalse(first.chainComplete());

        VerificationOutcome second = VerificationChains.forPriority(Priority.HIGH)
                .handle(List.of(approval(Role.AREA_COORDINATOR)), RELIEF_COORD, true);
        assertEquals(Role.RELIEF_COORDINATOR, second.roundRole());
        assertTrue(second.chainComplete());
    }

    @Test
    void criticalPriorityNeedsThreeRounds() {
        List<Verification> history =
                List.of(approval(Role.AREA_COORDINATOR), approval(Role.RELIEF_COORDINATOR));
        VerificationOutcome last = VerificationChains.forPriority(Priority.CRITICAL)
                .handle(history, ADMIN, true);
        assertEquals(Role.ADMIN, last.roundRole());
        assertTrue(last.chainComplete());
    }

    @Test
    void wrongRoleCannotDecideTheCurrentRound() {
        assertThrows(IllegalStateException.class, () -> VerificationChains
                .forPriority(Priority.HIGH).handle(List.of(), RELIEF_COORD, true));
        assertThrows(IllegalStateException.class, () -> VerificationChains
                .forPriority(Priority.NORMAL).handle(List.of(), VOLUNTEER, true));
    }

    @Test
    void adminMayActForAnyRound() {
        VerificationOutcome outcome = VerificationChains.forPriority(Priority.CRITICAL)
                .handle(List.of(), ADMIN, true);
        assertEquals(Role.AREA_COORDINATOR, outcome.roundRole());
        assertFalse(outcome.chainComplete());
    }

    @Test
    void rejectionEndsTheChainImmediately() {
        VerificationOutcome outcome = VerificationChains.forPriority(Priority.CRITICAL)
                .handle(List.of(approval(Role.AREA_COORDINATOR)), RELIEF_COORD, false);
        assertFalse(outcome.approved());
        assertTrue(outcome.chainComplete());
    }

    @Test
    void completedChainRefusesFurtherDecisions() {
        assertThrows(IllegalStateException.class, () -> VerificationChains
                .forPriority(Priority.NORMAL)
                .handle(List.of(approval(Role.AREA_COORDINATOR)), AREA_COORD, true));
    }
}
