package com.reliefsync.notification;

import com.reliefsync.model.NotificationEventType;
import com.reliefsync.model.Role;

/** Immutable domain event delivered synchronously to registered observers. */
public record ReliefEvent(NotificationEventType eventType, String eventKey,
                          long actorId, String actorName, Long requestId,
                          Role nextRole, String title, String message, String occurredAt) {
}
