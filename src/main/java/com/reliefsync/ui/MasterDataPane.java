package com.reliefsync.ui;

import com.reliefsync.facade.ReliefOperationFacade;
import com.reliefsync.model.AffectedArea;
import com.reliefsync.model.ReliefCenter;
import com.reliefsync.model.Resource;
import com.reliefsync.model.Vehicle;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.service.AccessControl;
import com.reliefsync.service.Feature;
import com.reliefsync.service.MasterDataService;
import com.reliefsync.service.Session;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * CRUD screens for master-data entities. Records are deactivated
 * rather than deleted so historical requests keep their references.
 */
class MasterDataPane extends ContentPane {

    private final MasterDataService service = new MasterDataService();
    private final ReliefOperationFacade facade = new ReliefOperationFacade();
    private final List<Runnable> loaders = new ArrayList<>();

    MasterDataPane() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        if (AccessControl.can(Session.user().role(), Feature.MASTER_DATA)) {
            tabs.getTabs().addAll(areaTab(), centerTab(), resourceTab());
        }
        if (AccessControl.can(Session.user().role(), Feature.VEHICLES)) {
            tabs.getTabs().add(vehicleTab());
        }
        VBox box = new VBox(8, Ui.heading("Master Data"), tabs);
        setCenter(box);
    }

    @Override
    void refresh() {
        loaders.forEach(Runnable::run);
    }

    // ---- Affected areas ----

    private Tab areaTab() {
        TableView<AffectedArea> table = new TableView<>();
        table.getColumns().addAll(List.of(
                Ui.col("Name", AffectedArea::name, 190),
                Ui.col("District", AffectedArea::district, 130),
                Ui.col("Population", AffectedArea::population, 100),
                Ui.col("Severity", AffectedArea::severity, 80),
                Ui.col("Active", a -> a.active() ? "Yes" : "No", 70)));

        TextField search = new TextField();
        search.setPromptText("Search name or district");
        Runnable load = () -> table.setItems(
                FXCollections.observableArrayList(service.searchAreas(search.getText())));
        loaders.add(load);
        search.textProperty().addListener((obs, o, n) -> load.run());

        TextField name = new TextField();
        name.setPromptText("Name");
        TextField district = new TextField();
        district.setPromptText("District");
        TextField population = new TextField();
        population.setPromptText("Population");
        population.setPrefWidth(90);
        TextField severity = new TextField();
        severity.setPromptText("Severity 1-5");
        severity.setPrefWidth(90);

        final Long[] selectedId = {null};
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, area) -> {
            if (area != null) {
                selectedId[0] = area.id();
                name.setText(area.name());
                district.setText(area.district());
                population.setText(String.valueOf(area.population()));
                severity.setText(String.valueOf(area.severity()));
            }
        });

        Button newButton = new Button("New");
        newButton.setOnAction(e -> {
            selectedId[0] = null;
            table.getSelectionModel().clearSelection();
            name.clear();
            district.clear();
            population.clear();
            severity.clear();
        });
        Button save = new Button("Save");
        save.setOnAction(e -> Ui.guarded(() -> {
            service.saveArea(selectedId[0], name.getText(), district.getText(),
                    Ui.intOf(population, "Population"), Ui.intOf(severity, "Severity"));
            load.run();
        }));
        Button deactivate = new Button("Deactivate");
        deactivate.setOnAction(e -> toggleActive(table.getSelectionModel().getSelectedItem() == null
                ? null : table.getSelectionModel().getSelectedItem().id(), false, load, "area"));
        Button activate = new Button("Activate");
        activate.setOnAction(e -> toggleActive(table.getSelectionModel().getSelectedItem() == null
                ? null : table.getSelectionModel().getSelectedItem().id(), true, load, "area"));

        return buildTab("Affected Areas", search, table,
                new HBox(8, name, district, population, severity),
                new HBox(8, newButton, save, deactivate, activate));
    }

    private void toggleActive(Long id, boolean active, Runnable load, String kind) {
        Ui.guarded(() -> {
            if (id == null) {
                throw new IllegalArgumentException("Select a " + kind + " in the table first");
            }
            switch (kind) {
                case "area" -> service.setAreaActive(id, active);
                case "center" -> service.setCenterActive(id, active);
                default -> service.setResourceActive(id, active);
            }
            load.run();
        });
    }

    // ---- Relief centers ----

    private Tab centerTab() {
        TableView<ReliefCenter> table = new TableView<>();
        table.getColumns().addAll(List.of(
                Ui.col("Name", ReliefCenter::name, 200),
                Ui.col("Location", ReliefCenter::location, 180),
                Ui.col("Capacity", ReliefCenter::capacity, 100),
                Ui.col("Active", c -> c.active() ? "Yes" : "No", 70)));

        TextField search = new TextField();
        search.setPromptText("Search name or location");
        Runnable load = () -> table.setItems(
                FXCollections.observableArrayList(service.searchCenters(search.getText())));
        loaders.add(load);
        search.textProperty().addListener((obs, o, n) -> load.run());

        TextField name = new TextField();
        name.setPromptText("Name");
        TextField location = new TextField();
        location.setPromptText("Location");
        TextField capacity = new TextField();
        capacity.setPromptText("Capacity");
        capacity.setPrefWidth(90);

        final Long[] selectedId = {null};
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, center) -> {
            if (center != null) {
                selectedId[0] = center.id();
                name.setText(center.name());
                location.setText(center.location());
                capacity.setText(String.valueOf(center.capacity()));
            }
        });

        Button newButton = new Button("New");
        newButton.setOnAction(e -> {
            selectedId[0] = null;
            table.getSelectionModel().clearSelection();
            name.clear();
            location.clear();
            capacity.clear();
        });
        Button save = new Button("Save");
        save.setOnAction(e -> Ui.guarded(() -> {
            service.saveCenter(selectedId[0], name.getText(), location.getText(),
                    Ui.intOf(capacity, "Capacity"));
            load.run();
        }));
        Button deactivate = new Button("Deactivate");
        deactivate.setOnAction(e -> toggleActive(table.getSelectionModel().getSelectedItem() == null
                ? null : table.getSelectionModel().getSelectedItem().id(), false, load, "center"));
        Button activate = new Button("Activate");
        activate.setOnAction(e -> toggleActive(table.getSelectionModel().getSelectedItem() == null
                ? null : table.getSelectionModel().getSelectedItem().id(), true, load, "center"));

        return buildTab("Relief Centers", search, table,
                new HBox(8, name, location, capacity),
                new HBox(8, newButton, save, deactivate, activate));
    }

    // ---- Resources ----

    private Tab resourceTab() {
        TableView<Resource> table = new TableView<>();
        table.getColumns().addAll(List.of(
                Ui.col("Name", Resource::name, 200),
                Ui.col("Unit", Resource::unit, 100),
                Ui.col("Low-stock threshold", Resource::lowStockThreshold, 150),
                Ui.col("Active", r -> r.active() ? "Yes" : "No", 70)));

        TextField search = new TextField();
        search.setPromptText("Search name");
        Runnable load = () -> table.setItems(
                FXCollections.observableArrayList(service.searchResources(search.getText())));
        loaders.add(load);
        search.textProperty().addListener((obs, o, n) -> load.run());

        TextField name = new TextField();
        name.setPromptText("Name");
        TextField unit = new TextField();
        unit.setPromptText("Unit (kg, litre…)");
        unit.setPrefWidth(120);
        TextField threshold = new TextField();
        threshold.setPromptText("Low-stock threshold");
        threshold.setPrefWidth(140);

        final Long[] selectedId = {null};
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, resource) -> {
            if (resource != null) {
                selectedId[0] = resource.id();
                name.setText(resource.name());
                unit.setText(resource.unit());
                threshold.setText(String.valueOf(resource.lowStockThreshold()));
            }
        });

        Button newButton = new Button("New");
        newButton.setOnAction(e -> {
            selectedId[0] = null;
            table.getSelectionModel().clearSelection();
            name.clear();
            unit.clear();
            threshold.clear();
        });
        Button save = new Button("Save");
        save.setOnAction(e -> Ui.guarded(() -> {
            service.saveResource(selectedId[0], name.getText(), unit.getText(),
                    Ui.intOf(threshold, "Low-stock threshold"));
            load.run();
        }));
        Button deactivate = new Button("Deactivate");
        deactivate.setOnAction(e -> toggleActive(table.getSelectionModel().getSelectedItem() == null
                ? null : table.getSelectionModel().getSelectedItem().id(), false, load, "resource"));
        Button activate = new Button("Activate");
        activate.setOnAction(e -> toggleActive(table.getSelectionModel().getSelectedItem() == null
                ? null : table.getSelectionModel().getSelectedItem().id(), true, load, "resource"));

        return buildTab("Resources", search, table,
                new HBox(8, name, unit, threshold),
                new HBox(8, newButton, save, deactivate, activate));
    }

    // ---- Vehicles ----

    private Tab vehicleTab() {
        TableView<Vehicle> table = new TableView<>();
        table.getColumns().addAll(List.of(
                Ui.col("Registration", Vehicle::registrationNumber, 170),
                Ui.col("Type", Vehicle::vehicleType, 150),
                Ui.col("Capacity units", Vehicle::capacity, 120),
                Ui.badgeCol("Status", Vehicle::status, 110)));

        TextField search = new TextField();
        search.setPromptText("Search registration or type");
        Runnable load = () -> table.setItems(FXCollections.observableArrayList(
                facade.vehicles(Session.user(), search.getText())));
        loaders.add(load);
        search.textProperty().addListener((obs, oldValue, newValue) -> load.run());

        TextField registration = new TextField();
        registration.setPromptText("Registration number");
        TextField type = new TextField();
        type.setPromptText("Vehicle type");
        TextField capacity = new TextField();
        capacity.setPromptText("Capacity units");
        capacity.setPrefWidth(120);

        final Long[] selectedId = {null};
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVehicle, vehicle) -> {
            if (vehicle != null) {
                selectedId[0] = vehicle.id();
                registration.setText(vehicle.registrationNumber());
                type.setText(vehicle.vehicleType());
                capacity.setText(String.valueOf(vehicle.capacity()));
            }
        });

        Button newButton = new Button("New");
        newButton.setOnAction(e -> {
            selectedId[0] = null;
            table.getSelectionModel().clearSelection();
            registration.clear();
            type.clear();
            capacity.clear();
        });
        Button save = Ui.primary(new Button("Save"));
        save.setOnAction(e -> Ui.guarded(() -> {
            facade.saveVehicle(Session.user(), selectedId[0], registration.getText(), type.getText(),
                    Ui.intOf(capacity, "Vehicle capacity"));
            load.run();
        }));
        Button available = new Button("Set available");
        available.setOnAction(e -> setVehicleStatus(table, VehicleStatus.AVAILABLE, load));
        Button maintenance = new Button("Set maintenance");
        maintenance.setOnAction(e -> setVehicleStatus(table, VehicleStatus.MAINTENANCE, load));
        Button deactivate = Ui.danger(new Button("Deactivate"));
        deactivate.setOnAction(e -> setVehicleStatus(table, VehicleStatus.INACTIVE, load));

        return buildTab("Vehicles", search, table,
                new HBox(8, registration, type, capacity),
                new HBox(8, newButton, save, available, maintenance, deactivate));
    }

    private void setVehicleStatus(TableView<Vehicle> table, VehicleStatus status, Runnable load) {
        Ui.guarded(() -> {
            Vehicle selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                throw new IllegalArgumentException("Select a vehicle in the table first");
            }
            facade.setVehicleStatus(Session.user(), selected.id(), status);
            load.run();
        });
    }

    private Tab buildTab(String title, TextField search, TableView<?> table, HBox form, HBox buttons) {
        form.setAlignment(Pos.CENTER_LEFT);
        buttons.setAlignment(Pos.CENTER_LEFT);
        VBox bottom = new VBox(8, new Label("Details"), form, buttons);
        bottom.setPadding(new Insets(10, 0, 0, 0));
        BorderPane pane = new BorderPane();
        pane.setTop(search);
        BorderPane.setMargin(search, new Insets(10, 0, 8, 0));
        pane.setCenter(table);
        pane.setBottom(bottom);
        pane.setPadding(new Insets(8));
        Tab tab = new Tab(title, pane);
        return tab;
    }
}
