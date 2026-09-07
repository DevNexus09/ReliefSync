package com.reliefsync.application;

import com.reliefsync.controller.DashboardController;
import com.reliefsync.controller.LoginController;
import com.reliefsync.security.SessionManager;
import com.reliefsync.service.AuthenticationService;
import java.util.Objects;
import javafx.util.Callback;

public final class ControllerFactory implements Callback<Class<?>, Object> {
  private final AuthenticationService authenticationService;
  private final SessionManager sessionManager;

  public ControllerFactory(
      AuthenticationService authenticationService, SessionManager sessionManager) {
    this.authenticationService = Objects.requireNonNull(authenticationService);
    this.sessionManager = Objects.requireNonNull(sessionManager);
  }

  @Override
  public Object call(Class<?> controllerType) {
    if (controllerType == LoginController.class) {
      return new LoginController(authenticationService);
    }
    if (controllerType == DashboardController.class) {
      return new DashboardController(authenticationService, sessionManager);
    }
    throw new IllegalArgumentException("Unsupported FXML controller: " + controllerType.getName());
  }
}
