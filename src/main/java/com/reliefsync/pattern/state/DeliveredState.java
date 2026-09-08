package com.reliefsync.pattern.state;

import com.reliefsync.model.enums.RequestStateType;

public final class DeliveredState implements ReliefRequestState {
  public RequestStateType type() {
    return RequestStateType.DELIVERED;
  }
}
