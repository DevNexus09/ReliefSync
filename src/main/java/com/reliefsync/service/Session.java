package com.reliefsync.service;

import com.reliefsync.model.User;

/** In-memory holder of the authenticated user; cleared on logout. */
public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User user() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is logged in");
        }
        return currentUser;
    }
}
