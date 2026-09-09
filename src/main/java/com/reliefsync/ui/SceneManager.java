package com.reliefsync.ui;

import java.util.Objects;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Switches between the login scene and the authenticated main scene. */
public final class SceneManager {

    private static Stage stage;

    private SceneManager() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void showLogin() {
        // Drop the main window's minimums first, or the login card floats in empty space.
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setScene(styled(new LoginPane(), 980, 720));
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    public static void showMain() {
        stage.setScene(styled(new MainPane(), 1180, 720));
        stage.setMinWidth(940);
        stage.setMinHeight(620);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    private static Scene styled(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(Objects.requireNonNull(
                SceneManager.class.getResource("/app.css"),
                "app.css is missing from the classpath").toExternalForm());
        return scene;
    }
}
