package com.reliefsync.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
    @ValueSource(strings = {"LOGIN", "DASHBOARD"})
    void givenARegisteredView_whenFxmlLoads_thenNoErrorOccurs(String viewName) throws Exception {
        View view = View.valueOf(viewName);
        assertNotNull(ReliefSyncApplication.class.getResource(view.getFxmlPath()));

        FutureTask<Parent> loadTask = new FutureTask<>(() -> FXMLLoader.load(
                ReliefSyncApplication.class.getResource(view.getFxmlPath())));
        Platform.runLater(loadTask);
        assertNotNull(assertDoesNotThrow(() -> loadTask.get()));
    }

    @Test
    void givenOneStage_whenNavigating_thenTheStageAndStylesheetAreReused() throws Exception {
        FutureTask<Void> navigationTask = new FutureTask<>(() -> {
            Stage stage = new Stage();
            SceneManager.initialize(stage, true);

            SceneManager.showLogin();
            assertEquals("ReliefSync | Sign in", stage.getTitle());
            assertEquals(1, stage.getScene().getStylesheets().size());
            assertTrue(SceneManager.isDatabaseConnected());

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
