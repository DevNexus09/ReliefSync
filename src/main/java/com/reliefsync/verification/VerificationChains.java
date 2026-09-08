package com.reliefsync.verification;

import com.reliefsync.model.Priority;
import com.reliefsync.model.Role;
import java.util.List;

/** Builds the verification chain that matches a request's priority. */
public final class VerificationChains {

    private VerificationChains() {
    }

    public static VerificationHandler forPriority(Priority priority) {
        VerificationHandler head = new AreaCoordinatorVerifier();
        if (priority == Priority.NORMAL) {
            return head;
        }
        VerificationHandler second = head.setNext(new ReliefCoordinatorVerifier());
        if (priority == Priority.CRITICAL) {
            second.setNext(new AdminVerifier());
        }
        return head;
    }

    /** Roles required in order, for display in the UI. */
    public static List<Role> requiredRoles(Priority priority) {
        return switch (priority) {
            case NORMAL -> List.of(Role.AREA_COORDINATOR);
            case HIGH -> List.of(Role.AREA_COORDINATOR, Role.RELIEF_COORDINATOR);
            case CRITICAL -> List.of(Role.AREA_COORDINATOR, Role.RELIEF_COORDINATOR, Role.ADMIN);
        };
    }
}
