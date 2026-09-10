package com.reliefsync.ui;

import com.reliefsync.model.User;
import com.reliefsync.facade.ReliefOperationFacade;
import com.reliefsync.service.AccessControl;
import com.reliefsync.service.Feature;
import com.reliefsync.service.Session;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;

class MainPane extends BorderPane {

    private final StackPane content = new StackPane();
    private final List<Button> navButtons = new ArrayList<>();
    private final ReliefOperationFacade facade = new ReliefOperationFacade();
    private final Label unreadBadge = new Label();
    private final Button notificationBell = new Button();
    private final Popup notificationPopup = new Popup();
    private final NotificationsPane notificationsPane =
            new NotificationsPane(this::refreshNotificationCount);

    MainPane() {
        User user = Session.user();

        Label appName = new Label("ReliefSync");
        appName.getStyleClass().add("app-name");
        Label roleLabel = new Label(user.role().label());
        roleLabel.getStyleClass().add("user-pill");
        Button logout = new Button("Log out");
        logout.getStyleClass().add("logout-button");
        logout.setOnAction(e -> {
            notificationPopup.hide();
            Session.logout();
            SceneManager.showLogin();
        });

        notificationBell.getStyleClass().add("notification-bell");
        SVGPath bellGlyph = new SVGPath();
        bellGlyph.setContent("M 12 22 C 13.1 22 14 21.1 14 20 L 10 20 "
                + "C 10 21.1 10.9 22 12 22 Z M 18 8 C 18 4.7 15.3 2 12 2 "
                + "C 8.7 2 6 4.7 6 8 C 6 14 3 16 3 17 L 21 17 C 21 16 18 14 18 8 Z");
        bellGlyph.getStyleClass().add("notification-bell-glyph");
        notificationBell.setGraphic(bellGlyph);
        notificationBell.setAccessibleText("Open notifications");
        notificationBell.setTooltip(new Tooltip("Notifications"));
        notificationBell.setOnAction(e -> toggleNotifications());
        unreadBadge.getStyleClass().add("notification-count");
        unreadBadge.setMouseTransparent(true);
        StackPane bellWithBadge = new StackPane(notificationBell, unreadBadge);
        bellWithBadge.getStyleClass().add("notification-bell-wrap");
        StackPane.setAlignment(unreadBadge, Pos.TOP_RIGHT);

        HBox accountActions = new HBox(10, roleLabel, bellWithBadge, logout);
        accountActions.setAlignment(Pos.CENTER_RIGHT);

        StackPane popupCard = new StackPane(notificationsPane);
        popupCard.getStyleClass().add("notification-popup");
        popupCard.getStylesheets().add(MainPane.class.getResource("/app.css").toExternalForm());
        popupCard.setPrefSize(820, 480);
        popupCard.setMinSize(680, 400);
        notificationPopup.getContent().add(popupCard);
        notificationPopup.setAutoHide(true);
        notificationPopup.setHideOnEscape(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(12, appName, spacer, accountActions);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 18, 12, 18));
        topBar.getStyleClass().add("top-bar");
        setTop(topBar);

        VBox nav = new VBox(6);
        nav.setPadding(new Insets(18, 12, 18, 12));
        nav.setPrefWidth(218);
        nav.getStyleClass().add("sidebar");

        Label navCaption = new Label("WORKSPACE");
        navCaption.getStyleClass().add("sidebar-caption");
        nav.getChildren().add(navCaption);

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
        refreshNotificationCount();
        addEventFilter(MouseEvent.MOUSE_RELEASED, e -> Platform.runLater(() -> {
            if (getScene() != null) refreshNotificationCount();
        }));
        setLeft(nav);

        content.setPadding(new Insets(20));
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
        refreshNotificationCount();
    }

    private void toggleNotifications() {
        if (notificationPopup.isShowing()) {
            notificationPopup.hide();
            return;
        }

        Ui.guarded(notificationsPane::refresh);
        Bounds mainBounds = localToScreen(getBoundsInLocal());
        if (mainBounds == null || getScene() == null || getScene().getWindow() == null) {
            return;
        }

        double popupWidth = notificationPopup.getContent().getFirst().prefWidth(-1);
        double popupHeight = notificationPopup.getContent().getFirst().prefHeight(-1);
        notificationPopup.show(notificationBell,
                mainBounds.getMinX() + (mainBounds.getWidth() - popupWidth) / 2,
                mainBounds.getMinY() + (mainBounds.getHeight() - popupHeight) / 2);
    }

    private void refreshNotificationCount() {
        int unread = facade.unreadNotificationCount(Session.user());
        unreadBadge.setText(unread > 99 ? "99+" : Integer.toString(unread));
        unreadBadge.setVisible(unread > 0);
        unreadBadge.setManaged(unread > 0);
        notificationBell.setAccessibleText(unread == 0
                ? "Open notifications"
                : "Open notifications, " + unread + " unread");
    }
}
