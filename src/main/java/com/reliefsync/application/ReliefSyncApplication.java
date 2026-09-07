package com.reliefsync.application;

import com.reliefsync.database.DatabaseHealthCheck.HealthStatus;
import com.reliefsync.exception.PersistenceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class ReliefSyncApplication extends Application {
  private static final Logger LOGGER = Logger.getLogger(ReliefSyncApplication.class.getName());

  @Override
  public void start(Stage stage) {
    ApplicationContext context = new ApplicationContext();
    try {
      context.initializeDatabase();
      HealthStatus databaseStatus = checkDatabaseConnection(context);
      if (!databaseStatus.connected()) {
        showFatalStartupError(databaseStatus.detail());
        return;
      }
      SceneManager.initialize(stage, true, context.controllerFactory());
      SceneManager.showLogin();
    } catch (PersistenceException exception) {
      LOGGER.log(Level.SEVERE, "Database initialization failed: {0}", exception.getMessage());
      showFatalStartupError(exception.getMessage());
    }
  }

  private HealthStatus checkDatabaseConnection(ApplicationContext context) {
    HealthStatus status = context.databaseHealth();
    if (status.connected()) {
      LOGGER.info(() -> "SQLite connection successful. Version: " + status.sqliteVersion());
    } else {
      LOGGER.log(Level.SEVERE, "SQLite connection smoke test failed: {0}", status.detail());
    }
    return status;
  }

  private void showFatalStartupError(String detail) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("ReliefSync startup error");
    alert.setHeaderText("The database could not be initialized.");
    alert.setContentText(detail == null ? "Please check the application log." : detail);
    alert.showAndWait();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
