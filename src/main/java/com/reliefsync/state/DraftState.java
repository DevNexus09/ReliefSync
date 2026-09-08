package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

public final class DraftState extends RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.DRAFT;
    }

    @Override
    public RequestStatus submit() {
        return RequestStatus.SUBMITTED;
    }

    @Override
    public RequestStatus cancel() {
        return RequestStatus.CANCELLED;
    }
}
