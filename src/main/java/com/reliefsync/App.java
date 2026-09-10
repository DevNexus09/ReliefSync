package com.reliefsync;

import com.reliefsync.db.Database;
import com.reliefsync.db.Seeder;
import com.reliefsync.notification.NotificationBootstrap;
import com.reliefsync.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Database.initDefault();
        NotificationBootstrap.initialize();
        if (Seeder.seedRequested()) {
            Seeder.seedDemo();
        }
        SceneManager.init(stage);
        SceneManager.showLogin();
        stage.setTitle("ReliefSync — Disaster Relief Coordination");
        stage.show();
    }

    @Override
    public void stop() {
        NotificationBootstrap.reset();
        Database.reset();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
