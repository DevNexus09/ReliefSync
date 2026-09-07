package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.model.DisasterEvent;
import com.reliefsync.model.enums.*;
import com.reliefsync.model.search.DisasterEventSearchCriteria;
import com.reliefsync.security.*;
import com.reliefsync.service.DisasterEventService;
import com.reliefsync.util.*;
import java.time.LocalDate;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public final class DisasterEventController {
  private final DisasterEventService service;
  private final SessionManager sessions;
  private final AuthorizationService auth;
  @FXML private TableView<DisasterEvent> table;
  @FXML private TextField searchField;
  @FXML private ComboBox<DisasterType> typeFilter;
  @FXML private ComboBox<DisasterStatus> statusFilter;
  @FXML private DatePicker fromFilter;
  @FXML private DatePicker toFilter;
  @FXML private HBox writeActions;

  public DisasterEventController(
      DisasterEventService service, SessionManager sessions, AuthorizationService auth) {
    this.service = service;
    this.sessions = sessions;
    this.auth = auth;
  }

  @FXML
  private void initialize() {
    TableSupport.column(table, "ID", 55, DisasterEvent::id);
    TableSupport.column(table, "Name", 230, DisasterEvent::name);
    TableSupport.column(table, "Type", 120, DisasterEvent::type);
    TableSupport.column(table, "Start", 110, DisasterEvent::startDate);
    TableSupport.column(table, "End", 110, DisasterEvent::endDate);
    TableSupport.column(table, "Status", 100, DisasterEvent::status);
    typeFilter.setItems(FXCollections.observableArrayList(DisasterType.values()));
    statusFilter.setItems(FXCollections.observableArrayList(DisasterStatus.values()));
    writeActions.setVisible(auth.can(session(), Permission.MANAGE_DISASTERS));
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
                        new DisasterEventSearchCriteria(
                            searchField.getText(),
                            typeFilter.getValue(),
                            statusFilter.getValue(),
                            fromFilter.getValue(),
                            toFilter.getValue()),
                        session()))));
  }

  @FXML
  private void reset() {
    searchField.clear();
    typeFilter.setValue(null);
    statusFilter.setValue(null);
    fromFilter.setValue(null);
    toFilter.setValue(null);
    search();
  }

  @FXML
  private void create() {
    FormDialogHelper.show(
            "New disaster event",
            List.of(
                f("name", "Name", ""),
                f("type", "Type (FLOOD/CYCLONE/WATERLOGGING/EMERGENCY)", "FLOOD"),
                f("description", "Description", ""),
                f("start", "Start date (YYYY-MM-DD)", LocalDate.now().toString()),
                f("end", "End date (optional)", "")))
        .ifPresent(
            v ->
                run(
                    () -> {
                      service.create(
                          new DisasterEvent(
                              0,
                              v.get("name"),
                              DisasterType.valueOf(v.get("type").trim().toUpperCase()),
                              v.get("description"),
                              LocalDate.parse(v.get("start").trim()),
                              date(v.get("end")),
                              DisasterStatus.ACTIVE,
                              0,
                              null),
                          session());
                      search();
                    }));
  }

  @FXML
  private void edit() {
    DisasterEvent e = selected();
    if (e == null) return;
    FormDialogHelper.show(
            "Edit disaster event",
            List.of(
                f("name", "Name", e.name()),
                f("type", "Type", e.type().name()),
                f("description", "Description", e.description()),
                f("start", "Start date", e.startDate().toString()),
                f("end", "End date", e.endDate() == null ? "" : e.endDate().toString())))
        .ifPresent(
            v ->
                run(
                    () -> {
                      service.update(
                          e.id(),
                          new DisasterEvent(
                              e.id(),
                              v.get("name"),
                              DisasterType.valueOf(v.get("type").trim().toUpperCase()),
                              v.get("description"),
                              LocalDate.parse(v.get("start").trim()),
                              date(v.get("end")),
                              e.status(),
                              e.createdBy(),
                              e.createdAt()),
                          session());
                      search();
                    }));
  }

  @FXML
  private void close() {
    DisasterEvent e = selected();
    if (e != null && UiAlertHelper.confirm("Close selected disaster event?"))
      run(
          () -> {
            service.close(e.id(), session());
            search();
          });
  }

  @FXML
  private void reactivate() {
    DisasterEvent e = selected();
    if (e != null && UiAlertHelper.confirm("Reactivate selected disaster event?"))
      run(
          () -> {
            service.reactivate(e.id(), session());
            search();
          });
  }

  @FXML
  private void back() {
    NavigationService.showDashboard();
  }

  private DisasterEvent selected() {
    DisasterEvent e = table.getSelectionModel().getSelectedItem();
    if (e == null) UiAlertHelper.info("Select a disaster event first.");
    return e;
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

  private static LocalDate date(String s) {
    return s == null || s.isBlank() ? null : LocalDate.parse(s.trim());
  }

  private static FormDialogHelper.Field f(String k, String l, String v) {
    return new FormDialogHelper.Field(k, l, v);
  }
}
