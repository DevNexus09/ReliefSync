package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

/** Terminal state. */
public final class CancelledState extends RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.CANCELLED;
    }
}
