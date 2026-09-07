package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.application.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {
    @FXML
    private Label databaseStatusLabel;

    @FXML
    private void initialize() {
        boolean connected = SceneManager.isDatabaseConnected();
        databaseStatusLabel.setText(connected ? "Database: Connected" : "Database: Connection Failed");
        databaseStatusLabel.getStyleClass().add(connected ? "database-connected" : "database-failed");
    }

    @FXML
    private void signOut() {
        NavigationService.showLogin();
    }
}
