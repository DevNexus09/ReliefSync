package com.reliefsync.pattern.state;

import com.reliefsync.exception.InvalidStateTransitionException;
import com.reliefsync.model.enums.RequestStateType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ReliefRequestStateRegistry {
  private final Map<RequestStateType, ReliefRequestState> states =
      new EnumMap<>(RequestStateType.class);

  public ReliefRequestStateRegistry() {
    List.of(
            new SubmittedState(),
            new UnderVerificationState(),
            new VerifiedState(),
            new PartiallyAllocatedState(),
            new AllocatedState(),
            new DispatchedState(),
            new DeliveredState(),
            new ReturnedState(),
            new RejectedState(),
            new CancelledState())
        .forEach(s -> states.put(s.type(), s));
  }

  public ReliefRequestState resolve(RequestStateType type) {
    ReliefRequestState state = states.get(type);
    if (state == null)
      throw new InvalidStateTransitionException("Unsupported request state: " + type);
    return state;
  }
}
