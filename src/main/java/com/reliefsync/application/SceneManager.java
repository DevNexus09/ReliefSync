package com.reliefsync.application;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public final class SceneManager {
    private static final Logger LOGGER = Logger.getLogger(SceneManager.class.getName());
    private static final String STYLESHEET = "/css/application.css";
    private static final double WINDOW_WIDTH = 1100;
    private static final double WINDOW_HEIGHT = 700;

    private static Stage primaryStage;

    private SceneManager() {
    }

    public static void initialize(Stage stage) {
        primaryStage = Objects.requireNonNull(stage, "Primary stage must not be null.");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setTitle("ReliefSync");
    }

    public static void showLogin() {
        showScene("/fxml/login-view.fxml", "ReliefSync | Sign in");
    }

    public static void showDashboard() {
        showScene("/fxml/dashboard-view.fxml", "ReliefSync | Dashboard");
    }

    private static void showScene(String fxmlPath, String title) {
        if (primaryStage == null) {
            throw new IllegalStateException("SceneManager must be initialized before navigation.");
        }

        try {
            Parent root = loadView(fxmlPath);
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            URL stylesheet = requireResource(STYLESHEET);
            scene.getStylesheets().add(stylesheet.toExternalForm());

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Unable to open view " + fxmlPath, exception);
            showError("Navigation error", "ReliefSync could not open the requested screen.");
            throw exception;
        }
    }

    public static Parent loadView(String fxmlPath) {
        URL resource = requireResource(fxmlPath);
        try {
            return FXMLLoader.load(resource);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load FXML resource: " + fxmlPath, exception);
        }
    }

    private static URL requireResource(String path) {
        URL resource = SceneManager.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException("Required resource was not found: " + path);
        }
        return resource;
    }

    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
