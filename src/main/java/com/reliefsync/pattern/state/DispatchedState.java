package com.reliefsync.pattern.state;

import com.reliefsync.model.enums.RequestStateType;

public final class DispatchedState implements ReliefRequestState {
  public RequestStateType type() {
    return RequestStateType.DISPATCHED;
  }

  public void deliver(ReliefRequestContext c) {
    c.transitionTo(RequestStateType.DELIVERED);
  }
}
