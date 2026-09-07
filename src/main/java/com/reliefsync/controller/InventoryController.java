package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.search.InventorySearchCriteria;
import com.reliefsync.security.*;
import com.reliefsync.service.InventoryService;
import com.reliefsync.service.dto.*;
import com.reliefsync.util.*;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public final class InventoryController {
  private final InventoryService service;
  private final SessionManager sessions;
  private final AuthorizationService auth;
  @FXML private TableView<InventoryOverview> table;
  @FXML private TextField searchField;
  @FXML private TextField centerFilter;
  @FXML private TextField resourceFilter;
  @FXML private HBox writeActions;

  public InventoryController(InventoryService s, SessionManager sm, AuthorizationService a) {
    service = s;
    sessions = sm;
    auth = a;
  }

  @FXML
  private void initialize() {
    TableSupport.column(table, "Center", 175, InventoryOverview::centerName);
    TableSupport.column(table, "Resource", 175, InventoryOverview::resourceName);
    TableSupport.column(table, "Category", 120, InventoryOverview::category);
    TableSupport.column(table, "Total", 75, InventoryOverview::total);
    TableSupport.column(table, "Reserved", 80, InventoryOverview::reserved);
    TableSupport.column(table, "Dispatched", 90, InventoryOverview::dispatched);
    TableSupport.column(table, "Available", 85, InventoryOverview::available);
    TableSupport.column(table, "Threshold", 85, InventoryOverview::threshold);
    TableSupport.column(table, "Status", 90, x -> x.lowStock() ? "LOW STOCK" : "NORMAL");
    writeActions.setVisible(auth.can(session(), Permission.MANAGE_INVENTORY));
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
                        new InventorySearchCriteria(
                            id(centerFilter.getText()),
                            id(resourceFilter.getText()),
                            searchField.getText(),
                            false),
                        session()))));
  }

  @FXML
  private void lowStock() {
    run(
        () ->
            table.setItems(
                FXCollections.observableArrayList(
                    service.search(
                        new InventorySearchCriteria(
                            id(centerFilter.getText()),
                            id(resourceFilter.getText()),
                            searchField.getText(),
                            true),
                        session()))));
  }

  @FXML
  private void reset() {
    searchField.clear();
    centerFilter.clear();
    resourceFilter.clear();
    search();
  }

  @FXML
  private void initializeStock() {
    FormDialogHelper.show(
            "Initialize stock",
            List.of(
                f("center", "Relief center ID", ""),
                f("resource", "Resource ID", ""),
                f("quantity", "Initial quantity", "0"),
                f("reason", "Reason", "Opening stock")))
        .ifPresent(
            v ->
                run(
                    () -> {
                      service.initializeStock(
                          Long.parseLong(v.get("center").trim()),
                          Long.parseLong(v.get("resource").trim()),
                          Long.parseLong(v.get("quantity").trim()),
                          v.get("reason"),
                          session());
                      search();
                    }));
  }

  @FXML
  private void adjust() {
    InventoryOverview i = table.getSelectionModel().getSelectedItem();
    if (i == null) {
      UiAlertHelper.info("Select an inventory row first.");
      return;
    }
    FormDialogHelper.show(
            "Adjust stock",
            List.of(
                f("current", "Current total", Long.toString(i.total())),
                f("delta", "Delta (+/-)", "0"),
                f("reason", "Reason", "")))
        .ifPresent(
            v ->
                run(
                    () -> {
                      InventoryAdjustmentResult result =
                          service.adjustStock(
                              i.centerId(),
                              i.resourceId(),
                              Long.parseLong(v.get("delta").trim()),
                              v.get("reason"),
                              session());
                      search();
                      if (result.lowStock())
                        UiAlertHelper.info(
                            "Warning: available stock is at or below the minimum threshold ("
                                + result.threshold()
                                + ").");
                    }));
  }

  @FXML
  private void back() {
    NavigationService.showDashboard();
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
