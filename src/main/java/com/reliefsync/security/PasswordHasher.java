package com.reliefsync.security;

import com.reliefsync.exception.AuthenticationException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int SALT_BYTES = 16;
  private static final int KEY_BITS = 256;
  private static final int ITERATIONS = 600_000;

  private final SecureRandom secureRandom;

  public PasswordHasher() {
    this(new SecureRandom());
  }

  PasswordHasher(SecureRandom secureRandom) {
    this.secureRandom = secureRandom;
  }

  public PasswordHash hash(char[] password) {
    requirePassword(password);
    byte[] salt = new byte[SALT_BYTES];
    secureRandom.nextBytes(salt);
    byte[] hash = null;
    try {
      hash = derive(password, salt);
      return new PasswordHash(
          Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(hash));
    } finally {
      Arrays.fill(password, '\0');
      Arrays.fill(salt, (byte) 0);
      if (hash != null) Arrays.fill(hash, (byte) 0);
    }
  }

  public boolean verify(char[] password, String encodedSalt, String encodedHash) {
    requirePassword(password);
    byte[] salt = null;
    byte[] expected = null;
    byte[] actual = null;
    try {
      salt = Base64.getDecoder().decode(encodedSalt);
      expected = Base64.getDecoder().decode(encodedHash);
      actual = derive(password, salt);
      return MessageDigest.isEqual(expected, actual);
    } catch (IllegalArgumentException exception) {
      throw new AuthenticationException("Stored credentials are invalid.");
    } finally {
      Arrays.fill(password, '\0');
      if (salt != null) Arrays.fill(salt, (byte) 0);
      if (expected != null) Arrays.fill(expected, (byte) 0);
      if (actual != null) Arrays.fill(actual, (byte) 0);
    }
  }

  private byte[] derive(char[] password, byte[] salt) {
    PBEKeySpec specification = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
    try {
      return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(specification).getEncoded();
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Password hashing is unavailable.", exception);
    } finally {
      specification.clearPassword();
    }
  }

  private void requirePassword(char[] password) {
    if (password == null || password.length == 0) {
      throw new AuthenticationException("Username and password are required.");
    }
  }
}
