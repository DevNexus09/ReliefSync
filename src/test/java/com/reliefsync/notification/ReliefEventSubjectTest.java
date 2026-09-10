package com.reliefsync.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.reliefsync.model.NotificationEventType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReliefEventSubjectTest {
    private final ReliefEvent event = new ReliefEvent(NotificationEventType.REQUEST_DELIVERED,
            "key", 1, "Actor", 2L, null, "Title", "Message", "2026-01-01T00:00:00");

    @Test void registeredObserverIsNotifiedAndUnregisteredObserverIsNot() {
        ReliefEventSubject subject = new ReliefEventSubject();
        List<ReliefEvent> received = new ArrayList<>();
        ReliefEventObserver observer = received::add;
        subject.register(observer);
        subject.publish(event);
        subject.unregister(observer);
        subject.publish(event);
        assertEquals(List.of(event), received);
    }

    @Test void duplicateRegistrationProducesOneCallback() {
        ReliefEventSubject subject = new ReliefEventSubject();
        List<ReliefEvent> received = new ArrayList<>();
        ReliefEventObserver observer = received::add;
        subject.register(observer);
        subject.register(observer);
        subject.publish(event);
        assertEquals(1, received.size());
    }

    @Test void observerAndEventOrderArePreserved() {
        ReliefEventSubject subject = new ReliefEventSubject();
        List<String> calls = new ArrayList<>();
        subject.register(e -> calls.add("first:" + e.eventKey()));
        subject.register(e -> calls.add("second:" + e.eventKey()));
        subject.publish(event);
        ReliefEvent event2 = new ReliefEvent(event.eventType(), "key-2", 1, "Actor", 2L,
                null, "Title", "Message", "2026-01-01T00:00:01");
        subject.publish(event2);
        assertEquals(List.of("first:key", "second:key", "first:key-2", "second:key-2"), calls);
    }

    @Test void observerFailurePropagatesAndStopsLaterCallbacks() {
        ReliefEventSubject subject = new ReliefEventSubject();
        List<String> calls = new ArrayList<>();
        subject.register(e -> { throw new IllegalStateException("persistence failed"); });
        subject.register(e -> calls.add("should not run"));
        assertThrows(IllegalStateException.class, () -> subject.publish(event));
        assertEquals(List.of(), calls);
    }
}
