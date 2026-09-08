package com.reliefsync.ui;

import com.reliefsync.facade.ReliefOperationFacade;
import com.reliefsync.model.PlannedAllocation;
import com.reliefsync.model.RequestRow;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.Shortage;
import com.reliefsync.service.AccessControl;
import com.reliefsync.service.AllocationResult;
import com.reliefsync.service.Feature;
import com.reliefsync.service.Session;
import com.reliefsync.strategy.AllocationStrategies;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Second multi-step workflow: allocate reserved stock to a verified request
 * with a selectable Strategy, then dispatch and confirm delivery.
 */
class AllocationPane extends ContentPane {

    private final ReliefOperationFacade facade = new ReliefOperationFacade();

    private final TableView<RequestRow> requestTable = new TableView<>();
    private final TableView<PlannedAllocation> planTable = new TableView<>();
    private final ComboBox<String> strategyBox = new ComboBox<>();
    private final Label strategyInfo = new Label();
    private final Label planInfo = new Label();

    AllocationPane() {
        requestTable.getColumns().addAll(List.of(
                Ui.col("#", RequestRow::id, 50),
                Ui.col("Area", RequestRow::areaName, 200),
                Ui.badgeCol("Priority", RequestRow::priority, 90),
                Ui.badgeCol("Status", RequestRow::status, 110),
                Ui.col("Created", RequestRow::createdAt, 150)));
        requestTable.setPrefHeight(200);
        requestTable.setPlaceholder(new Label("No verified, allocated, or dispatched requests"));
        Ui.fitColumns(requestTable);

        planTable.getColumns().addAll(List.of(
                Ui.col("Resource", PlannedAllocation::resourceName, 200),
                Ui.col("From center", PlannedAllocation::centerName, 220),
                Ui.col("Quantity", PlannedAllocation::quantity, 100)));
        planTable.setPrefHeight(170);
        planTable.setPlaceholder(new Label("Preview a plan to see the proposed distribution"));
        Ui.fitColumns(planTable);

        strategyBox.setItems(FXCollections.observableArrayList(facade.strategyNames()));
        strategyBox.setValue(strategyBox.getItems().get(0));
        strategyBox.setOnAction(e -> updateStrategyInfo());
        updateStrategyInfo();

        Button preview = new Button("Preview plan");
        preview.setOnAction(e -> withSelected(id -> showPlan(
                facade.previewAllocation(id, strategyBox.getValue()), "Previewed")));

        boolean canAllocate = AccessControl.can(Session.user().role(), Feature.ALLOCATE);
        boolean canTransport = AccessControl.can(Session.user().role(), Feature.TRANSPORT);

        Button allocate = Ui.primary(new Button("Allocate & reserve stock"));
        allocate.setDisable(!canAllocate);
        allocate.setOnAction(e -> withSelected(id -> {
            AllocationResult result = facade.allocate(Session.user(), id, strategyBox.getValue());
            showPlan(result, "Reserved");
            reloadRequests();
        }));

        Button dispatch = new Button("Dispatch");
        dispatch.setDisable(!canTransport);
        dispatch.setOnAction(e -> withSelected(id -> {
            facade.dispatch(Session.user(), id);
            reloadRequests();
            Ui.info("Request #" + id + " dispatched.");
        }));

        Button deliver = new Button("Confirm delivery");
        deliver.setDisable(!canTransport);
        deliver.setOnAction(e -> withSelected(id -> {
            facade.deliver(Session.user(), id);
            reloadRequests();
            Ui.info("Request #" + id + " delivered. Workflow complete.");
        }));

        HBox strategyRow = new HBox(10, new Label("Strategy:"), strategyBox, strategyInfo);
        strategyRow.setAlignment(Pos.CENTER_LEFT);
        HBox actionRow = new HBox(10, preview, allocate, dispatch, deliver);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, Ui.heading("Allocation & Dispatch"),
                requestTable, strategyRow, actionRow, planTable, planInfo);
        setCenter(box);
    }

    private void updateStrategyInfo() {
        strategyInfo.setText(AllocationStrategies.byName(strategyBox.getValue()).description());
        strategyInfo.setStyle("-fx-text-fill: #666666;");
    }

    private void withSelected(java.util.function.LongConsumer action) {
        RequestRow row = requestTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            Ui.error("Select a request in the table first");
            return;
        }
        Ui.guarded(() -> action.accept(row.id()));
    }

    private void showPlan(AllocationResult result, String verb) {
        planTable.setItems(FXCollections.observableArrayList(result.lines()));
        if (result.fullyCovered()) {
            planInfo.setText(verb + ": every outstanding item is fully covered.");
            planInfo.setStyle("-fx-text-fill: #1b7f3a;");
        } else {
            StringBuilder sb = new StringBuilder(verb + " with shortages — ");
            for (Shortage shortage : result.shortages()) {
                sb.append(shortage.resourceName()).append(" short by ")
                        .append(shortage.missing()).append("; ");
            }
            planInfo.setText(sb.toString());
            planInfo.setStyle("-fx-text-fill: #b00020;");
        }
    }

    private void reloadRequests() {
        requestTable.setItems(FXCollections.observableArrayList(facade.requestsByStatuses(
                List.of(RequestStatus.VERIFIED, RequestStatus.ALLOCATED, RequestStatus.DISPATCHED))));
    }

    @Override
    void refresh() {
        reloadRequests();
        planTable.getItems().clear();
        planInfo.setText("");
    }
}
