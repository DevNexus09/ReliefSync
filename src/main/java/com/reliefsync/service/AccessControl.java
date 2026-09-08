package com.reliefsync.service;

import com.reliefsync.model.Role;
import com.reliefsync.model.User;
import java.util.EnumSet;
import java.util.Map;

/** Central role-to-feature grants; UI and services both consult this table. */
public final class AccessControl {

    private static final Map<Role, EnumSet<Feature>> GRANTS = Map.of(
            Role.ADMIN, EnumSet.allOf(Feature.class),
            Role.AREA_COORDINATOR, EnumSet.of(Feature.DASHBOARD, Feature.REQUESTS, Feature.VERIFY, Feature.REPORTS),
            Role.CENTER_MANAGER, EnumSet.of(Feature.DASHBOARD, Feature.MASTER_DATA, Feature.INVENTORY, Feature.REPORTS),
            Role.TRANSPORT_COORDINATOR, EnumSet.of(Feature.DASHBOARD, Feature.TRANSPORT, Feature.REPORTS),
            Role.VOLUNTEER, EnumSet.of(Feature.DASHBOARD, Feature.REQUESTS),
            Role.RELIEF_COORDINATOR, EnumSet.of(Feature.DASHBOARD, Feature.VERIFY, Feature.ALLOCATE, Feature.REPORTS));

    private AccessControl() {
    }

    public static boolean can(Role role, Feature feature) {
        return GRANTS.getOrDefault(role, EnumSet.noneOf(Feature.class)).contains(feature);
    }

    public static void require(User user, Feature feature) {
        if (!can(user.role(), feature)) {
            throw new IllegalStateException(
                    "Role " + user.role().label() + " is not permitted to use " + feature);
        }
    }
}
