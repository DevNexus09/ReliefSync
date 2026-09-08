package com.reliefsync.pattern.state;

import com.reliefsync.exception.InvalidStateTransitionException;
import com.reliefsync.model.enums.RequestStateType;

public final class VerifiedState implements ReliefRequestState {
  public RequestStateType type() {
    return RequestStateType.VERIFIED;
  }

  public void allocate(ReliefRequestContext c) {
    if (c.allocationProgress() == null)
      throw new InvalidStateTransitionException("Allocation progress is required.");
    c.transitionTo(
        c.allocationProgress() == AllocationProgress.PARTIAL
            ? RequestStateType.PARTIALLY_ALLOCATED
            : RequestStateType.ALLOCATED);
  }

  public void cancel(ReliefRequestContext c) {
    c.transitionTo(RequestStateType.CANCELLED);
  }
}
