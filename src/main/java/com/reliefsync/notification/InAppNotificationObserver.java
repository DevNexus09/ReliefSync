package com.reliefsync.notification;

import com.reliefsync.model.User;
import com.reliefsync.repository.NotificationRepository;

/** Concrete Observer that resolves recipients and persists one idempotent row per recipient. */
public final class InAppNotificationObserver implements ReliefEventObserver {
    private final NotificationRecipientPolicy recipients = new NotificationRecipientPolicy();
    private final NotificationRepository notifications = new NotificationRepository();

    @Override
    public void onEvent(ReliefEvent event) {
        for (User recipient : recipients.recipients(event)) {
            notifications.insert(recipient.id(), event.eventType(), event.eventKey(), event.title(),
                    event.message(), event.requestId(), event.occurredAt());
        }
    }
}
