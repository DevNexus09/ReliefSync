package com.reliefsync.ui;

import com.reliefsync.facade.ReliefOperationFacade;
import com.reliefsync.model.AffectedArea;
import com.reliefsync.model.Allocation;
import com.reliefsync.model.AllocationEvent;
import com.reliefsync.model.DispatchManifest;
import com.reliefsync.model.DispatchManifestItem;
import com.reliefsync.model.DeliveryFailure;
import com.reliefsync.model.DraftItem;
import com.reliefsync.model.Priority;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.RequestRow;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.Resource;
import com.reliefsync.model.StatusChange;
import com.reliefsync.model.Verification;
import com.reliefsync.service.AccessControl;
import com.reliefsync.service.CancellationResult;
import com.reliefsync.service.Feature;
import com.reliefsync.service.MasterDataService;
import com.reliefsync.service.Session;
import com.reliefsync.verification.VerificationChains;
import com.reliefsync.verification.VerificationOutcome;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

class RequestsPane extends ContentPane {

    private final ReliefOperationFacade facade = new ReliefOperationFacade();
    private final MasterDataService masterData = new MasterDataService();

    private final ComboBox<AffectedArea> areaBox = new ComboBox<>();
    private final ComboBox<Priority> priorityBox = new ComboBox<>();
    private final ComboBox<Resource> resourceBox = new ComboBox<>();
    private final TextField noteField = new TextField();
    private final TextField quantityField = new TextField();
    private final ObservableList<DraftItem> draftItems = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final TableView<RequestRow> table = new TableView<>();

    RequestsPane() {
        VBox box = new VBox(12, Ui.heading("Relief Requests"),
                Ui.subtitle("Create, verify, and track requests through their operational lifecycle."));

        if (AccessControl.can(Session.user().role(), Feature.REQUESTS)) {
            box.getChildren().add(draftSection());
        }
        box.getChildren().add(listSection());
        setCenter(box);
    }

    // ---- Draft creation (multi-step workflow, step 1) ----

    private TitledPane draftSection() {
        priorityBox.setItems(FXCollections.observableArrayList(Priority.values()));
        priorityBox.setValue(Priority.NORMAL);
        noteField.setPromptText("Note (optional)");
        noteField.setPrefWidth(220);
        quantityField.setPromptText("Qty");
        quantityField.setPrefWidth(70);

        TableView<DraftItem> itemsTable = new TableView<>(draftItems);
        itemsTable.getColumns().addAll(List.of(
                Ui.col("Resource", DraftItem::resourceName, 240),
                Ui.col("Quantity", DraftItem::quantity, 100)));
        itemsTable.setPrefHeight(140);
        itemsTable.setPlaceholder(new Label("No items added yet"));

        Button addItem = new Button("Add item");
        addItem.setOnAction(e -> Ui.guarded(() -> {
            Resource resource = resourceBox.getValue();
            if (resource == null) {
                throw new IllegalArgumentException("Select a resource first");
            }
            int quantity = Ui.intOf(quantityField, "Quantity");
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
            if (draftItems.stream().anyMatch(i -> i.resourceId() == resource.id())) {
                throw new IllegalArgumentException(resource.name() + " is already in the draft");
            }
            draftItems.add(new DraftItem(resource.id(), resource.name(), quantity));
            quantityField.clear();
        }));
        Button removeItem = new Button("Remove selected");
        removeItem.setOnAction(e -> {
            DraftItem selected = itemsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                draftItems.remove(selected);
            }
        });

        Button createDraft = Ui.primary(new Button("Create draft"));
        createDraft.setOnAction(e -> Ui.guarded(() -> {
            AffectedArea area = areaBox.getValue();
            if (area == null) {
                throw new IllegalArgumentException("Select an affected area first");
            }
            if (facade.hasOpenRequestForArea(area.id())
                    && !Ui.confirm("\"" + area.name() + "\" already has an open relief request "
                    + "(probable duplicate).\nCreate another request anyway?")) {
                return;
            }
            long id = facade.createDraft(Session.user(), area.id(), priorityBox.getValue(),
                    noteField.getText(), new ArrayList<>(draftItems));
            draftItems.clear();
            noteField.clear();
            reloadTable();
            Ui.info("Draft request #" + id + " created. Select it below and press Submit "
                    + "to start verification.");
        }));

        HBox headerRow = new HBox(10, new Label("Area:"), areaBox, new Label("Priority:"), priorityBox, noteField);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getStyleClass().add("form-fields");
        HBox itemRow = new HBox(10, new Label("Item:"), resourceBox, quantityField, addItem, removeItem, createDraft);
        itemRow.setAlignment(Pos.CENTER_LEFT);
        itemRow.getStyleClass().add("action-bar");
        VBox content = new VBox(10, headerRow, itemRow, itemsTable);
        content.setPadding(new Insets(10));

        TitledPane pane = new TitledPane("New relief request (draft)", content);
        pane.setExpanded(true);
        return pane;
    }

    // ---- Request list and lifecycle actions ----

    private VBox listSection() {
        searchField.setPromptText("Search by area name");
        statusFilter.getItems().add("All statuses");
        for (RequestStatus status : RequestStatus.values()) {
            statusFilter.getItems().add(status.name());
        }
        statusFilter.setValue("All statuses");
        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> reloadTable());
        searchField.setOnAction(e -> reloadTable());
        HBox filterRow = new HBox(10, searchField, statusFilter, searchButton);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.getStyleClass().add("toolbar");

        table.getColumns().addAll(List.of(
                Ui.col("#", RequestRow::id, 50),
                Ui.col("Area", RequestRow::areaName, 200),
                Ui.badgeCol("Priority", RequestRow::priority, 90),
                Ui.badgeCol("Status", RequestRow::status, 110),
                Ui.col("Created", RequestRow::createdAt, 150),
                Ui.col("Created by", RequestRow::createdByName, 150),
                Ui.col("Note", RequestRow::note, 180)));
        table.setPrefHeight(260);
        table.setPlaceholder(new Label("No requests found"));
        Ui.fitColumns(table);

        List<Button> actions = new ArrayList<>();
        boolean canRequest = AccessControl.can(Session.user().role(), Feature.REQUESTS);
        boolean canVerify = AccessControl.can(Session.user().role(), Feature.VERIFY);

        if (canRequest) {
            Button submit = Ui.primary(new Button("Submit"));
            submit.setOnAction(e -> withSelected(id -> {
                facade.submit(Session.user(), id);
                Ui.info("Request #" + id + " submitted for verification.");
            }));
            Button cancel = new Button("Cancel request");
            cancel.setDisable(true);
            table.getSelectionModel().selectedItemProperty().addListener((observable, oldRow, row) ->
                    cancel.setDisable(row == null || !facade.canCancel(Session.user(), row.id())));
            cancel.setOnAction(e -> withSelected(id -> {
                RequestRow row = table.getSelectionModel().getSelectedItem();
                String message = row.status() == RequestStatus.ALLOCATED
                        ? "Cancel request #" + id + " and return all reserved stock to inventory?"
                        : "Cancel request #" + id + "?";
                if (Ui.confirm(message)) {
                    CancellationResult result = facade.cancel(Session.user(), id);
                    if (result.releasedAllocations() > 0) {
                        Ui.info("Request #" + id + " cancelled. Released " + result.releasedQuantity()
                                + " units across " + result.releasedAllocations() + " reservations.");
                    } else {
                        Ui.info("Request #" + id + " cancelled.");
                    }
                }
            }));
            actions.add(submit);
            actions.add(cancel);
        }
        if (canVerify) {
            Button approve = Ui.primary(new Button("Approve round"));
            approve.setOnAction(e -> decide(true));
            Button reject = Ui.danger(new Button("Reject"));
            reject.setOnAction(e -> decide(false));
            actions.add(approve);
            actions.add(reject);
        }
        Button details = new Button("Details");
        details.setOnAction(e -> withSelected(this::showDetails));
        actions.add(details);

        HBox actionRow = new HBox(10);
        actionRow.getChildren().addAll(actions);
        actionRow.getStyleClass().add("action-bar");

        return new VBox(8, filterRow, table, actionRow);
    }

    private void decide(boolean approve) {
        withSelected(id -> {
            RequestRow row = table.getSelectionModel().getSelectedItem();
            String header = (approve ? "Approve" : "Reject") + " request #" + id
                    + " (" + row.priority() + " — rounds: "
                    + VerificationChains.requiredRoles(row.priority()) + ")\nComment:";
            Ui.promptText(header).ifPresent(comment -> Ui.guarded(() -> {
                VerificationOutcome outcome = facade.verify(Session.user(), id, approve, comment);
                if (!outcome.approved()) {
                    Ui.info("Request #" + id + " was rejected.");
                } else if (outcome.chainComplete()) {
                    Ui.info("Request #" + id + " is fully VERIFIED and ready for allocation.");
                } else {
                    Ui.info("Round approved (" + outcome.roundRole().label()
                            + "). The request now awaits the next verification round.");
                }
                reloadTable();
            }));
        });
    }

    private void withSelected(java.util.function.LongConsumer action) {
        RequestRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            Ui.error("Select a request in the table first");
            return;
        }
        Ui.guarded(() -> {
            action.accept(row.id());
            reloadTable();
        });
    }

    private void showDetails(long requestId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Items:\n");
        for (RequestItem item : facade.items(requestId)) {
            sb.append("  - ").append(item.resourceName()).append(": ")
                    .append(item.quantityAllocated()).append(" allocated of ")
                    .append(item.quantityRequested()).append(" requested; ")
                    .append(item.outstanding()).append(" outstanding\n");
        }
        List<Verification> verifications = facade.verifications(requestId);
        if (!verifications.isEmpty()) {
            sb.append("\nVerification rounds:\n");
            for (Verification v : verifications) {
                sb.append("  - ").append(v.roundRole().label()).append(": ")
                        .append(v.approved() ? "APPROVED" : "REJECTED")
                        .append(" by ").append(v.verifierName())
                        .append(" at ").append(v.decidedAt());
                if (!v.comment().isEmpty()) {
                    sb.append(" — \"").append(v.comment()).append("\"");
                }
                sb.append("\n");
            }
        }
        List<Allocation> allocations = facade.allocations(requestId);
        if (!allocations.isEmpty()) {
            sb.append("\nAllocations:\n");
            for (Allocation a : allocations) {
                sb.append("  - ").append(a.quantity()).append(" × ").append(a.resourceName())
                        .append(" from ").append(a.centerName())
                        .append(" (").append(a.strategy()).append(") — ")
                        .append(a.active() ? "ACTIVE" : "RELEASED");
                if (!a.active()) {
                    sb.append(" by ").append(a.releasedByName()).append(" at ").append(a.releasedAt());
                }
                sb.append("\n");
            }
        }
        List<AllocationEvent> allocationEvents = facade.allocationEvents(requestId);
        if (!allocationEvents.isEmpty()) {
            sb.append("\nAllocation audit events:\n");
            for (AllocationEvent event : allocationEvents) {
                sb.append("  - ").append(event.eventType()).append(": ")
                        .append(event.quantity()).append(" × ").append(event.resourceName())
                        .append(" at ").append(event.centerName())
                        .append(" by ").append(event.actorName())
                        .append(" at ").append(event.occurredAt()).append("\n");
            }
        }
        List<DispatchManifest> attempts = facade.dispatchAttempts(requestId);
        if (!attempts.isEmpty()) {
            sb.append("\nDispatch attempts:\n");
            for (DispatchManifest manifest : attempts) {
                sb.append("  Attempt #").append(manifest.attemptNumber()).append(" — ")
                        .append(manifest.status()).append("\n")
                        .append("    Vehicle: ").append(manifest.vehicleRegistration())
                        .append(" (").append(manifest.vehicleType()).append(", capacity ")
                        .append(manifest.vehicleCapacity()).append(")\n")
                        .append("    Driver: ").append(manifest.driverName()).append("\n")
                        .append("    Load: ").append(manifest.totalLoad()).append(" generic capacity units\n")
                        .append("    Dispatched: ").append(manifest.dispatchedAt()).append("\n");
                if (manifest.failedAt() != null) {
                    sb.append("    Failed: ").append(manifest.failedAt()).append("\n");
                }
                if (manifest.deliveredAt() != null) {
                    sb.append("    Delivered: ").append(manifest.deliveredAt()).append("\n");
                }
                sb.append("    Pickup lines:\n");
                for (DispatchManifestItem item : facade.manifestItems(manifest.id())) {
                    sb.append("      ").append(item.centerName()).append(": ")
                            .append(item.quantity()).append(" × ").append(item.resourceName()).append("\n");
                }
            }
        }
        List<DeliveryFailure> failures = facade.deliveryFailures(requestId);
        if (!failures.isEmpty()) {
            sb.append("\nDelivery failure history:\n");
            for (DeliveryFailure failure : failures) {
                sb.append("  - Attempt #").append(failure.attemptNumber()).append(": ")
                        .append(failure.reason()).append("\n")
                        .append("    Reported by ").append(failure.reporterName())
                        .append(" at ").append(failure.reportedAt()).append("\n")
                        .append("    Recovery: ").append(failure.recoveryAction().label())
                        .append(failure.resolved() ? " — resolved " + failure.resolvedAt() : " — pending")
                        .append("\n");
                if (failure.recoveryNotes() != null) {
                    sb.append("    Notes: ").append(failure.recoveryNotes()).append("\n");
                }
            }
        }
        sb.append("\nStatus history:\n");
        for (StatusChange change : facade.history(requestId)) {
            sb.append("  - ").append(change.fromStatus()).append(" → ").append(change.toStatus())
                    .append(" by ").append(change.changedBy())
                    .append(" at ").append(change.changedAt()).append("\n");
        }

        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setPrefSize(560, 380);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ReliefSync");
        alert.setHeaderText("Request #" + requestId);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    private void reloadTable() {
        RequestStatus status = "All statuses".equals(statusFilter.getValue())
                ? null : RequestStatus.valueOf(statusFilter.getValue());
        table.setItems(FXCollections.observableArrayList(
                facade.requests(searchField.getText(), status)));
    }

    @Override
    void refresh() {
        areaBox.setItems(FXCollections.observableArrayList(masterData.activeAreas()));
        resourceBox.setItems(FXCollections.observableArrayList(masterData.activeResources()));
        reloadTable();
    }
}
