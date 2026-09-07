package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.application.SceneManager;
import com.reliefsync.application.View;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.security.AuthorizationService;
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
  private final AuthorizationService authorizationService;

  @FXML private Label databaseStatusLabel;
  @FXML private Label welcomeLabel;
  @FXML private Label usernameLabel;
  @FXML private Label roleLabel;
  @FXML private javafx.scene.layout.FlowPane moduleLinks;

  public DashboardController(
      AuthenticationService authenticationService,
      SessionManager sessionManager,
      AuthorizationService authorizationService) {
    this.authenticationService = Objects.requireNonNull(authenticationService);
    this.sessionManager = Objects.requireNonNull(sessionManager);
    this.authorizationService = Objects.requireNonNull(authorizationService);
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
    addModule("Disaster Events", View.DISASTER_EVENTS, Permission.VIEW_DISASTERS, session);
    addModule("Affected Areas", View.AFFECTED_AREAS, Permission.VIEW_AFFECTED_AREAS, session);
    addModule("Relief Centers", View.RELIEF_CENTERS, Permission.VIEW_RELIEF_CENTERS, session);
    addModule("Resources", View.RESOURCES, Permission.VIEW_RESOURCES, session);
    addModule("Inventory", View.INVENTORY, Permission.VIEW_INVENTORY, session);
    addModule("Vehicles", View.VEHICLES, Permission.VIEW_VEHICLES, session);
  }

  private void addModule(String text, View view, Permission permission, UserSession session) {
    if (!authorizationService.can(session, permission)) return;
    javafx.scene.control.Button button = new javafx.scene.control.Button(text);
    button.getStyleClass().add("module-button");
    button.setOnAction(event -> NavigationService.show(view));
    moduleLinks.getChildren().add(button);
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
