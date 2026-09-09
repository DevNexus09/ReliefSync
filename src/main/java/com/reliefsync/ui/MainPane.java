package com.reliefsync.ui;

import com.reliefsync.model.User;
import com.reliefsync.service.AccessControl;
import com.reliefsync.service.Feature;
import com.reliefsync.service.Session;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

class MainPane extends BorderPane {

    private final StackPane content = new StackPane();
    private final List<Button> navButtons = new ArrayList<>();

    MainPane() {
        User user = Session.user();

        Label appName = new Label("ReliefSync");
        appName.getStyleClass().add("app-name");
        Label userLabel = new Label(user.fullName() + "  ·  " + user.username()
                + "  ·  " + user.role().label());
        Button logout = new Button("Log out");
        logout.getStyleClass().add("logout-button");
        logout.setOnAction(e -> {
            Session.logout();
            SceneManager.showLogin();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(12, appName, spacer, userLabel, logout);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 14, 10, 14));
        topBar.getStyleClass().add("top-bar");
        setTop(topBar);

        VBox nav = new VBox(6);
        nav.setPadding(new Insets(14, 10, 14, 10));
        nav.setPrefWidth(200);
        nav.getStyleClass().add("sidebar");

        DashboardPane dashboard = new DashboardPane();
        Button dashboardButton = null;
        if (AccessControl.can(user.role(), Feature.DASHBOARD)) {
            dashboardButton = addButton(nav, "Dashboard", dashboard);
        }
        if (AccessControl.can(user.role(), Feature.MASTER_DATA)
                || AccessControl.can(user.role(), Feature.VEHICLES)) {
            addButton(nav, "Master Data", new MasterDataPane());
        }
        if (AccessControl.can(user.role(), Feature.INVENTORY)) {
            addButton(nav, "Inventory", new InventoryPane());
        }
        if (AccessControl.can(user.role(), Feature.REQUESTS)
                || AccessControl.can(user.role(), Feature.VERIFY)) {
            addButton(nav, "Relief Requests", new RequestsPane());
        }
        if (AccessControl.can(user.role(), Feature.ALLOCATE)
                || AccessControl.can(user.role(), Feature.TRANSPORT)) {
            addButton(nav, "Allocation & Dispatch", new AllocationPane());
        }
        if (AccessControl.can(user.role(), Feature.REPORTS)) {
            addButton(nav, "Reports", new ReportsPane());
        }
        setLeft(nav);

        content.setPadding(new Insets(14));
        setCenter(content);
        if (dashboardButton != null) {
            activate(dashboardButton, dashboard);
        }
    }

    private Button addButton(VBox nav, String title, ContentPane pane) {
        Button button = new Button(title);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");
        button.setOnAction(e -> activate(button, pane));
        nav.getChildren().add(button);
        navButtons.add(button);
        return button;
    }

    private void activate(Button button, ContentPane pane) {
        for (Button other : navButtons) {
            other.getStyleClass().remove("active");
        }
        button.getStyleClass().add("active");
        content.getChildren().setAll(pane);
        Ui.guarded(pane::refresh);
    }
}
