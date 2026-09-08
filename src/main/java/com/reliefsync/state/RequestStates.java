package com.reliefsync.state;

import com.reliefsync.model.RequestStatus;
import java.util.EnumMap;
import java.util.Map;

/** Registry resolving a persisted status to its stateless behavior object. */
public final class RequestStates {

    private static final Map<RequestStatus, RequestState> STATES = new EnumMap<>(RequestStatus.class);

    static {
        register(new DraftState());
        register(new SubmittedState());
        register(new VerifiedState());
        register(new RejectedState());
        register(new AllocatedState());
        register(new DispatchedState());
        register(new DeliveredState());
        register(new CancelledState());
    }

    private RequestStates() {
    }

    private static void register(RequestState state) {
        STATES.put(state.status(), state);
    }

    public static RequestState of(RequestStatus status) {
        return STATES.get(status);
    }
}
