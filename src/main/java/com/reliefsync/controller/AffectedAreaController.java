package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.model.AffectedArea;
import com.reliefsync.model.enums.*;
import com.reliefsync.model.search.AffectedAreaSearchCriteria;
import com.reliefsync.security.*;
import com.reliefsync.service.AffectedAreaService;
import com.reliefsync.util.*;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public final class AffectedAreaController {
  private final AffectedAreaService service;
  private final SessionManager sessions;
  private final AuthorizationService auth;
  @FXML private TableView<AffectedArea> table;
  @FXML private TextField searchField;
  @FXML private TextField eventFilter;
  @FXML private ComboBox<Severity> severityFilter;
  @FXML private ComboBox<Accessibility> accessibilityFilter;
  @FXML private TextField statusFilter;
  @FXML private HBox writeActions;

  public AffectedAreaController(AffectedAreaService s, SessionManager sm, AuthorizationService a) {
    service = s;
    sessions = sm;
    auth = a;
  }

  @FXML
  private void initialize() {
    TableSupport.column(table, "Area", 170, AffectedArea::name);
    TableSupport.column(table, "Event ID", 75, AffectedArea::disasterEventId);
    TableSupport.column(table, "District", 120, AffectedArea::district);
    TableSupport.column(table, "Population", 95, AffectedArea::populationAffected);
    TableSupport.column(table, "Families", 80, AffectedArea::familiesAffected);
    TableSupport.column(table, "Severity", 90, AffectedArea::severity);
    TableSupport.column(table, "Accessibility", 145, AffectedArea::accessibility);
    TableSupport.column(table, "Medical", 90, AffectedArea::medicalUrgency);
    TableSupport.column(table, "Water", 100, AffectedArea::waterAccess);
    TableSupport.column(table, "Status", 80, AffectedArea::status);
    severityFilter.setItems(FXCollections.observableArrayList(Severity.values()));
    accessibilityFilter.setItems(FXCollections.observableArrayList(Accessibility.values()));
    writeActions.setVisible(auth.can(session(), Permission.MANAGE_AFFECTED_AREAS));
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
                        new AffectedAreaSearchCriteria(
                            longValue(eventFilter.getText()),
                            searchField.getText(),
                            severityFilter.getValue(),
                            accessibilityFilter.getValue(),
                            blank(statusFilter.getText())),
                        session()))));
  }

  @FXML
  private void reset() {
    searchField.clear();
    eventFilter.clear();
    severityFilter.setValue(null);
    accessibilityFilter.setValue(null);
    statusFilter.clear();
    search();
  }

  @FXML
  private void create() {
    dialog(null);
  }

  @FXML
  private void edit() {
    AffectedArea a = selected();
    if (a != null) dialog(a);
  }

  private void dialog(AffectedArea a) {
    FormDialogHelper.show(
            a == null ? "New affected area" : "Edit affected area",
            List.of(
                f(
                    "event",
                    "Disaster event ID",
                    a == null ? "" : Long.toString(a.disasterEventId())),
                f("name", "Name", a == null ? "" : a.name()),
                f("district", "District", a == null ? "" : a.district()),
                f(
                    "latitude",
                    "Latitude (optional)",
                    a == null || a.latitude() == null ? "" : a.latitude().toString()),
                f(
                    "longitude",
                    "Longitude (optional)",
                    a == null || a.longitude() == null ? "" : a.longitude().toString()),
                f(
                    "population",
                    "Population affected",
                    a == null ? "0" : Long.toString(a.populationAffected())),
                f(
                    "families",
                    "Families affected",
                    a == null ? "0" : Long.toString(a.familiesAffected())),
                f("severity", "Severity", a == null ? "MEDIUM" : a.severity().name()),
                f(
                    "accessibility",
                    "Accessibility",
                    a == null ? "ACCESSIBLE" : a.accessibility().name()),
                f("medical", "Medical urgency", a == null ? "LOW" : a.medicalUrgency().name()),
                f("water", "Water access", a == null ? "LIMITED" : a.waterAccess()),
                f("status", "Status", a == null ? "ACTIVE" : a.status()),
                f("notes", "Notes", a == null ? "" : a.notes())))
        .ifPresent(
            v ->
                run(
                    () -> {
                      AffectedArea x =
                          new AffectedArea(
                              0,
                              Long.parseLong(v.get("event").trim()),
                              v.get("name"),
                              v.get("district"),
                              d(v.get("latitude")),
                              d(v.get("longitude")),
                              Long.parseLong(v.get("population").trim()),
                              Long.parseLong(v.get("families").trim()),
                              Severity.valueOf(v.get("severity").trim().toUpperCase()),
                              Accessibility.valueOf(v.get("accessibility").trim().toUpperCase()),
                              MedicalUrgency.valueOf(v.get("medical").trim().toUpperCase()),
                              v.get("water"),
                              v.get("status"),
                              v.get("notes"),
                              null,
                              null);
                      if (a == null) service.create(x, session());
                      else service.update(a.id(), x, session());
                      search();
                    }));
  }

  @FXML
  private void back() {
    NavigationService.showDashboard();
  }

  private AffectedArea selected() {
    AffectedArea a = table.getSelectionModel().getSelectedItem();
    if (a == null) UiAlertHelper.info("Select an affected area first.");
    return a;
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

  private static Double d(String s) {
    return s == null || s.isBlank() ? null : Double.valueOf(s.trim());
  }

  private static Long longValue(String value) {
    return value == null || value.isBlank() ? null : Long.valueOf(value.trim());
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static FormDialogHelper.Field f(String k, String l, String v) {
    return new FormDialogHelper.Field(k, l, v == null ? "" : v);
  }
}
