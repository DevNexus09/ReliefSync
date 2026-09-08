package com.reliefsync.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.database.DatabaseConfig;
import com.reliefsync.database.DatabaseManager;
import com.reliefsync.model.enums.Role;
import com.reliefsync.security.UserSession;
import java.nio.file.Files;
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
  private static ApplicationContext context;

  @BeforeAll
  static void startJavaFxToolkit() throws Exception {
    context =
        new ApplicationContext(
            new DatabaseManager(
                new DatabaseConfig(Files.createTempDirectory("reliefsync-fxml").resolve("ui.db"))));
    context.initializeDatabase();
    context.sessionManager().login(new UserSession(1, "Test User", "tester", Role.ADMINISTRATOR));
    CountDownLatch started = new CountDownLatch(1);
    Platform.startup(started::countDown);
    assertTrue(started.await(10, TimeUnit.SECONDS));
    Platform.setImplicitExit(false);
  }

  @AfterAll
  static void stopJavaFxToolkit() {
    Platform.exit();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "LOGIN",
        "DASHBOARD",
        "DISASTER_EVENTS",
        "AFFECTED_AREAS",
        "RELIEF_CENTERS",
        "RESOURCES",
        "INVENTORY",
        "VEHICLES",
        "RELIEF_REQUESTS",
        "VERIFICATION"
      })
  void givenARegisteredView_whenFxmlLoads_thenNoErrorOccurs(String viewName) throws Exception {
    View view = View.valueOf(viewName);
    assertNotNull(ReliefSyncApplication.class.getResource(view.getFxmlPath()));

    FutureTask<Parent> loadTask =
        new FutureTask<>(
            () -> {
              FXMLLoader loader =
                  new FXMLLoader(ReliefSyncApplication.class.getResource(view.getFxmlPath()));
              loader.setControllerFactory(context.controllerFactory());
              return loader.load();
            });
    Platform.runLater(loadTask);
    assertNotNull(assertDoesNotThrow(() -> loadTask.get()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/fxml/request-form-view.fxml", "/fxml/request-history-view.fxml"})
  void givenARequestDialogLayout_whenLoaded_thenItIsValid(String path) throws Exception {
    FutureTask<Parent> task =
        new FutureTask<>(() -> FXMLLoader.load(ReliefSyncApplication.class.getResource(path)));
    Platform.runLater(task);
    assertNotNull(assertDoesNotThrow(() -> task.get()));
  }

  @Test
  void givenOneStage_whenNavigating_thenTheStageAndStylesheetAreReused() throws Exception {
    FutureTask<Void> navigationTask =
        new FutureTask<>(
            () -> {
              Stage stage = new Stage();
              SceneManager.initialize(stage, true, context.controllerFactory());

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
