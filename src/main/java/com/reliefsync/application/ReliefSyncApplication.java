package com.reliefsync.application;

import com.reliefsync.database.DatabaseHealthCheck;
import com.reliefsync.database.DatabaseHealthCheck.HealthStatus;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.Stage;

public class ReliefSyncApplication extends Application {
    private static final Logger LOGGER = Logger.getLogger(ReliefSyncApplication.class.getName());

    @Override
    public void start(Stage stage) {
        HealthStatus databaseStatus = checkDatabaseConnection();
        SceneManager.initialize(stage, databaseStatus.connected());
        SceneManager.showLogin();
    }

    private HealthStatus checkDatabaseConnection() {
        HealthStatus status = new DatabaseHealthCheck().check();
        if (status.connected()) {
            LOGGER.info(() -> "SQLite connection successful. Version: " + status.sqliteVersion());
        } else {
            LOGGER.log(Level.SEVERE, "SQLite connection smoke test failed: {0}", status.detail());
        }
        return status;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
