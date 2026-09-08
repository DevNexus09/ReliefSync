package com.reliefsync.ui;

import com.reliefsync.model.RequestRow;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.StockView;
import com.reliefsync.service.MasterDataService;
import com.reliefsync.service.ReportService;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

class DashboardPane extends ContentPane {

    private final ReportService reports = new ReportService();
    private final MasterDataService masterData = new MasterDataService();

    private final Label areasValue = statValue("#3b82f6");
    private final Label openValue = statValue("#d98f0b");
    private final Label lowStockValue = statValue("#d64545");
    private final Label deliveredValue = statValue("#1b9e4b");

    private final TableView<RequestRow> recentTable = new TableView<>();
    private final TableView<StockView> lowStockTable = new TableView<>();

    DashboardPane() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(4));
        box.getChildren().add(Ui.heading("Operations Overview"));

        HBox cards = new HBox(14,
                card(areasValue, "Active affected areas"),
                card(openValue, "Open requests"),
                card(lowStockValue, "Low-stock lines"),
                card(deliveredValue, "Deliveries completed"));
        box.getChildren().add(cards);

        recentTable.getColumns().addAll(List.of(
                Ui.col("#", RequestRow::id, 40),
                Ui.col("Area", RequestRow::areaName, 150),
                Ui.badgeCol("Priority", RequestRow::priority, 85),
                Ui.badgeCol("Status", RequestRow::status, 100)));
        recentTable.setPlaceholder(new Label("No requests yet"));
        Ui.fitColumns(recentTable);

        lowStockTable.getColumns().addAll(List.of(
                Ui.col("Center", StockView::centerName, 150),
                Ui.col("Resource", StockView::resourceName, 120),
                Ui.col("Qty", StockView::quantity, 55),
                Ui.badgeCol("Status", StockView::statusLabel, 100)));
        lowStockTable.setPlaceholder(new Label("Nothing at or below threshold"));
        Ui.fitColumns(lowStockTable);

        VBox recentBox = panel("Recent requests", recentTable);
        VBox stockBox = panel("Stock needing attention", lowStockTable);
        HBox.setHgrow(recentBox, Priority.ALWAYS);
        HBox.setHgrow(stockBox, Priority.ALWAYS);
        HBox tables = new HBox(14, recentBox, stockBox);
        VBox.setVgrow(tables, Priority.ALWAYS);
        box.getChildren().add(tables);

        Label db = new Label("Database: SQLite · data/reliefsync.db");
        db.getStyleClass().add("muted");
        box.getChildren().add(db);
        setCenter(box);
    }

    private static VBox panel(String title, TableView<?> table) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #46536b;");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox panel = new VBox(8, label, table);
        panel.setPadding(new Insets(14));
        panel.getStyleClass().add("card");
        return panel;
    }

    private static Label statValue(String color) {
        Label label = new Label("0");
        label.getStyleClass().add("stat-value");
        label.setStyle("-fx-text-fill: " + color + ";");
        return label;
    }

    private static VBox card(Label value, String caption) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("stat-caption");
        VBox card = new VBox(4, value, captionLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(215);
        card.getStyleClass().add("card");
        return card;
    }

    @Override
    void refresh() {
        areasValue.setText(String.valueOf(masterData.activeAreas().size()));
        openValue.setText(String.valueOf(reports.countWithStatus(
                RequestStatus.SUBMITTED, RequestStatus.VERIFIED,
                RequestStatus.ALLOCATED, RequestStatus.DISPATCHED)));
        lowStockValue.setText(String.valueOf(reports.lowStock().size()));
        deliveredValue.setText(String.valueOf(reports.countWithStatus(RequestStatus.DELIVERED)));

        List<RequestRow> recent = reports.searchRequests("", null);
        recentTable.setItems(FXCollections.observableArrayList(
                recent.subList(0, Math.min(8, recent.size()))));
        lowStockTable.setItems(FXCollections.observableArrayList(reports.lowStock()));
    }
}
