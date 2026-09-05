package com.reliefsync.application;

import com.reliefsync.database.DatabaseManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.Stage;

public class ReliefSyncApplication extends Application {
    private static final Logger LOGGER = Logger.getLogger(ReliefSyncApplication.class.getName());

    @Override
    public void start(Stage stage) {
        verifyDatabaseConnection();
        SceneManager.initialize(stage);
        SceneManager.showLogin();
    }

    private void verifyDatabaseConnection() {
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            if (!databaseManager.isConnectionValid()) {
                throw new SQLException("SQLite SELECT 1 smoke query did not return the expected result.");
            }
            String sqliteVersion = databaseManager.getSqliteVersion();
            LOGGER.info(() -> "SQLite connection successful. Version: " + sqliteVersion);
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "SQLite connection smoke test failed.", exception);
            throw new IllegalStateException("ReliefSync could not connect to its local database.", exception);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
