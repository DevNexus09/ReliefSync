package com.reliefsync.ui;

import com.reliefsync.facade.ReliefOperationFacade;
import com.reliefsync.model.Notification;
import com.reliefsync.service.Session;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Persistent notifications belonging only to the current authenticated user. */
class NotificationsPane extends ContentPane {
    private final ReliefOperationFacade facade = new ReliefOperationFacade();
    private final TableView<Notification> table = new TableView<>();
    private final Runnable countChanged;

    NotificationsPane(Runnable countChanged) {
        this.countChanged = countChanged;
        table.getColumns().addAll(List.of(
                Ui.col("State", Notification::stateLabel, 70),
                Ui.col("Title", Notification::title, 155),
                Ui.col("Message", Notification::message, 430),
                Ui.col("Request", n -> n.requestId() == null ? "—" : "#" + n.requestId(), 80),
                Ui.col("Created", Notification::createdAt, 145)));
        table.setPlaceholder(new Label("You have no notifications yet"));
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Notification item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(!empty && item != null && item.unread()
                        ? "-fx-background-color: #eaf2ff; -fx-font-weight: bold;" : "");
            }
        });
        Ui.fitColumns(table);

        Button markOne = Ui.primary(new Button("Mark selected as read"));
        markOne.setOnAction(e -> Ui.guarded(() -> {
            Notification selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) throw new IllegalArgumentException("Select a notification first");
            facade.markNotificationRead(Session.user(), selected.id());
            refresh();
        }));
        Button markAll = new Button("Mark all as read");
        markAll.setOnAction(e -> Ui.guarded(() -> {
            facade.markAllNotificationsRead(Session.user());
            refresh();
        }));
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> Ui.guarded(this::refresh));
        HBox actions = new HBox(10, markOne, markAll, refresh);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(12, Ui.heading("Notifications"),
                new Label("Operational updates for your role and requests"), table, actions);
        box.setPadding(new Insets(4));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        setCenter(box);
    }

    @Override void refresh() {
        table.setItems(FXCollections.observableArrayList(facade.notifications(Session.user())));
        countChanged.run();
    }
}
