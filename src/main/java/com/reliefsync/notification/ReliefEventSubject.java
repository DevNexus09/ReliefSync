package com.reliefsync.notification;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Ordered, synchronous Observer subject. Observer exceptions intentionally propagate. */
public final class ReliefEventSubject {
    private final Set<ReliefEventObserver> observers = new LinkedHashSet<>();

    public void register(ReliefEventObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    public void unregister(ReliefEventObserver observer) {
        observers.remove(observer);
    }

    public void publish(ReliefEvent event) {
        for (ReliefEventObserver observer : List.copyOf(observers)) {
            observer.onEvent(Objects.requireNonNull(event, "event"));
        }
    }

    public void clear() {
        observers.clear();
    }
}
