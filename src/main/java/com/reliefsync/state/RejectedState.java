package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

/** Terminal state. */
public final class RejectedState extends RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.REJECTED;
    }
}
