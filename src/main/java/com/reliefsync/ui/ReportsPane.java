package com.reliefsync.ui;

import com.reliefsync.model.AreaFulfillment;
import com.reliefsync.model.RequestRow;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.StatusCount;
import com.reliefsync.model.StockView;
import com.reliefsync.service.ReportService;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

class ReportsPane extends ContentPane {

    private final ReportService reports = new ReportService();

    private final TableView<StockView> lowStockTable = new TableView<>();
    private final TableView<StatusCount> statusTable = new TableView<>();
    private final TableView<AreaFulfillment> fulfillmentTable = new TableView<>();
    private final TableView<RequestRow> searchTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();

    ReportsPane() {
        lowStockTable.getColumns().addAll(List.of(
                Ui.col("Center", StockView::centerName, 220),
                Ui.col("Resource", StockView::resourceName, 180),
                Ui.col("Quantity", StockView::quantity, 90),
                Ui.col("Threshold", StockView::lowStockThreshold, 90),
                Ui.badgeCol("Status", StockView::statusLabel, 110)));
        lowStockTable.setPrefHeight(170);
        lowStockTable.setPlaceholder(new Label("No line is at or below its threshold"));
        Ui.fitColumns(lowStockTable);

        statusTable.getColumns().addAll(List.of(
                Ui.badgeCol("Status", StatusCount::status, 160),
                Ui.col("Requests", StatusCount::count, 100)));
        statusTable.setPrefHeight(170);
        statusTable.setPlaceholder(new Label("No requests yet"));
        Ui.fitColumns(statusTable);

        fulfillmentTable.getColumns().addAll(List.of(
                Ui.col("Area", AreaFulfillment::areaName, 220),
                Ui.col("Requested", AreaFulfillment::requested, 100),
                Ui.col("Allocated", AreaFulfillment::allocated, 100),
                Ui.col("Fulfillment", AreaFulfillment::percent, 100)));
        fulfillmentTable.setPrefHeight(170);
        fulfillmentTable.setPlaceholder(new Label("No requests with items yet"));
        Ui.fitColumns(fulfillmentTable);

        searchField.setPromptText("Search by area name");
        statusFilter.getItems().add("All statuses");
        for (RequestStatus status : RequestStatus.values()) {
            statusFilter.getItems().add(status.name());
        }
        statusFilter.setValue("All statuses");
        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> runSearch());
        searchField.setOnAction(e -> runSearch());
        HBox searchRow = new HBox(10, searchField, statusFilter, searchButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getStyleClass().add("toolbar");
        searchTable.getColumns().addAll(List.of(
                Ui.col("#", RequestRow::id, 50),
                Ui.col("Area", RequestRow::areaName, 200),
                Ui.badgeCol("Priority", RequestRow::priority, 90),
                Ui.badgeCol("Status", RequestRow::status, 110),
                Ui.col("Created", RequestRow::createdAt, 150),
                Ui.col("Created by", RequestRow::createdByName, 150)));
        searchTable.setPrefHeight(200);
        searchTable.setPlaceholder(new Label("No matching requests"));
        Ui.fitColumns(searchTable);
        VBox searchBox = new VBox(8, searchRow, searchTable);

        VBox box = new VBox(12, Ui.heading("Reports & Search"),
                Ui.subtitle("Review stock risk, request progress, and fulfillment performance."),
                new TitledPane("Low-stock report", lowStockTable),
                new TitledPane("Requests by status", statusTable),
                new TitledPane("Fulfillment by affected area", fulfillmentTable),
                new TitledPane("Request search", searchBox));
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        setCenter(scroll);
    }

    private void runSearch() {
        RequestStatus status = "All statuses".equals(statusFilter.getValue())
                ? null : RequestStatus.valueOf(statusFilter.getValue());
        searchTable.setItems(FXCollections.observableArrayList(
                reports.searchRequests(searchField.getText(), status)));
    }

    @Override
    void refresh() {
        lowStockTable.setItems(FXCollections.observableArrayList(reports.lowStock()));
        statusTable.setItems(FXCollections.observableArrayList(reports.statusSummary()));
        fulfillmentTable.setItems(FXCollections.observableArrayList(reports.areaFulfillment()));
        runSearch();
    }
}
