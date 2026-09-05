package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import javafx.fxml.FXML;

public class DashboardController {
    @FXML
    private void signOut() {
        NavigationService.showLogin();
    }
}
