package com.reliefsync.notification;

@FunctionalInterface
public interface ReliefEventObserver {
    void onEvent(ReliefEvent event);
}
