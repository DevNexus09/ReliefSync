package com.reliefsync.model;

public record Notification(long id, long recipientId, NotificationEventType eventType,
                           String eventKey, String title, String message, Long requestId,
                           String createdAt, String readAt) {
    public boolean unread() {
        return readAt == null;
    }

    public String stateLabel() {
        return unread() ? "Unread" : "Read";
    }
}
