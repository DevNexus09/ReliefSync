package com.reliefsync.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void correctPasswordVerifies() {
        String stored = PasswordHasher.hash("ReliefSync@2026");
        assertTrue(PasswordHasher.verify("ReliefSync@2026", stored));
    }

    @Test
    void wrongPasswordFails() {
        String stored = PasswordHasher.hash("ReliefSync@2026");
        assertFalse(PasswordHasher.verify("wrong-password", stored));
    }

    @Test
    void saltMakesHashesUnique() {
        assertNotEquals(PasswordHasher.hash("same"), PasswordHasher.hash("same"));
    }

    @Test
    void malformedStoredValueFailsSafely() {
        assertFalse(PasswordHasher.verify("anything", "not-a-valid-hash"));
        assertFalse(PasswordHasher.verify("anything", "1:2:3:4"));
    }
}
