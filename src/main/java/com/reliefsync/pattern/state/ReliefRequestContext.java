package com.reliefsync.pattern.state;

import com.reliefsync.exception.InvalidStateTransitionException;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.enums.RequestStateType;

public final class ReliefRequestContext {
  private final ReliefRequestStateRegistry registry;
  private ReliefRequestState state;
  private AllocationProgress allocationProgress;
  private boolean hasDispatchedQuantity;

  public ReliefRequestContext(ReliefRequest request, ReliefRequestStateRegistry registry) {
    this.registry = registry;
    this.state = registry.resolve(request.state());
  }

  public ReliefRequestState state() {
    return state;
  }

  public RequestStateType stateType() {
    return state.type();
  }

  public void transitionTo(RequestStateType target) {
    state = registry.resolve(target);
  }

  public AllocationProgress allocationProgress() {
    return allocationProgress;
  }

  public void setAllocationProgress(AllocationProgress value) {
    allocationProgress = value;
  }

  public boolean hasDispatchedQuantity() {
    return hasDispatchedQuantity;
  }

  public void setHasDispatchedQuantity(boolean value) {
    hasDispatchedQuantity = value;
  }

  public InvalidStateTransitionException invalidTransition(String action) {
    return new InvalidStateTransitionException(
        "Cannot " + action + " a request in state " + state.type() + ".");
  }
}
