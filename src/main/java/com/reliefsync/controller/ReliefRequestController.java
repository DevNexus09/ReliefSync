package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.model.*;
import com.reliefsync.model.enums.*;
import com.reliefsync.model.request.*;
import com.reliefsync.model.search.ReliefRequestSearchCriteria;
import com.reliefsync.security.*;
import com.reliefsync.service.*;
import com.reliefsync.service.dto.ReliefRequestDetails;
import com.reliefsync.util.*;
import java.util.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public final class ReliefRequestController {
  private final ReliefRequestService service;
  private final VerificationService verification;
  private final SessionManager sessions;
  private final AuthorizationService auth;
  @FXML private TableView<ReliefRequest> table;
  @FXML private TextField disasterFilter;
  @FXML private TextField areaFilter;
  @FXML private ComboBox<RequestPriority> priorityFilter;
  @FXML private ComboBox<RequestStateType> stateFilter;
  @FXML private HBox createActions;

  public ReliefRequestController(
      ReliefRequestService service,
      VerificationService verification,
      SessionManager sessions,
      AuthorizationService auth) {
    this.service = service;
    this.verification = verification;
    this.sessions = sessions;
    this.auth = auth;
  }

  @FXML
  private void initialize() {
    TableSupport.column(table, "ID", 55, ReliefRequest::id);
    TableSupport.column(table, "Disaster", 80, ReliefRequest::disasterEventId);
    TableSupport.column(table, "Area", 70, ReliefRequest::affectedAreaId);
    TableSupport.column(table, "Priority", 100, ReliefRequest::priority);
    TableSupport.column(table, "State", 155, ReliefRequest::state);
    TableSupport.column(table, "Requested By", 100, ReliefRequest::requestedBy);
    TableSupport.column(table, "Submitted", 155, ReliefRequest::submittedAt);
    TableSupport.column(table, "Round", 65, ReliefRequest::verificationRound);
    priorityFilter.setItems(FXCollections.observableArrayList(RequestPriority.values()));
    stateFilter.setItems(FXCollections.observableArrayList(RequestStateType.values()));
    createActions.setVisible(auth.can(session(), Permission.CREATE_RELIEF_REQUEST));
    createActions.setManaged(createActions.isVisible());
    search();
  }

  @FXML
  private void search() {
    run(
        () ->
            table.setItems(
                FXCollections.observableArrayList(
                    service.search(
                        new ReliefRequestSearchCriteria(
                            id(disasterFilter.getText()),
                            id(areaFilter.getText()),
                            priorityFilter.getValue(),
                            stateFilter.getValue(),
                            null),
                        session()))));
  }

  @FXML
  private void reset() {
    disasterFilter.clear();
    areaFilter.clear();
    priorityFilter.setValue(null);
    stateFilter.setValue(null);
    search();
  }

  @FXML
  private void create() {
    showForm(null);
  }

  @FXML
  private void edit() {
    ReliefRequest selected = selected();
    if (selected != null) showForm(service.details(selected.id(), session()));
  }

  private void showForm(ReliefRequestDetails current) {
    List<ReliefRequestItem> old = current == null ? List.of() : current.items();
    String itemText =
        old.stream()
            .map(i -> i.resourceId() + ":" + i.requestedQuantity())
            .reduce((a, b) -> a + "," + b)
            .orElse("");
    ReliefRequest r = current == null ? null : current.request();
    FormDialogHelper.show(
            r == null ? "New relief request" : "Edit relief request",
            List.of(
                f(
                    "disaster",
                    "Disaster event ID",
                    r == null ? "" : Long.toString(r.disasterEventId())),
                f("area", "Affected area ID", r == null ? "" : Long.toString(r.affectedAreaId())),
                f(
                    "priority",
                    "Priority (NORMAL/HIGH/CRITICAL)",
                    r == null ? "NORMAL" : r.priority().name()),
                f("description", "Description", r == null ? "" : r.description()),
                f("items", "Resources (resourceId:quantity, ...)", itemText)))
        .ifPresent(
            v ->
                run(
                    () -> {
                      ReliefRequestDraft draft =
                          new ReliefRequestDraft(
                              Long.parseLong(v.get("disaster").trim()),
                              Long.parseLong(v.get("area").trim()),
                              RequestPriority.valueOf(v.get("priority").trim().toUpperCase()),
                              v.get("description"),
                              parseItems(v.get("items")));
                      if (r != null) {
                        service.edit(r.id(), draft, session());
                        search();
                        return;
                      }
                      DuplicateCheckResult duplicates =
                          service.checkDuplicates(draft, null, session());
                      DuplicateResolution resolution = DuplicateResolution.CONTINUE_ANYWAY;
                      Long mergeId = null;
                      if (duplicates.probableDuplicate()) {
                        List<String> choices = new ArrayList<>();
                        choices.add("Continue anyway");
                        choices.add("Cancel");
                        duplicates.matches().stream()
                            .filter(DuplicateMatch::mergeable)
                            .forEach(m -> choices.add("Merge into request #" + m.requestId()));
                        ChoiceDialog<String> dialog = new ChoiceDialog<>("Cancel", choices);
                        dialog.setTitle("Possible duplicate request");
                        dialog.setHeaderText("Recent requests overlap by at least 50%.");
                        String choice = dialog.showAndWait().orElse("Cancel");
                        if (choice.equals("Cancel")) resolution = DuplicateResolution.CANCEL;
                        else if (choice.startsWith("Merge")) {
                          resolution = DuplicateResolution.MERGE;
                          mergeId = Long.valueOf(choice.substring(choice.indexOf('#') + 1));
                        }
                      }
                      service.submit(draft, resolution, mergeId, session());
                      search();
                    }));
  }

  @FXML
  private void details() {
    ReliefRequest r = selected();
    if (r == null) return;
    ReliefRequestDetails d = service.details(r.id(), session());
    String text =
        d.items().stream()
            .map(
                i ->
                    "Resource #"
                        + i.resourceId()
                        + ": requested "
                        + i.requestedQuantity()
                        + ", allocated "
                        + i.allocatedQuantity()
                        + ", delivered "
                        + i.deliveredQuantity()
                        + ", remaining "
                        + i.getRemainingQuantity()
                        + ", fulfillment "
                        + String.format("%.1f%%", i.getFulfillmentPercentage()))
            .reduce((a, b) -> a + "\n" + b)
            .orElse("No items");
    UiAlertHelper.info(text);
  }

  @FXML
  private void history() {
    ReliefRequest r = selected();
    if (r == null) return;
    String text =
        verification.history(r.id(), session()).stream()
            .map(
                v ->
                    "Round "
                        + v.verificationRound()
                        + " · "
                        + v.level()
                        + " · "
                        + v.decision()
                        + " · "
                        + (v.reason() == null ? "" : v.reason())
                        + " · "
                        + v.createdAt())
            .reduce((a, b) -> a + "\n" + b)
            .orElse("No verification history.");
    UiAlertHelper.info(text);
  }

  @FXML
  private void cancelRequest() {
    ReliefRequest r = selected();
    if (r != null && UiAlertHelper.confirm("Cancel this request?"))
      run(
          () -> {
            service.cancel(r.id(), session());
            search();
          });
  }

  @FXML
  private void resubmit() {
    ReliefRequest r = selected();
    if (r != null)
      run(
          () -> {
            service.resubmit(r.id(), session());
            search();
          });
  }

  @FXML
  private void back() {
    NavigationService.showDashboard();
  }

  private ReliefRequest selected() {
    ReliefRequest r = table.getSelectionModel().getSelectedItem();
    if (r == null) UiAlertHelper.info("Select a relief request first.");
    return r;
  }

  private UserSession session() {
    return sessions.requireCurrentSession();
  }

  private void run(Runnable work) {
    try {
      work.run();
    } catch (RuntimeException e) {
      UiAlertHelper.error(e);
    }
  }

  private static Long id(String s) {
    return s == null || s.isBlank() ? null : Long.valueOf(s.trim());
  }

  private static List<ReliefRequestDraftItem> parseItems(String text) {
    if (text == null || text.isBlank()) return List.of();
    List<ReliefRequestDraftItem> result = new ArrayList<>();
    for (String token : text.split(",")) {
      String[] parts = token.trim().split(":");
      if (parts.length != 2)
        throw new com.reliefsync.exception.ValidationException(
            "Use resourceId:quantity for every item.");
      result.add(
          new ReliefRequestDraftItem(
              Long.parseLong(parts[0].trim()), Long.parseLong(parts[1].trim())));
    }
    return result;
  }

  private static FormDialogHelper.Field f(String key, String label, String value) {
    return new FormDialogHelper.Field(key, label, value == null ? "" : value);
  }
}
