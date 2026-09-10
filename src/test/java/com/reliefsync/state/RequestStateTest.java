package com.reliefsync.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.reliefsync.model.RequestStatus;
import org.junit.jupiter.api.Test;

class RequestStateTest {

    @Test
    void everyStatusHasARegisteredState() {
        for (RequestStatus status : RequestStatus.values()) {
            assertNotNull(RequestStates.of(status), status + " has no state class");
            assertEquals(status, RequestStates.of(status).status());
        }
    }

    @Test
    void draftCanOnlyBeSubmittedOrCancelled() {
        RequestState draft = RequestStates.of(RequestStatus.DRAFT);
        assertEquals(RequestStatus.SUBMITTED, draft.submit());
        assertEquals(RequestStatus.CANCELLED, draft.cancel());
        assertThrows(IllegalStateException.class, draft::allocate);
        assertThrows(IllegalStateException.class, draft::dispatch);
        assertThrows(IllegalStateException.class, draft::deliver);
        assertThrows(IllegalStateException.class, draft::reallocate);
        assertThrows(IllegalStateException.class, draft::deliveryFailed);
        assertThrows(IllegalStateException.class, draft::retryDelivery);
        assertThrows(IllegalStateException.class, draft::returnForReallocation);
        assertThrows(IllegalStateException.class, () -> draft.verdict(true, true));
    }

    @Test
    void submittedVerdictsFollowChainCompletion() {
        RequestState submitted = RequestStates.of(RequestStatus.SUBMITTED);
        assertEquals(RequestStatus.SUBMITTED, submitted.verdict(true, false));
        assertEquals(RequestStatus.VERIFIED, submitted.verdict(true, true));
        assertEquals(RequestStatus.REJECTED, submitted.verdict(false, true));
    }

    @Test
    void happyPathRunsToDelivered() {
        assertEquals(RequestStatus.ALLOCATED, RequestStates.of(RequestStatus.VERIFIED).allocate());
        assertEquals(RequestStatus.DISPATCHED, RequestStates.of(RequestStatus.ALLOCATED).dispatch());
        assertEquals(RequestStatus.DELIVERED, RequestStates.of(RequestStatus.DISPATCHED).deliver());
    }

    @Test
    void failedDeliveryCanRetryOrReturnForReallocation() {
        RequestState dispatched = RequestStates.of(RequestStatus.DISPATCHED);
        assertEquals(RequestStatus.DELIVERY_FAILED, dispatched.deliveryFailed());
        RequestState failed = RequestStates.of(RequestStatus.DELIVERY_FAILED);
        assertEquals(RequestStatus.DISPATCHED, failed.retryDelivery());
        assertEquals(RequestStatus.ALLOCATED, failed.returnForReallocation());
        assertThrows(IllegalStateException.class, failed::deliver);
        assertThrows(IllegalStateException.class, failed::deliveryFailed);
    }

    @Test
    void allocatedCanReallocateOrCancelBeforeDispatch() {
        RequestState allocated = RequestStates.of(RequestStatus.ALLOCATED);
        assertEquals(RequestStatus.ALLOCATED, allocated.reallocate());
        assertEquals(RequestStatus.CANCELLED, allocated.cancel());
        assertThrows(IllegalStateException.class, allocated::submit);
        assertThrows(IllegalStateException.class, allocated::allocate);
        assertThrows(IllegalStateException.class, allocated::deliver);
    }

    @Test
    void dispatchAndDeliveryBlockCancellationAndReallocation() {
        for (RequestStatus status : new RequestStatus[] {
                RequestStatus.DISPATCHED, RequestStatus.DELIVERY_FAILED, RequestStatus.DELIVERED}) {
            assertThrows(IllegalStateException.class, () -> RequestStates.of(status).cancel());
            assertThrows(IllegalStateException.class, () -> RequestStates.of(status).reallocate());
        }
    }

    @Test
    void terminalStatesAllowNothing() {
        for (RequestStatus terminal : new RequestStatus[] {
                RequestStatus.REJECTED, RequestStatus.DELIVERED, RequestStatus.CANCELLED}) {
            RequestState state = RequestStates.of(terminal);
            assertThrows(IllegalStateException.class, state::submit);
            assertThrows(IllegalStateException.class, state::allocate);
            assertThrows(IllegalStateException.class, state::dispatch);
            assertThrows(IllegalStateException.class, state::deliver);
            assertThrows(IllegalStateException.class, state::cancel);
            assertThrows(IllegalStateException.class, state::reallocate);
            assertThrows(IllegalStateException.class, state::deliveryFailed);
            assertThrows(IllegalStateException.class, state::retryDelivery);
            assertThrows(IllegalStateException.class, state::returnForReallocation);
        }
    }
}
