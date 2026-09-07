package com.reliefsync.service;

import com.reliefsync.exception.AuthenticationException;
import com.reliefsync.model.User;
import com.reliefsync.repository.UserRepository;
import com.reliefsync.security.PasswordHasher;
import com.reliefsync.security.SessionManager;
import com.reliefsync.security.UserSession;
import java.util.Arrays;
import java.util.Objects;

public final class AuthenticationService {
  private static final String INVALID_CREDENTIALS = "Invalid username or password.";

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final SessionManager sessionManager;

  public AuthenticationService(
      UserRepository userRepository, PasswordHasher passwordHasher, SessionManager sessionManager) {
    this.userRepository = Objects.requireNonNull(userRepository);
    this.passwordHasher = Objects.requireNonNull(passwordHasher);
    this.sessionManager = Objects.requireNonNull(sessionManager);
  }

  public UserSession login(String username, char[] password) {
    if (username == null || username.isBlank() || password == null || password.length == 0) {
      clear(password);
      throw new AuthenticationException(INVALID_CREDENTIALS);
    }
    User user =
        userRepository
            .findByUsername(username.trim())
            .orElseThrow(
                () -> {
                  clear(password);
                  return new AuthenticationException(INVALID_CREDENTIALS);
                });
    if (!user.active()) {
      clear(password);
      throw new AuthenticationException(INVALID_CREDENTIALS);
    }
    if (!passwordHasher.verify(password, user.passwordSalt(), user.passwordHash())) {
      throw new AuthenticationException(INVALID_CREDENTIALS);
    }
    UserSession session = new UserSession(user.id(), user.fullName(), user.username(), user.role());
    sessionManager.login(session);
    return session;
  }

  public void logout() {
    sessionManager.logout();
  }

  private void clear(char[] password) {
    if (password != null) Arrays.fill(password, '\0');
  }
}
