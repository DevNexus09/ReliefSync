package com.reliefsync.pattern.state;

import com.reliefsync.model.enums.RequestStateType;

public final class UnderVerificationState implements ReliefRequestState {
  public RequestStateType type() {
    return RequestStateType.UNDER_VERIFICATION;
  }

  public void markVerified(ReliefRequestContext c) {
    c.transitionTo(RequestStateType.VERIFIED);
  }

  public void returnForCorrection(ReliefRequestContext c) {
    c.transitionTo(RequestStateType.RETURNED);
  }

  public void reject(ReliefRequestContext c) {
    c.transitionTo(RequestStateType.REJECTED);
  }
}
