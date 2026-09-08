package com.reliefsync.pattern.state;

import com.reliefsync.exception.InvalidStateTransitionException;
import com.reliefsync.model.enums.RequestStateType;

public final class PartiallyAllocatedState implements ReliefRequestState {
  public RequestStateType type() {
    return RequestStateType.PARTIALLY_ALLOCATED;
  }

  public void allocate(ReliefRequestContext c) {
    if (c.allocationProgress() == null)
      throw new InvalidStateTransitionException("Allocation progress is required.");
    c.transitionTo(
        c.allocationProgress() == AllocationProgress.PARTIAL
            ? RequestStateType.PARTIALLY_ALLOCATED
            : RequestStateType.ALLOCATED);
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
