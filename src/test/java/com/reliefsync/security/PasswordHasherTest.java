package com.reliefsync.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {
  private final PasswordHasher hasher = new PasswordHasher();

  @Test
  void givenPassword_whenHashedTwice_thenPlaintextIsAbsentAndSaltsDiffer() {
    PasswordHash first = hasher.hash("secret-password".toCharArray());
    PasswordHash second = hasher.hash("secret-password".toCharArray());
    assertNotEquals("secret-password", first.hash());
    assertNotEquals(first.salt(), second.salt());
    assertNotEquals(first.hash(), second.hash());
  }

  @Test
  void givenStoredHash_whenPasswordsVerified_thenOnlyCorrectPasswordMatches() {
    PasswordHash stored = hasher.hash("correct-password".toCharArray());
    assertTrue(hasher.verify("correct-password".toCharArray(), stored.salt(), stored.hash()));
    assertFalse(hasher.verify("wrong-password".toCharArray(), stored.salt(), stored.hash()));
  }
}
