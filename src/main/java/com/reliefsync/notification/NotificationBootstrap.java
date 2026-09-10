package com.reliefsync.notification;

/** Explicit Observer lifecycle; safe to initialize repeatedly between application/test databases. */
public final class NotificationBootstrap {
    private static final ReliefEventSubject SUBJECT = new ReliefEventSubject();

    private NotificationBootstrap() { }

    public static synchronized void initialize() {
        SUBJECT.clear();
        SUBJECT.register(new InAppNotificationObserver());
    }

    public static synchronized void reset() {
        SUBJECT.clear();
    }

    public static void publish(ReliefEvent event) {
        SUBJECT.publish(event);
    }
}
