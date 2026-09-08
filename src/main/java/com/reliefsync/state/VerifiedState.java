package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

public final class VerifiedState extends RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.VERIFIED;
    }

    @Override
    public RequestStatus allocate() {
        return RequestStatus.ALLOCATED;
    }

    @Override
    public RequestStatus cancel() {
        return RequestStatus.CANCELLED;
    }
}
