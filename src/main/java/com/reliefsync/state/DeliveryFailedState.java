package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;

public final class DeliveryFailedState extends RequestState {

    @Override
    public RequestStatus status() {
        return RequestStatus.DELIVERY_FAILED;
    }

    @Override
    public RequestStatus retryDelivery() {
        return RequestStatus.DISPATCHED;
    }

    @Override
    public RequestStatus returnForReallocation() {
        return RequestStatus.ALLOCATED;
    }
}
