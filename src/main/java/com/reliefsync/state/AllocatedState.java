package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

/** Stock is reserved here, so plain cancel is not allowed any more. */
public final class AllocatedState extends RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.ALLOCATED;
    }

    @Override
    public RequestStatus dispatch() {
        return RequestStatus.DISPATCHED;
    }
}
