package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

/**
 * State pattern: lifecycle behavior of a relief request.
 *
 * Each concrete state overrides only the transitions that are legal from it and
 * returns the resulting status; every other action fails with a clear message.
 * Adding a new lifecycle state means adding one class here instead of growing
 * if/else chains across the services.
 */
public abstract class RequestState {

    public abstract RequestStatus status();

    /** DRAFT -> SUBMITTED */
    public RequestStatus submit() {
        throw deny("submit");
    }

    /** SUBMITTED -> VERIFIED / REJECTED / SUBMITTED (more rounds pending) */
    public RequestStatus verdict(boolean approved, boolean chainComplete) {
        throw deny("verify");
    }

    /** VERIFIED -> ALLOCATED */
    public RequestStatus allocate() {
        throw deny("allocate");
    }

    /** ALLOCATED -> ALLOCATED while outstanding quantities remain. */
    public RequestStatus reallocate() {
        throw deny("reallocate");
    }

    /** ALLOCATED -> DISPATCHED */
    public RequestStatus dispatch() {
        throw deny("dispatch");
    }

    /** DISPATCHED -> DELIVERED */
    public RequestStatus deliver() {
        throw deny("mark delivered");
    }

    /** DRAFT/SUBMITTED/VERIFIED -> CANCELLED (before stock is reserved) */
    public RequestStatus cancel() {
        throw deny("cancel");
    }

    protected final IllegalStateException deny(String action) {
        return new IllegalStateException("Cannot " + action + " a request in state " + status());
    }
}
