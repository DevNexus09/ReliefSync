package com.reliefsync.model;

import com.reliefsync.model.enums.Role;
import java.time.LocalDateTime;

public record User(
    long id,
    String fullName,
    String username,
    String passwordHash,
    String passwordSalt,
    Role role,
    boolean active,
    LocalDateTime createdAt) {}
