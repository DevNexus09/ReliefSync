package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.model.ReliefCenter;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.search.ReliefCenterSearchCriteria;
import com.reliefsync.security.*;
import com.reliefsync.service.ReliefCenterService;
import com.reliefsync.util.*;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public final class ReliefCenterController {
  private final ReliefCenterService service;
  private final SessionManager sessions;
  private final AuthorizationService auth;
  @FXML private TableView<ReliefCenter> table;
  @FXML private TextField searchField;
  @FXML private TextField districtFilter;
  @FXML private ComboBox<Boolean> activeFilter;
  @FXML private HBox writeActions;

  public ReliefCenterController(
      ReliefCenterService s, SessionManager sessions, AuthorizationService auth) {
    service = s;
    this.sessions = sessions;
    this.auth = auth;
  }

  @FXML
  private void initialize() {
    TableSupport.column(table, "Name", 200, ReliefCenter::name);
    TableSupport.column(table, "District", 150, ReliefCenter::district);
    TableSupport.column(table, "Latitude", 100, ReliefCenter::latitude);
    TableSupport.column(table, "Longitude", 100, ReliefCenter::longitude);
    TableSupport.column(table, "Contact", 190, ReliefCenter::contactInfo);
    TableSupport.column(table, "Active", 75, ReliefCenter::active);
    activeFilter.setItems(FXCollections.observableArrayList(Boolean.TRUE, Boolean.FALSE));
    writeActions.setVisible(auth.can(session(), Permission.MANAGE_RELIEF_CENTERS));
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
                        new ReliefCenterSearchCriteria(
                            searchField.getText(),
                            districtFilter.getText(),
                            activeFilter.getValue()),
                        session()))));
  }

  @FXML
  private void reset() {
    searchField.clear();
    districtFilter.clear();
    activeFilter.setValue(null);
    search();
  }

  @FXML
  private void create() {
    editDialog(null);
  }

  @FXML
  private void edit() {
    ReliefCenter c = selected();
    if (c != null) editDialog(c);
  }

  private void editDialog(ReliefCenter c) {
    FormDialogHelper.show(
            c == null ? "New relief center" : "Edit relief center",
            List.of(
                f("name", "Name", c == null ? "" : c.name()),
                f("district", "District", c == null ? "" : c.district()),
                f(
                    "latitude",
                    "Latitude (optional)",
                    c == null || c.latitude() == null ? "" : c.latitude().toString()),
                f(
                    "longitude",
                    "Longitude (optional)",
                    c == null || c.longitude() == null ? "" : c.longitude().toString()),
                f("contact", "Contact", c == null ? "" : c.contactInfo())))
        .ifPresent(
            v ->
                run(
                    () -> {
                      ReliefCenter x =
                          new ReliefCenter(
                              0,
                              v.get("name"),
                              v.get("district"),
                              number(v.get("latitude")),
                              number(v.get("longitude")),
                              v.get("contact"),
                              true,
                              null);
                      if (c == null) service.create(x, session());
                      else service.update(c.id(), x, session());
                      search();
                    }));
  }

  @FXML
  private void activate() {
    change(true);
  }

  @FXML
  private void deactivate() {
    change(false);
  }

  private void change(boolean active) {
    ReliefCenter c = selected();
    if (c != null)
      run(
          () -> {
            if (active) service.activate(c.id(), session());
            else service.deactivate(c.id(), session());
            search();
          });
  }

  @FXML
  private void back() {
    NavigationService.showDashboard();
  }

  private ReliefCenter selected() {
    ReliefCenter c = table.getSelectionModel().getSelectedItem();
    if (c == null) UiAlertHelper.info("Select a relief center first.");
    return c;
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

  private static Double number(String s) {
    return s == null || s.isBlank() ? null : Double.valueOf(s.trim());
  }

  private static FormDialogHelper.Field f(String k, String l, String v) {
    return new FormDialogHelper.Field(k, l, v == null ? "" : v);
  }
}
