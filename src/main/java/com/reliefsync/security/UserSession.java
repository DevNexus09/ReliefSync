package com.reliefsync.security;

import com.reliefsync.model.enums.Role;

public record UserSession(long userId, String fullName, String username, Role role) {}
