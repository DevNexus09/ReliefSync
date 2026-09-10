package com.reliefsync.ui;

import com.reliefsync.facade.ReliefOperationFacade;
import com.reliefsync.model.Allocation;
import com.reliefsync.model.DeliveryFailure;
import com.reliefsync.model.DeliveryRecoveryAction;
import com.reliefsync.model.DispatchManifest;
import com.reliefsync.model.DispatchManifestItem;
import com.reliefsync.model.PlannedAllocation;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.RequestRow;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.Shortage;
import com.reliefsync.model.Vehicle;
import com.reliefsync.service.AccessControl;
import com.reliefsync.service.AllocationResult;
import com.reliefsync.service.CancellationResult;
import com.reliefsync.service.Feature;
import com.reliefsync.service.ReallocationRecoveryResult;
import com.reliefsync.service.Session;
import com.reliefsync.strategy.AllocationStrategies;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
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
    private final TableView<Allocation> reservationTable = new TableView<>();
    private final ComboBox<String> strategyBox = new ComboBox<>();
    private final ComboBox<Vehicle> vehicleBox = new ComboBox<>();
    private final TextField driverField = new TextField();
    private final Label strategyInfo = new Label();
    private final Label itemInfo = new Label();
    private final Label transportInfo = new Label();
    private final Label planInfo = new Label();

    AllocationPane() {
        requestTable.getColumns().addAll(List.of(
                Ui.col("#", RequestRow::id, 50),
                Ui.col("Area", RequestRow::areaName, 200),
                Ui.badgeCol("Priority", RequestRow::priority, 90),
                Ui.badgeCol("Status", RequestRow::status, 110),
                Ui.col("Created", RequestRow::createdAt, 150)));
        requestTable.setPrefHeight(155);
        requestTable.setPlaceholder(new Label("No requests awaiting allocation, dispatch, or recovery"));
        Ui.fitColumns(requestTable);

        planTable.getColumns().addAll(List.of(
                Ui.col("Resource", PlannedAllocation::resourceName, 200),
                Ui.col("From center", PlannedAllocation::centerName, 220),
                Ui.col("Quantity", PlannedAllocation::quantity, 100)));
        planTable.setPrefHeight(135);
        planTable.setPlaceholder(new Label("Preview a plan to see the proposed distribution"));
        Ui.fitColumns(planTable);

        reservationTable.getColumns().addAll(List.of(
                Ui.col("Resource", Allocation::resourceName, 170),
                Ui.col("Center", Allocation::centerName, 190),
                Ui.col("Quantity", Allocation::quantity, 80),
                Ui.col("Strategy", Allocation::strategy, 170),
                Ui.col("Reservation", a -> a.active() ? "ACTIVE" : "RELEASED", 100)));
        reservationTable.setPrefHeight(120);
        reservationTable.setPlaceholder(new Label("No reservations recorded for this request"));
        Ui.fitColumns(reservationTable);
        itemInfo.setWrapText(true);
        transportInfo.setWrapText(true);
        vehicleBox.setPromptText("Select available vehicle");
        vehicleBox.setPrefWidth(310);
        driverField.setPromptText("Driver name");
        driverField.setPrefWidth(170);

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
        allocate.setOnAction(e -> withSelected(id -> {
            AllocationResult result = facade.allocate(Session.user(), id, strategyBox.getValue());
            showPlan(result, "Reserved");
            reloadRequests();
        }));

        Button reallocate = Ui.primary(new Button("Reallocate outstanding stock"));
        reallocate.setOnAction(e -> withSelected(id -> {
            AllocationResult result = facade.reallocate(Session.user(), id, strategyBox.getValue());
            showPlan(result, "Reallocated");
            reloadRequests();
        }));

        Button dispatch = new Button("Dispatch");
        dispatch.setOnAction(e -> withSelected(id -> {
            Vehicle vehicle = vehicleBox.getValue();
            if (vehicle == null) {
                throw new IllegalArgumentException("Select an available vehicle first");
            }
            facade.dispatch(Session.user(), id, vehicle.id(), driverField.getText());
            driverField.clear();
            reloadVehicles(canTransport);
            reloadRequests();
            Ui.info("Request #" + id + " dispatched using " + vehicle.registrationNumber() + ".");
        }));

        Button retry = Ui.primary(new Button("Retry delivery"));
        retry.setOnAction(e -> withSelected(id -> {
            Vehicle vehicle = vehicleBox.getValue();
            if (vehicle == null) {
                throw new IllegalArgumentException("Select an available vehicle first");
            }
            facade.retryDelivery(Session.user(), id, vehicle.id(), driverField.getText());
            driverField.clear();
            reloadVehicles(canTransport);
            reloadRequests();
            Ui.info("Delivery retry dispatched for request #" + id + " using "
                    + vehicle.registrationNumber() + ".");
        }));

        Button deliver = new Button("Confirm delivery");
        deliver.setOnAction(e -> withSelected(id -> {
            facade.deliver(Session.user(), id);
            reloadVehicles(canTransport);
            reloadRequests();
            Ui.info("Request #" + id + " delivered. Workflow complete.");
        }));

        Button reportFailure = Ui.danger(new Button("Report delivery failure"));
        reportFailure.setOnAction(e -> withSelected(id -> {
            FailureReport report = failureDialog();
            if (report == null) {
                return;
            }
            facade.reportDeliveryFailure(Session.user(), id, report.reason(),
                    report.action(), report.notes());
            reloadVehicles(canTransport);
            reloadRequests();
            Ui.info("Delivery failure recorded for request #" + id + ". Recovery: "
                    + report.action().label() + ".");
        }));

        Button returnForReallocation = new Button("Return for reallocation");
        returnForReallocation.setOnAction(e -> withSelected(id -> {
            if (!Ui.confirm("Return request #" + id + " for reallocation? Active reservations will be "
                    + "released and recoverable stock returned to inventory.")) {
                return;
            }
            ReallocationRecoveryResult result = facade.returnForReallocation(Session.user(), id, null);
            reloadRequests();
            Ui.info("Request #" + id + " returned for reallocation. Released "
                    + result.releasedQuantity() + " units across " + result.releasedAllocations()
                    + " reservations.");
        }));

        Runnable updateDispatch = () -> {
            RequestRow row = requestTable.getSelectionModel().getSelectedItem();
            Vehicle vehicle = vehicleBox.getValue();
            long load = activeLoad();
            boolean transportReady = canTransport && row != null
                    && vehicle != null && !driverField.getText().trim().isEmpty()
                    && load > 0 && load <= vehicle.capacity();
            DeliveryFailure failure = row == null ? null
                    : facade.latestDeliveryFailure(row.id()).orElse(null);
            dispatch.setDisable(!(transportReady && row.status() == RequestStatus.ALLOCATED));
            retry.setDisable(!(transportReady && row.status() == RequestStatus.DELIVERY_FAILED
                    && failure != null && !failure.resolved()
                    && failure.recoveryAction() == DeliveryRecoveryAction.RETRY));
            if (row != null && (row.status() == RequestStatus.ALLOCATED
                    || row.status() == RequestStatus.DELIVERY_FAILED) && vehicle != null
                    && load > vehicle.capacity()) {
                transportInfo.setText("Capacity warning: request load " + load
                        + " exceeds selected vehicle capacity " + vehicle.capacity() + ".");
                transportInfo.setStyle("-fx-text-fill: #b00020;");
            } else if (row != null) {
                showTransportDetails(row.id());
            }
        };
        vehicleBox.setOnAction(e -> updateDispatch.run());
        driverField.textProperty().addListener((observable, oldValue, newValue) -> updateDispatch.run());

        Button cancel = Ui.danger(new Button("Cancel and release stock"));
        cancel.setOnAction(e -> withSelected(id -> {
            if (!Ui.confirm("Cancel request #" + id + " and return all reserved stock to inventory?")) {
                return;
            }
            CancellationResult result = facade.cancel(Session.user(), id);
            reloadRequests();
            Ui.info("Request #" + id + " cancelled. Released " + result.releasedQuantity()
                    + " units across " + result.releasedAllocations() + " reservations.");
        }));

        requestTable.getSelectionModel().selectedItemProperty().addListener((observable, oldRow, row) -> {
            preview.setDisable(row == null || (row.status() != RequestStatus.VERIFIED
                    && row.status() != RequestStatus.ALLOCATED));
            allocate.setDisable(!canAllocate || row == null || row.status() != RequestStatus.VERIFIED);
            reallocate.setDisable(!canAllocate || row == null || row.status() != RequestStatus.ALLOCATED);
            deliver.setDisable(!canTransport || row == null || row.status() != RequestStatus.DISPATCHED);
            reportFailure.setDisable(!canTransport || row == null || row.status() != RequestStatus.DISPATCHED);
            DeliveryFailure failure = row == null ? null : facade.latestDeliveryFailure(row.id()).orElse(null);
            returnForReallocation.setDisable(!canAllocate || row == null
                    || row.status() != RequestStatus.DELIVERY_FAILED || failure == null || failure.resolved()
                    || failure.recoveryAction() != DeliveryRecoveryAction.REALLOCATE);
            cancel.setDisable(row == null || !facade.canCancel(Session.user(), row.id()));
            if (row == null) {
                itemInfo.setText("");
                transportInfo.setText("");
                reservationTable.getItems().clear();
            } else {
                showRequestDetails(row.id());
            }
            updateDispatch.run();
        });
        preview.setDisable(true);
        allocate.setDisable(true);
        reallocate.setDisable(true);
        dispatch.setDisable(true);
        retry.setDisable(true);
        deliver.setDisable(true);
        reportFailure.setDisable(true);
        returnForReallocation.setDisable(true);
        cancel.setDisable(true);

        HBox strategyRow = new HBox(10, new Label("Strategy:"), strategyBox, strategyInfo);
        strategyRow.setAlignment(Pos.CENTER_LEFT);
        strategyRow.getStyleClass().add("toolbar");
        HBox transportRow = new HBox(10, new Label("Vehicle:"), vehicleBox,
                new Label("Driver:"), driverField, dispatch, retry, deliver);
        transportRow.setAlignment(Pos.CENTER_LEFT);
        transportRow.getStyleClass().add("form-bar");
        HBox recoveryRow = new HBox(10, reportFailure, returnForReallocation);
        recoveryRow.setAlignment(Pos.CENTER_LEFT);
        recoveryRow.getStyleClass().add("action-bar");
        HBox actionRow = new HBox(10, preview, allocate, reallocate, cancel);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.getStyleClass().add("action-bar");

        VBox box = new VBox(12, Ui.heading("Allocation & Dispatch"),
                Ui.subtitle("Reserve stock, assign transport, and recover failed deliveries."),
                requestTable, itemInfo, new Label("Existing reservations:"), reservationTable,
                strategyRow, actionRow, new Label("Transport assignment:"), transportRow,
                recoveryRow, transportInfo, planTable, planInfo);
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scroll);
    }

    private void showRequestDetails(long requestId) {
        StringBuilder summary = new StringBuilder("Request items: ");
        for (RequestItem item : facade.items(requestId)) {
            summary.append(item.resourceName()).append(" — requested ")
                    .append(item.quantityRequested()).append(", allocated ")
                    .append(item.quantityAllocated()).append(", outstanding ")
                    .append(item.outstanding()).append(";  ");
        }
        itemInfo.setText(summary.toString());
        reservationTable.setItems(FXCollections.observableArrayList(facade.allocations(requestId)));
        showTransportDetails(requestId);
    }

    private long activeLoad() {
        return reservationTable.getItems().stream()
                .filter(Allocation::active)
                .mapToLong(Allocation::quantity)
                .sum();
    }

    private void showTransportDetails(long requestId) {
        DispatchManifest manifest = facade.manifest(requestId).orElse(null);
        if (manifest == null) {
            transportInfo.setText("Request load: " + activeLoad()
                    + " generic capacity units. Select an available vehicle and enter the driver name.");
            transportInfo.setStyle("-fx-text-fill: #666666;");
            return;
        }
        StringBuilder text = new StringBuilder("Latest attempt #")
                .append(manifest.attemptNumber()).append(" [").append(manifest.status()).append("]: ")
                .append(manifest.vehicleRegistration()).append(" — ")
                .append(manifest.vehicleType()).append(", driver ").append(manifest.driverName())
                .append(", load ").append(manifest.totalLoad()).append("/")
                .append(manifest.vehicleCapacity()).append(", dispatched ")
                .append(manifest.dispatchedAt());
        if (manifest.deliveredAt() != null) {
            text.append(", delivered ").append(manifest.deliveredAt());
        }
        if (manifest.failedAt() != null) {
            text.append(", failed ").append(manifest.failedAt());
        }
        text.append(". Pickups: ");
        for (DispatchManifestItem item : facade.manifestItems(manifest.id())) {
            text.append(item.centerName()).append(" — ").append(item.quantity()).append(" × ")
                    .append(item.resourceName()).append("; ");
        }
        DeliveryFailure failure = facade.latestDeliveryFailure(requestId).orElse(null);
        if (failure != null) {
            text.append(" Failure: ").append(failure.reason())
                    .append("; recovery ").append(failure.recoveryAction().label())
                    .append(failure.resolved() ? " (resolved)." : " (pending).");
        }
        transportInfo.setText(text.toString());
        transportInfo.setStyle("-fx-text-fill: #1f2937;");
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
                List.of(RequestStatus.VERIFIED, RequestStatus.ALLOCATED,
                        RequestStatus.DISPATCHED, RequestStatus.DELIVERY_FAILED))));
    }

    private static FailureReport failureDialog() {
        Dialog<FailureReport> dialog = new Dialog<>();
        dialog.setTitle("Report delivery failure");
        dialog.setHeaderText("Record what prevented this dispatch from being delivered");
        ButtonType record = new ButtonType("Record failure", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(record, ButtonType.CANCEL);

        TextArea reason = new TextArea();
        reason.setPromptText("Failure reason (required)");
        reason.setPrefRowCount(3);
        reason.setWrapText(true);
        ComboBox<DeliveryRecoveryAction> action = new ComboBox<>(
                FXCollections.observableArrayList(DeliveryRecoveryAction.values()));
        action.setValue(DeliveryRecoveryAction.RETRY);
        TextArea notes = new TextArea();
        notes.setPromptText("Optional recovery notes");
        notes.setPrefRowCount(2);
        notes.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Reason:"), reason);
        form.addRow(1, new Label("Recovery:"), action);
        form.addRow(2, new Label("Notes:"), notes);
        GridPane.setHgrow(reason, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(notes, javafx.scene.layout.Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(540);
        Node recordButton = dialog.getDialogPane().lookupButton(record);
        recordButton.disableProperty().bind(reason.textProperty().isEmpty());
        dialog.setResultConverter(button -> button == record
                ? new FailureReport(reason.getText(), action.getValue(), notes.getText()) : null);
        return dialog.showAndWait().orElse(null);
    }

    private record FailureReport(String reason, DeliveryRecoveryAction action, String notes) {
    }

    private void reloadVehicles(boolean canTransport) {
        vehicleBox.setItems(canTransport
                ? FXCollections.observableArrayList(facade.availableVehicles(Session.user()))
                : FXCollections.observableArrayList());
        vehicleBox.setValue(null);
    }

    @Override
    void refresh() {
        boolean canTransport = AccessControl.can(Session.user().role(), Feature.TRANSPORT);
        reloadVehicles(canTransport);
        reloadRequests();
        planTable.getItems().clear();
        reservationTable.getItems().clear();
        itemInfo.setText("");
        transportInfo.setText("");
        planInfo.setText("");
    }
}
