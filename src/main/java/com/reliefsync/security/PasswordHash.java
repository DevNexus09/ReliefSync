package com.reliefsync.security;

public record PasswordHash(String salt, String hash) {}
