package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

public final class SubmittedState extends RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.SUBMITTED;
    }

    @Override
    public RequestStatus verdict(boolean approved, boolean chainComplete) {
        if (!approved) {
            return RequestStatus.REJECTED;
        }
        return chainComplete ? RequestStatus.VERIFIED : RequestStatus.SUBMITTED;
    }

    @Override
    public RequestStatus cancel() {
        return RequestStatus.CANCELLED;
    }
}
