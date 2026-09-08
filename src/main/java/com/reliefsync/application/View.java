package com.reliefsync.application;

public enum View {
  LOGIN("/fxml/login-view.fxml", "ReliefSync | Sign in"),
  DASHBOARD("/fxml/dashboard-view.fxml", "ReliefSync | Dashboard"),
  DISASTER_EVENTS("/fxml/disaster-event-view.fxml", "ReliefSync | Disaster Events"),
  AFFECTED_AREAS("/fxml/affected-area-view.fxml", "ReliefSync | Affected Areas"),
  RELIEF_CENTERS("/fxml/relief-center-view.fxml", "ReliefSync | Relief Centers"),
  RESOURCES("/fxml/resource-view.fxml", "ReliefSync | Resources"),
  INVENTORY("/fxml/inventory-view.fxml", "ReliefSync | Inventory"),
  VEHICLES("/fxml/vehicle-view.fxml", "ReliefSync | Vehicles"),
  RELIEF_REQUESTS("/fxml/relief-request-view.fxml", "ReliefSync | Relief Requests"),
  VERIFICATION("/fxml/verification-view.fxml", "ReliefSync | Verification Queue");

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
