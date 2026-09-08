package com.reliefsync.controller;

import com.reliefsync.application.NavigationService;
import com.reliefsync.model.enums.VerificationDecision;
import com.reliefsync.model.verification.VerificationResult;
import com.reliefsync.security.*;
import com.reliefsync.service.VerificationService;
import com.reliefsync.service.dto.VerificationDetails;
import com.reliefsync.util.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class VerificationController {
  private final VerificationService service;
  private final SessionManager sessions;
  @FXML private TableView<VerificationDetails> table;
  @FXML private TextArea detailsArea;
  @FXML private TextArea reasonArea;

  public VerificationController(VerificationService service, SessionManager sessions) {
    this.service = service;
    this.sessions = sessions;
  }

  @FXML
  private void initialize() {
    TableSupport.column(table, "Request", 75, x -> x.request().id());
    TableSupport.column(table, "Area", 160, x -> x.area().name());
    TableSupport.column(table, "Priority", 90, x -> x.request().priority());
    TableSupport.column(table, "State", 145, x -> x.request().state());
    TableSupport.column(table, "Tier", 90, VerificationDetails::tier);
    TableSupport.column(table, "Round", 65, x -> x.request().verificationRound());
    TableSupport.column(table, "Next Level", 160, VerificationDetails::nextRequiredLevel);
    table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> show(b));
    refresh();
  }

  @FXML
  private void refresh() {
    run(() -> table.setItems(FXCollections.observableArrayList(service.pending(session()))));
  }

  private void show(VerificationDetails d) {
    if (d == null) {
      detailsArea.clear();
      return;
    }
    String items =
        d.items().stream()
            .map(
                i ->
                    "Resource #"
                        + i.resourceId()
                        + ": requested="
                        + i.requestedQuantity()
                        + ", allocated="
                        + i.allocatedQuantity()
                        + ", delivered="
                        + i.deliveredQuantity()
                        + ", remaining="
                        + i.getRemainingQuantity()
                        + ", fulfillment="
                        + String.format("%.1f%%", i.getFulfillmentPercentage()))
            .reduce((a, b) -> a + "\n" + b)
            .orElse("No items");
    detailsArea.setText(
        "Disaster #"
            + d.request().disasterEventId()
            + "\nArea: "
            + d.area().name()
            + "\nPopulation: "
            + d.area().populationAffected()
            + "\nSeverity: "
            + d.area().severity()
            + "\nMedical urgency: "
            + d.area().medicalUrgency()
            + "\nAccessibility: "
            + d.area().accessibility()
            + "\nPriority: "
            + d.request().priority()
            + "\nDescription: "
            + d.request().description()
            + "\nTier: "
            + d.tier()
            + "\nNext: "
            + d.nextRequiredLevel()
            + "\n\n"
            + items);
  }

  @FXML
  private void approve() {
    decide(VerificationDecision.APPROVED);
  }

  @FXML
  private void returnRequest() {
    decide(VerificationDecision.RETURNED);
  }

  @FXML
  private void reject() {
    decide(VerificationDecision.REJECTED);
  }

  private void decide(VerificationDecision decision) {
    VerificationDetails d = table.getSelectionModel().getSelectedItem();
    if (d == null) {
      UiAlertHelper.info("Select a pending request first.");
      return;
    }
    run(
        () -> {
          VerificationResult result =
              service.decide(d.request().id(), decision, reasonArea.getText(), session());
          reasonArea.clear();
          refresh();
          UiAlertHelper.info(message(result));
        });
  }

  @FXML
  private void history() {
    VerificationDetails d = table.getSelectionModel().getSelectedItem();
    if (d == null) return;
    String text =
        service.history(d.request().id(), session()).stream()
            .map(
                v ->
                    "Round "
                        + v.verificationRound()
                        + " · "
                        + v.level()
                        + " · "
                        + v.decision()
                        + " · "
                        + (v.reason() == null ? "" : v.reason()))
            .reduce((a, b) -> a + "\n" + b)
            .orElse("No history.");
    UiAlertHelper.info(text);
  }

  @FXML
  private void back() {
    NavigationService.showDashboard();
  }

  private String message(VerificationResult r) {
    return switch (r.outcome()) {
      case VERIFIED -> "Request verified successfully.";
      case RETURNED -> "Request returned to the requester for correction.";
      case REJECTED -> "Request rejected.";
      case STEP_APPROVED ->
          r.processedLevel()
              + " verification approved. Next required verifier: "
              + r.nextRequiredLevel()
              + ".";
    };
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
}
