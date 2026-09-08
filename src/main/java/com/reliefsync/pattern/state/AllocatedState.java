package com.reliefsync.pattern.state;

import com.reliefsync.exception.InvalidStateTransitionException;
import com.reliefsync.model.enums.RequestStateType;

public final class AllocatedState implements ReliefRequestState {
  public RequestStateType type() {
    return RequestStateType.ALLOCATED;
  }

  public void dispatch(ReliefRequestContext c) {
    c.transitionTo(RequestStateType.DISPATCHED);
  }

  public void cancel(ReliefRequestContext c) {
    if (c.hasDispatchedQuantity())
      throw new InvalidStateTransitionException("Cannot cancel after dispatch has started.");
    c.transitionTo(RequestStateType.CANCELLED);
  }
}
