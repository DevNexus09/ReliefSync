package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

/** Stock is reserved here; cancellation releases it before changing state. */
public final class AllocatedState extends RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.ALLOCATED;
    }

    @Override
    public RequestStatus dispatch() {
        return RequestStatus.DISPATCHED;
    }

    @Override
    public RequestStatus reallocate() {
        return RequestStatus.ALLOCATED;
    }

    @Override
    public RequestStatus cancel() {
        return RequestStatus.CANCELLED;
    }
}
