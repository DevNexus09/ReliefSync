package com.reliefsync.pattern.state;

import com.reliefsync.model.enums.RequestStateType;

public final class ReturnedState implements ReliefRequestState {
  public RequestStateType type() {
    return RequestStateType.RETURNED;
  }

  public void submit(ReliefRequestContext c) {
    c.transitionTo(RequestStateType.SUBMITTED);
  }

  public void cancel(ReliefRequestContext c) {
    c.transitionTo(RequestStateType.CANCELLED);
  }
}
