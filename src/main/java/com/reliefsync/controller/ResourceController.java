package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.model.Resource;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.search.ResourceSearchCriteria;
import com.reliefsync.security.*;
import com.reliefsync.service.ResourceService;
import com.reliefsync.util.*;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public final class ResourceController {
  private final ResourceService service;
  private final SessionManager sessions;
  private final AuthorizationService auth;
  @FXML private TableView<Resource> table;
  @FXML private TextField searchField;
  @FXML private TextField categoryFilter;
  @FXML private ComboBox<Boolean> activeFilter;
  @FXML private HBox writeActions;

  public ResourceController(ResourceService s, SessionManager sm, AuthorizationService a) {
    service = s;
    sessions = sm;
    auth = a;
  }

  @FXML
  private void initialize() {
    TableSupport.column(table, "Name", 220, Resource::name);
    TableSupport.column(table, "Category", 170, Resource::category);
    TableSupport.column(table, "Unit", 140, Resource::unit);
    TableSupport.column(table, "Minimum Threshold", 160, Resource::minimumStockThreshold);
    TableSupport.column(table, "Active", 80, Resource::active);
    activeFilter.setItems(FXCollections.observableArrayList(Boolean.TRUE, Boolean.FALSE));
    writeActions.setVisible(auth.can(session(), Permission.MANAGE_RESOURCES));
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
                        new ResourceSearchCriteria(
                            searchField.getText(),
                            categoryFilter.getText(),
                            activeFilter.getValue()),
                        session()))));
  }

  @FXML
  private void reset() {
    searchField.clear();
    categoryFilter.clear();
    activeFilter.setValue(null);
    search();
  }

  @FXML
  private void create() {
    dialog(null);
  }

  @FXML
  private void edit() {
    Resource r = selected();
    if (r != null) dialog(r);
  }

  private void dialog(Resource r) {
    FormDialogHelper.show(
            r == null ? "New resource" : "Edit resource",
            List.of(
                f("name", "Name", r == null ? "" : r.name()),
                f("category", "Category", r == null ? "" : r.category()),
                f("unit", "Unit", r == null ? "" : r.unit()),
                f(
                    "threshold",
                    "Minimum stock threshold",
                    r == null ? "0" : Long.toString(r.minimumStockThreshold()))))
        .ifPresent(
            v ->
                run(
                    () -> {
                      Resource x =
                          new Resource(
                              0,
                              v.get("name"),
                              v.get("category"),
                              v.get("unit"),
                              Long.parseLong(v.get("threshold").trim()),
                              true);
                      if (r == null) service.create(x, session());
                      else service.update(r.id(), x, session());
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
    Resource r = selected();
    if (r != null)
      run(
          () -> {
            if (active) service.activate(r.id(), session());
            else service.deactivate(r.id(), session());
            search();
          });
  }

  @FXML
  private void back() {
    NavigationService.showDashboard();
  }

  private Resource selected() {
    Resource r = table.getSelectionModel().getSelectedItem();
    if (r == null) UiAlertHelper.info("Select a resource first.");
    return r;
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
}
