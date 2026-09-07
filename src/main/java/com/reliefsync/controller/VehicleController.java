package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.model.Vehicle;
import com.reliefsync.model.enums.*;
import com.reliefsync.model.search.VehicleSearchCriteria;
import com.reliefsync.security.*;
import com.reliefsync.service.VehicleService;
import com.reliefsync.util.*;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public final class VehicleController {
  private final VehicleService service;
  private final SessionManager sessions;
  private final AuthorizationService auth;
  @FXML private TableView<Vehicle> table;
  @FXML private TextField searchField;
  @FXML private TextField typeFilter;
  @FXML private ComboBox<VehicleStatus> statusFilter;
  @FXML private TextField centerFilter;
  @FXML private ComboBox<Boolean> activeFilter;
  @FXML private HBox writeActions;

  public VehicleController(VehicleService s, SessionManager sm, AuthorizationService a) {
    service = s;
    sessions = sm;
    auth = a;
  }

  @FXML
  private void initialize() {
    TableSupport.column(table, "Registration", 180, Vehicle::registrationNo);
    TableSupport.column(table, "Type", 150, Vehicle::type);
    TableSupport.column(table, "Capacity", 100, Vehicle::capacity);
    TableSupport.column(table, "Status", 120, Vehicle::status);
    TableSupport.column(table, "Home Center ID", 130, Vehicle::reliefCenterId);
    TableSupport.column(table, "Active", 75, Vehicle::active);
    statusFilter.setItems(FXCollections.observableArrayList(VehicleStatus.values()));
    activeFilter.setItems(FXCollections.observableArrayList(Boolean.TRUE, Boolean.FALSE));
    writeActions.setVisible(auth.can(session(), Permission.MANAGE_VEHICLES));
    writeActions.setManaged(writeActions.isVisible());
    search();
  }

  @FXML
  private void search() {
    run(
        () ->
            table.setItems(
                FXCollections.observableArrayList(
                    service.search(
                        new VehicleSearchCriteria(
                            searchField.getText(),
                            typeFilter.getText(),
                            statusFilter.getValue(),
                            id(centerFilter.getText()),
                            activeFilter.getValue()),
                        session()))));
  }

  @FXML
  private void reset() {
    searchField.clear();
    typeFilter.clear();
    statusFilter.setValue(null);
    centerFilter.clear();
    activeFilter.setValue(null);
    search();
  }

  @FXML
  private void create() {
    dialog(null);
  }

  @FXML
  private void edit() {
    Vehicle v = selected();
    if (v != null) dialog(v);
  }

  private void dialog(Vehicle v) {
    FormDialogHelper.show(
            v == null ? "Register vehicle" : "Edit vehicle",
            List.of(
                f("registration", "Registration number", v == null ? "" : v.registrationNo()),
                f("type", "Type", v == null ? "" : v.type()),
                f("capacity", "Capacity", v == null ? "1" : Long.toString(v.capacity())),
                f(
                    "center",
                    "Home relief center ID (optional)",
                    v == null || v.reliefCenterId() == null ? "" : v.reliefCenterId().toString())))
        .ifPresent(
            m ->
                run(
                    () -> {
                      Long center =
                          m.get("center").isBlank() ? null : Long.valueOf(m.get("center").trim());
                      Vehicle x =
                          new Vehicle(
                              0,
                              m.get("registration"),
                              m.get("type"),
                              Long.parseLong(m.get("capacity").trim()),
                              VehicleStatus.AVAILABLE,
                              center,
                              true);
                      if (v == null) service.register(x, session());
                      else service.update(v.id(), x, session());
                      search();
                    }));
  }

  @FXML
  private void available() {
    status(VehicleStatus.AVAILABLE);
  }

  @FXML
  private void unavailable() {
    status(VehicleStatus.UNAVAILABLE);
  }

  private void status(VehicleStatus s) {
    Vehicle v = selected();
    if (v != null)
      run(
          () -> {
            service.changeManualAvailability(v.id(), s, session());
            search();
          });
  }

  @FXML
  private void activate() {
    Vehicle v = selected();
    if (v != null)
      run(
          () -> {
            service.activate(v.id(), session());
            search();
          });
  }

  @FXML
  private void deactivate() {
    Vehicle v = selected();
    if (v != null && UiAlertHelper.confirm("Deactivate selected vehicle?"))
      run(
          () -> {
            service.deactivate(v.id(), session());
            search();
          });
  }

  @FXML
  private void back() {
    NavigationService.showDashboard();
  }

  private Vehicle selected() {
    Vehicle v = table.getSelectionModel().getSelectedItem();
    if (v == null) UiAlertHelper.info("Select a vehicle first.");
    return v;
  }

  private UserSession session() {
    return sessions.requireCurrentSession();
  }

  private void run(Runnable r) {
    try {
      r.run();
    } catch (RuntimeException e) {
      UiAlertHelper.error(e);
    }
  }

  private static FormDialogHelper.Field f(String k, String l, String v) {
    return new FormDialogHelper.Field(k, l, v);
  }

  private static Long id(String value) {
    return value == null || value.isBlank() ? null : Long.valueOf(value.trim());
  }
}
