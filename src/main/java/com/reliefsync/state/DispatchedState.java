package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

public final class DispatchedState extends RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.DISPATCHED;
    }

    @Override
    public RequestStatus deliver() {
        return RequestStatus.DELIVERED;
    }

    @Override
    public RequestStatus deliveryFailed() {
        return RequestStatus.DELIVERY_FAILED;
    }
}
