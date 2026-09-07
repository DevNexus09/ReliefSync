package com.reliefsync.security;

import com.reliefsync.exception.AuthenticationException;
import java.util.Objects;
import java.util.Optional;

public final class SessionManager {
  private UserSession currentSession;

  public void login(UserSession session) {
    currentSession = Objects.requireNonNull(session, "Session must not be null.");
  }

  public void logout() {
    currentSession = null;
  }

  public Optional<UserSession> getCurrentSession() {
    return Optional.ofNullable(currentSession);
  }

  public UserSession requireCurrentSession() {
    return getCurrentSession()
        .orElseThrow(() -> new AuthenticationException("No user is logged in."));
  }

  public boolean isLoggedIn() {
    return currentSession != null;
  }
}
