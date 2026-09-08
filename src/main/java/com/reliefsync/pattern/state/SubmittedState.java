package com.reliefsync.pattern.state;

import com.reliefsync.model.enums.RequestStateType;

public final class SubmittedState implements ReliefRequestState {
  public RequestStateType type() {
    return RequestStateType.SUBMITTED;
  }

  public void beginVerification(ReliefRequestContext c) {
    c.transitionTo(RequestStateType.UNDER_VERIFICATION);
  }

  public void cancel(ReliefRequestContext c) {
    c.transitionTo(RequestStateType.CANCELLED);
  }
}
