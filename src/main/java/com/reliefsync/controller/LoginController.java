package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.exception.AuthenticationException;
import com.reliefsync.service.AuthenticationService;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
  private final AuthenticationService authenticationService;

  @FXML private TextField usernameField;
  @FXML private PasswordField passwordField;
  @FXML private Label errorLabel;

  public LoginController(AuthenticationService authenticationService) {
    this.authenticationService = Objects.requireNonNull(authenticationService);
  }

  @FXML
  private void handleLogin() {
    errorLabel.setText("");
    try {
      authenticationService.login(usernameField.getText(), passwordField.getText().toCharArray());
      NavigationService.showDashboard();
    } catch (AuthenticationException exception) {
      errorLabel.setText("Invalid username or password.");
    } finally {
      passwordField.clear();
    }
  }
}
