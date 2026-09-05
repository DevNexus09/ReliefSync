package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import javafx.fxml.FXML;

public class LoginController {
    @FXML
    private void openDashboard() {
        NavigationService.showDashboard();
    }
}
