package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

/** Terminal state. */
public final class DeliveredState extends RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.DELIVERED;
    }
}
