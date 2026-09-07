package com.reliefsync.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class UiAlertHelper {
  private UiAlertHelper() {}

  public static void error(Throwable error) {
    show(
        Alert.AlertType.ERROR,
        "Operation failed",
        error.getMessage() == null ? "The operation could not be completed." : error.getMessage());
  }

  public static void info(String message) {
    show(Alert.AlertType.INFORMATION, "ReliefSync", message);
  }

  public static boolean confirm(String message) {
    Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
    a.setHeaderText(null);
    return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
  }

  private static void show(Alert.AlertType type, String title, String message) {
    Alert a = new Alert(type);
    a.setTitle(title);
    a.setHeaderText(null);
    a.setContentText(message);
    a.showAndWait();
  }
}
