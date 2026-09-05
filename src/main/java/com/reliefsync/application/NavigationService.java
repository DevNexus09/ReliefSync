package com.reliefsync.application;

public final class NavigationService {
    private NavigationService() {
    }

    public static void showLogin() {
        SceneManager.showLogin();
    }

    public static void showDashboard() {
        SceneManager.showDashboard();
    }
}
