package com.reliefsync.notification;

import com.reliefsync.model.NotificationEventType;
import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.Role;
import com.reliefsync.model.User;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The single role/ownership policy used by the notification observer. */
public final class NotificationRecipientPolicy {
    private final UserRepository users = new UserRepository();
    private final RequestRepository requests = new RequestRepository();

    public List<User> recipients(ReliefEvent event) {
        LinkedHashMap<Long, User> recipients = new LinkedHashMap<>();
        for (User user : users.findByRoles(rolesFor(event))) recipients.put(user.id(), user);
        if (includesCreator(event.eventType()) && event.requestId() != null) {
            ReliefRequest request = requests.findById(event.requestId()).orElse(null);
            if (request != null) {
                users.findById(request.createdBy()).ifPresent(u -> recipients.put(u.id(), u));
            }
        }
        User actor = recipients.get(event.actorId());
        if (actor != null && actor.role() != Role.ADMIN) recipients.remove(event.actorId());
        if (recipients.isEmpty() && event.eventType() == NotificationEventType.REQUEST_AWAITING_VERIFICATION) {
            users.findById(event.actorId()).ifPresent(u -> recipients.put(u.id(), u));
        }
        return List.copyOf(recipients.values());
    }

    private static Set<Role> rolesFor(ReliefEvent event) {
        Set<Role> roles = new LinkedHashSet<>();
        switch (event.eventType()) {
            case REQUEST_AWAITING_VERIFICATION -> {
                if (event.nextRole() != null) roles.add(event.nextRole());
                roles.add(Role.ADMIN);
            }
            case VERIFICATION_ROUND_COMPLETED -> {
                if (event.nextRole() != null) roles.add(event.nextRole());
                roles.add(Role.ADMIN);
            }
            case REQUEST_ALLOCATED -> {
                roles.add(Role.TRANSPORT_COORDINATOR);
                roles.add(Role.ADMIN);
            }
            case LOW_STOCK_WARNING -> {
                roles.add(Role.CENTER_MANAGER);
                roles.add(Role.ADMIN);
            }
            case DELIVERY_FAILURE -> {
                roles.add(Role.TRANSPORT_COORDINATOR);
                roles.add(Role.RELIEF_COORDINATOR);
                roles.add(Role.ADMIN);
            }
            case REQUEST_DELIVERED -> {
                roles.add(Role.RELIEF_COORDINATOR);
                roles.add(Role.ADMIN);
            }
        }
        return roles;
    }

    private static boolean includesCreator(NotificationEventType type) {
        return type == NotificationEventType.VERIFICATION_ROUND_COMPLETED
                || type == NotificationEventType.REQUEST_ALLOCATED
                || type == NotificationEventType.DELIVERY_FAILURE
                || type == NotificationEventType.REQUEST_DELIVERED;
    }
}
