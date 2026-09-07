package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.application.SceneManager;
import com.reliefsync.security.SessionManager;
import com.reliefsync.security.UserSession;
import com.reliefsync.service.AuthenticationService;
import java.util.Locale;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {
  private final AuthenticationService authenticationService;
  private final SessionManager sessionManager;

  @FXML private Label databaseStatusLabel;
  @FXML private Label welcomeLabel;
  @FXML private Label usernameLabel;
  @FXML private Label roleLabel;

  public DashboardController(
      AuthenticationService authenticationService, SessionManager sessionManager) {
    this.authenticationService = Objects.requireNonNull(authenticationService);
    this.sessionManager = Objects.requireNonNull(sessionManager);
  }

  @FXML
  private void initialize() {
    boolean connected = SceneManager.isDatabaseConnected();
    databaseStatusLabel.setText(connected ? "Database: Connected" : "Database: Connection Failed");
    databaseStatusLabel.getStyleClass().add(connected ? "database-connected" : "database-failed");
    UserSession session = sessionManager.requireCurrentSession();
    welcomeLabel.setText("Welcome, " + session.fullName());
    usernameLabel.setText("Username: " + session.username());
    roleLabel.setText("Role: " + formatRole(session.role().name()));
  }

  @FXML
  private void signOut() {
    authenticationService.logout();
    NavigationService.showLogin();
  }

  private String formatRole(String role) {
    String[] words = role.toLowerCase(Locale.ROOT).split("_");
    StringBuilder result = new StringBuilder();
    for (String word : words) {
      if (!result.isEmpty()) result.append(' ');
      result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }
    return result.toString();
  }
}
