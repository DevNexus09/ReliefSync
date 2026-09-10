package com.reliefsync.ui;

import com.reliefsync.model.ReliefCenter;
import com.reliefsync.model.Resource;
import com.reliefsync.model.StockView;
import com.reliefsync.service.InventoryService;
import com.reliefsync.service.MasterDataService;
import com.reliefsync.service.Session;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.List;

class InventoryPane extends ContentPane {

    private final InventoryService inventory = new InventoryService();
    private final MasterDataService masterData = new MasterDataService();

    private final ComboBox<ReliefCenter> centerBox = new ComboBox<>();
    private final ComboBox<Resource> resourceBox = new ComboBox<>();
    private final TableView<StockView> table = new TableView<>();
    private final TextField quantityField = new TextField();

    InventoryPane() {
        table.getColumns().addAll(List.of(
                Ui.col("Resource", StockView::resourceName, 220),
                Ui.col("Quantity", StockView::quantity, 100),
                Ui.col("Low-stock threshold", StockView::lowStockThreshold, 150),
                Ui.badgeCol("Status", StockView::statusLabel, 120)));
        table.setPlaceholder(new Label("No stock recorded for this center yet"));
        Ui.fitColumns(table);

        centerBox.setOnAction(e -> reloadStock());
        HBox top = new HBox(10, new Label("Relief center:"), centerBox);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("toolbar");

        quantityField.setPromptText("Amount");
        quantityField.setPrefWidth(90);
        Button setButton = new Button("Set exact");
        setButton.setOnAction(e -> Ui.guarded(() -> {
            inventory.setQuantity(Session.user(), selectedCenterId(), selectedResourceId(),
                    Ui.intOf(quantityField, "Amount"));
            reloadStock();
        }));
        Button receiveButton = new Button("Receive (+)");
        receiveButton.setOnAction(e -> Ui.guarded(() -> {
            inventory.adjust(Session.user(), selectedCenterId(), selectedResourceId(),
                    Ui.intOf(quantityField, "Amount"));
            reloadStock();
        }));
        Button issueButton = new Button("Issue (−)");
        issueButton.setOnAction(e -> Ui.guarded(() -> {
            inventory.adjust(Session.user(), selectedCenterId(), selectedResourceId(),
                    -Ui.intOf(quantityField, "Amount"));
            reloadStock();
        }));
        HBox form = new HBox(10, new Label("Resource:"), resourceBox, quantityField,
                setButton, receiveButton, issueButton);
        form.setAlignment(Pos.CENTER_LEFT);
        form.getStyleClass().add("form-bar");

        VBox box = new VBox(Ui.heading("Inventory"),
                Ui.subtitle("Monitor and adjust usable stock at each relief center."), top, table, form);
        box.setSpacing(12);
        setCenter(box);
    }

    private long selectedCenterId() {
        ReliefCenter center = centerBox.getValue();
        if (center == null) {
            throw new IllegalArgumentException("Select a relief center first");
        }
        return center.id();
    }

    private long selectedResourceId() {
        Resource resource = resourceBox.getValue();
        if (resource == null) {
            throw new IllegalArgumentException("Select a resource first");
        }
        return resource.id();
    }

    private void reloadStock() {
        if (centerBox.getValue() != null) {
            table.setItems(FXCollections.observableArrayList(
                    inventory.stockForCenter(centerBox.getValue().id())));
        }
    }

    @Override
    void refresh() {
        ReliefCenter selected = centerBox.getValue();
        centerBox.setItems(FXCollections.observableArrayList(masterData.activeCenters()));
        if (selected != null) {
            centerBox.getItems().stream()
                    .filter(c -> c.id() == selected.id())
                    .findFirst()
                    .ifPresent(centerBox::setValue);
        } else if (!centerBox.getItems().isEmpty()) {
            centerBox.setValue(centerBox.getItems().get(0));
        }
        resourceBox.setItems(FXCollections.observableArrayList(masterData.activeResources()));
        reloadStock();
    }
}
