package com.reliefsync.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FxmlLoadingTest {
    @BeforeAll
    static void startJavaFxToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void stopJavaFxToolkit() {
        Platform.exit();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/fxml/login-view.fxml", "/fxml/dashboard-view.fxml"})
    void viewLoadsWithoutError(String resourcePath) throws Exception {
        URL resource = ReliefSyncApplication.class.getResource(resourcePath);
        assertNotNull(resource);

        FutureTask<Parent> loadTask = new FutureTask<>(() -> FXMLLoader.load(resource));
        Platform.runLater(loadTask);
        assertNotNull(assertDoesNotThrow(() -> loadTask.get()));
    }

    @org.junit.jupiter.api.Test
    void sceneManagerNavigatesUsingOneStageAndAppliesCss() throws Exception {
        FutureTask<Void> navigationTask = new FutureTask<>(() -> {
            Stage stage = new Stage();
            SceneManager.initialize(stage);

            SceneManager.showLogin();
            assertEquals("ReliefSync | Sign in", stage.getTitle());
            assertEquals(1, stage.getScene().getStylesheets().size());

            NavigationService.showDashboard();
            assertEquals("ReliefSync | Dashboard", stage.getTitle());
            assertEquals(1, stage.getScene().getStylesheets().size());

            NavigationService.showLogin();
            assertEquals("ReliefSync | Sign in", stage.getTitle());
            stage.close();
            return null;
        });

        Platform.runLater(navigationTask);
        assertDoesNotThrow(() -> navigationTask.get());
    }
}
