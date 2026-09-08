package com.reliefsync.pattern.state;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefsync.exception.InvalidStateTransitionException;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.enums.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReliefRequestStateTest {
  private final ReliefRequestStateRegistry registry = new ReliefRequestStateRegistry();

  @Test
  void legalVerificationAndReturnTransitionsWork() {
    ReliefRequestContext submitted = context(RequestStateType.SUBMITTED);
    submitted.state().beginVerification(submitted);
    assertEquals(RequestStateType.UNDER_VERIFICATION, submitted.stateType());
    submitted.state().returnForCorrection(submitted);
    assertEquals(RequestStateType.RETURNED, submitted.stateType());
    submitted.state().submit(submitted);
    assertEquals(RequestStateType.SUBMITTED, submitted.stateType());
  }

  @Test
  void underVerificationCanVerifyOrReject() {
    ReliefRequestContext verified = context(RequestStateType.UNDER_VERIFICATION);
    verified.state().markVerified(verified);
    assertEquals(RequestStateType.VERIFIED, verified.stateType());
    ReliefRequestContext rejected = context(RequestStateType.UNDER_VERIFICATION);
    rejected.state().reject(rejected);
    assertEquals(RequestStateType.REJECTED, rejected.stateType());
  }

  @Test
  void allocationProgressControlsTargetWithoutAllocationLogic() {
    ReliefRequestContext partial = context(RequestStateType.VERIFIED);
    partial.setAllocationProgress(AllocationProgress.PARTIAL);
    partial.state().allocate(partial);
    assertEquals(RequestStateType.PARTIALLY_ALLOCATED, partial.stateType());
    partial.setAllocationProgress(AllocationProgress.FULL);
    partial.state().allocate(partial);
    assertEquals(RequestStateType.ALLOCATED, partial.stateType());
  }

  @Test
  void illegalAndTerminalTransitionsFail() {
    assertThrows(
        InvalidStateTransitionException.class,
        () ->
            context(RequestStateType.SUBMITTED)
                .state()
                .dispatch(context(RequestStateType.SUBMITTED)));
    assertThrows(
        InvalidStateTransitionException.class,
        () ->
            context(RequestStateType.DELIVERED)
                .state()
                .allocate(context(RequestStateType.DELIVERED)));
    assertThrows(
        InvalidStateTransitionException.class,
        () ->
            context(RequestStateType.REJECTED).state().submit(context(RequestStateType.REJECTED)));
    assertThrows(
        InvalidStateTransitionException.class,
        () ->
            context(RequestStateType.CANCELLED)
                .state()
                .beginVerification(context(RequestStateType.CANCELLED)));
  }

  @Test
  void cancellationAfterDispatchActivityFails() {
    ReliefRequestContext allocated = context(RequestStateType.ALLOCATED);
    allocated.setHasDispatchedQuantity(true);
    assertThrows(InvalidStateTransitionException.class, () -> allocated.state().cancel(allocated));
  }

  @Test
  void cancellationFromSubmittedWorks() {
    ReliefRequestContext c = context(RequestStateType.SUBMITTED);
    c.state().cancel(c);
    assertEquals(RequestStateType.CANCELLED, c.stateType());
  }

  private ReliefRequestContext context(RequestStateType state) {
    LocalDateTime now = LocalDateTime.now();
    return new ReliefRequestContext(
        new ReliefRequest(1, 1, 1, 1, RequestPriority.NORMAL, state, null, now, null, 1, now, now),
        registry);
  }
}
