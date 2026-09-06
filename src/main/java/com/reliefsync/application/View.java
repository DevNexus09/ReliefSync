package com.reliefsync.application;

public enum View {
    LOGIN("/fxml/login-view.fxml", "ReliefSync | Sign in"),
    DASHBOARD("/fxml/dashboard-view.fxml", "ReliefSync | Dashboard");

    private final String fxmlPath;
    private final String windowTitle;

    View(String fxmlPath, String windowTitle) {
        this.fxmlPath = fxmlPath;
        this.windowTitle = windowTitle;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public String getWindowTitle() {
        return windowTitle;
    }
}
