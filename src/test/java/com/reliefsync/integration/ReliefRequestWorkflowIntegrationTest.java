package com.reliefsync.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefsync.application.ApplicationContext;
import com.reliefsync.database.*;
import com.reliefsync.exception.*;
import com.reliefsync.model.*;
import com.reliefsync.model.enums.*;
import com.reliefsync.model.request.*;
import com.reliefsync.model.verification.*;
import com.reliefsync.repository.sqlite.*;
import com.reliefsync.security.*;
import com.reliefsync.service.*;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ReliefRequestWorkflowIntegrationTest {
  @TempDir Path directory;
  DatabaseManager database;
  ApplicationContext context;
  UserSession admin, areaCoordinator, volunteer, reliefCoordinator;
  long eventId, areaId, resource1, resource2;

  @BeforeEach
  void setup() {
    database = new DatabaseManager(new DatabaseConfig(directory.resolve("workflow.db")));
    new MigrationRunner(database).runMigrations();
    new DatabaseSeeder(new TransactionManager(database), new PasswordHasher()).seedDemoData();
    context = new ApplicationContext(database);
    SQLiteUserRepository users = new SQLiteUserRepository(database);
    admin = session(users, DatabaseSeeder.DEMO_USERNAME);
    areaCoordinator = session(users, DatabaseSeeder.AREA_COORDINATOR_USERNAME);
    volunteer = session(users, DatabaseSeeder.VOLUNTEER_USERNAME);
    reliefCoordinator = session(users, DatabaseSeeder.RELIEF_COORDINATOR_USERNAME);
    DisasterEvent event =
        context
            .disasterEventService()
            .create(
                new DisasterEvent(
                    0,
                    "Flood 2026",
                    DisasterType.FLOOD,
                    "",
                    LocalDate.now(),
                    null,
                    DisasterStatus.ACTIVE,
                    0,
                    null),
                admin);
    eventId = event.id();
    AffectedArea area =
        context
            .affectedAreaService()
            .create(
                new AffectedArea(
                    0,
                    eventId,
                    "Ward 7",
                    "Dhaka",
                    null,
                    null,
                    500,
                    100,
                    Severity.LOW,
                    Accessibility.ACCESSIBLE,
                    MedicalUrgency.LOW,
                    "AVAILABLE",
                    "ACTIVE",
                    null,
                    null,
                    null),
                admin);
    areaId = area.id();
    resource1 =
        context
            .resourceService()
            .create(new Resource(0, "Rice", "FOOD", "bag", 10, true), admin)
            .id();
    resource2 =
        context
            .resourceService()
            .create(new Resource(0, "Water", "WATER", "case", 10, true), admin)
            .id();
  }

  @Test
  void normalMultiItemWorkflowPersistsAcrossRepositoryReload() {
    ReliefRequest created =
        context
            .reliefRequestService()
            .submit(draft(RequestPriority.NORMAL, 100, 200), areaCoordinator);
    assertEquals(RequestStateType.SUBMITTED, created.state());
    List<ReliefRequestItem> items =
        new SQLiteReliefRequestItemRepository(database).findByRequestId(created.id());
    assertEquals(2, items.size());
    assertTrue(
        items.stream()
            .allMatch(
                i ->
                    i.allocatedQuantity() == 0
                        && i.deliveredQuantity() == 0
                        && i.getRemainingQuantity() == i.requestedQuantity()));
    VerificationResult first =
        context
            .verificationService()
            .decide(created.id(), VerificationDecision.APPROVED, null, volunteer);
    assertEquals(VerificationOutcome.STEP_APPROVED, first.outcome());
    assertEquals(
        RequestStateType.UNDER_VERIFICATION,
        new SQLiteReliefRequestRepository(database).findById(created.id()).orElseThrow().state());
    VerificationResult finalResult =
        context
            .verificationService()
            .decide(created.id(), VerificationDecision.APPROVED, null, areaCoordinator);
    assertEquals(VerificationOutcome.VERIFIED, finalResult.outcome());
    ReliefRequest persisted =
        new SQLiteReliefRequestRepository(database).findById(created.id()).orElseThrow();
    assertEquals(RequestStateType.VERIFIED, persisted.state());
    assertNotNull(persisted.verifiedAt());
    assertEquals(
        2, new SQLiteVerificationRecordRepository(database).findByRequestId(created.id()).size());
  }

  @Test
  void returnedRequestCanBeRevisedAndRestartsAtVolunteerInNewRound() {
    ReliefRequest request =
        context
            .reliefRequestService()
            .submit(draft(RequestPriority.NORMAL, 100, 100), areaCoordinator);
    context
        .verificationService()
        .decide(request.id(), VerificationDecision.APPROVED, null, volunteer);
    assertThrows(
        ValidationException.class,
        () ->
            context
                .verificationService()
                .decide(request.id(), VerificationDecision.RETURNED, "", areaCoordinator));
    context
        .verificationService()
        .decide(
            request.id(), VerificationDecision.RETURNED, "Correct water quantity", areaCoordinator);
    assertEquals(
        RequestStateType.RETURNED,
        new SQLiteReliefRequestRepository(database).findById(request.id()).orElseThrow().state());
    context
        .reliefRequestService()
        .edit(request.id(), draft(RequestPriority.NORMAL, 100, 300), areaCoordinator);
    ReliefRequest resubmitted =
        context.reliefRequestService().resubmit(request.id(), areaCoordinator);
    assertEquals(2, resubmitted.verificationRound());
    assertEquals(2, context.verificationService().history(request.id(), areaCoordinator).size());
    assertTrue(
        context.verificationService().pending(volunteer).stream()
            .anyMatch(
                d ->
                    d.request().id() == request.id()
                        && d.nextRequiredLevel() == VerificationLevel.VOLUNTEER));
  }

  @Test
  void highAndCriticalChainsRequireEveryHumanInOrder() {
    ReliefRequest high =
        context.reliefRequestService().submit(draft(RequestPriority.HIGH, 10, 10), areaCoordinator);
    context.verificationService().decide(high.id(), VerificationDecision.APPROVED, null, volunteer);
    context
        .verificationService()
        .decide(high.id(), VerificationDecision.APPROVED, null, areaCoordinator);
    assertEquals(
        RequestStateType.UNDER_VERIFICATION,
        new SQLiteReliefRequestRepository(database).findById(high.id()).orElseThrow().state());
    context
        .verificationService()
        .decide(high.id(), VerificationDecision.APPROVED, null, reliefCoordinator);
    assertEquals(
        RequestStateType.VERIFIED,
        new SQLiteReliefRequestRepository(database).findById(high.id()).orElseThrow().state());
    ReliefRequest critical =
        context
            .reliefRequestService()
            .submit(draft(RequestPriority.CRITICAL, 10, 10), areaCoordinator);
    assertThrows(
        AuthorizationException.class,
        () ->
            context
                .verificationService()
                .decide(critical.id(), VerificationDecision.APPROVED, null, admin));
    context
        .verificationService()
        .decide(critical.id(), VerificationDecision.APPROVED, null, volunteer);
    context
        .verificationService()
        .decide(critical.id(), VerificationDecision.APPROVED, null, areaCoordinator);
    context
        .verificationService()
        .decide(critical.id(), VerificationDecision.APPROVED, null, reliefCoordinator);
    context.verificationService().decide(critical.id(), VerificationDecision.APPROVED, null, admin);
    assertEquals(
        RequestStateType.VERIFIED,
        new SQLiteReliefRequestRepository(database).findById(critical.id()).orElseThrow().state());
  }

  @Test
  void duplicateDetectionWarnsAndMergeIsRestricted() {
    ReliefRequest original =
        context
            .reliefRequestService()
            .submit(draft(RequestPriority.NORMAL, 10, 0), areaCoordinator);
    DuplicateCheckResult check =
        context
            .reliefRequestService()
            .checkDuplicates(draft(RequestPriority.NORMAL, 5, 5), null, areaCoordinator);
    assertTrue(check.probableDuplicate());
    assertEquals(0.5, check.matches().getFirst().overlapRatio());
    ReliefRequest merged =
        context
            .reliefRequestService()
            .submit(
                draft(RequestPriority.NORMAL, 5, 5),
                DuplicateResolution.MERGE,
                original.id(),
                areaCoordinator);
    assertEquals(
        15,
        new SQLiteReliefRequestItemRepository(database)
            .findByRequestId(merged.id()).stream()
                .filter(i -> i.resourceId() == resource1)
                .findFirst()
                .orElseThrow()
                .requestedQuantity());
    context
        .verificationService()
        .decide(original.id(), VerificationDecision.APPROVED, null, volunteer);
    assertThrows(
        BusinessRuleException.class,
        () ->
            context
                .reliefRequestService()
                .merge(original.id(), draft(RequestPriority.NORMAL, 1, 1), areaCoordinator));
  }

  @Test
  void multiWriteFailuresRollbackHeadersDecisionsAndState() throws Exception {
    try (Connection c = database.openConnection();
        Statement s = c.createStatement()) {
      s.execute(
          "CREATE TRIGGER fail_items BEFORE INSERT ON relief_request_items BEGIN SELECT"
              + " RAISE(FAIL,'forced'); END");
    }
    assertThrows(
        PersistenceException.class,
        () ->
            context
                .reliefRequestService()
                .submit(draft(RequestPriority.NORMAL, 10, 10), areaCoordinator));
    assertEquals(0, new SQLiteReliefRequestRepository(database).findAll().size());
    try (Connection c = database.openConnection();
        Statement s = c.createStatement()) {
      s.execute("DROP TRIGGER fail_items");
    }
    ReliefRequest request =
        context
            .reliefRequestService()
            .submit(draft(RequestPriority.NORMAL, 10, 10), areaCoordinator);
    try (Connection c = database.openConnection();
        Statement s = c.createStatement()) {
      s.execute(
          "CREATE TRIGGER fail_verification BEFORE INSERT ON verification_records BEGIN SELECT"
              + " RAISE(FAIL,'forced'); END");
    }
    assertThrows(
        PersistenceException.class,
        () ->
            context
                .verificationService()
                .decide(request.id(), VerificationDecision.APPROVED, null, volunteer));
    assertEquals(
        RequestStateType.SUBMITTED,
        new SQLiteReliefRequestRepository(database).findById(request.id()).orElseThrow().state());
    assertTrue(
        new SQLiteVerificationRecordRepository(database).findByRequestId(request.id()).isEmpty());
  }

  @Test
  void validationOwnershipCancellationAndMigrationRulesHold() throws Exception {
    assertThrows(
        ValidationException.class,
        () ->
            context
                .reliefRequestService()
                .submit(
                    new ReliefRequestDraft(eventId, areaId, RequestPriority.NORMAL, "", List.of()),
                    areaCoordinator));
    assertThrows(
        ValidationException.class,
        () ->
            context
                .reliefRequestService()
                .submit(
                    new ReliefRequestDraft(
                        eventId,
                        areaId,
                        RequestPriority.NORMAL,
                        "",
                        List.of(new ReliefRequestDraftItem(resource1, 0))),
                    areaCoordinator));
    assertThrows(
        ValidationException.class,
        () ->
            context
                .reliefRequestService()
                .submit(
                    new ReliefRequestDraft(
                        eventId,
                        areaId,
                        RequestPriority.NORMAL,
                        "",
                        List.of(
                            new ReliefRequestDraftItem(resource1, 1),
                            new ReliefRequestDraftItem(resource1, 2))),
                    areaCoordinator));
    ReliefRequest request =
        context.reliefRequestService().submit(draft(RequestPriority.NORMAL, 1, 1), areaCoordinator);
    assertThrows(
        AuthorizationException.class,
        () ->
            context
                .reliefRequestService()
                .edit(request.id(), draft(RequestPriority.NORMAL, 2, 2), adminSessionOther()));
    assertEquals(
        RequestStateType.CANCELLED,
        context.reliefRequestService().cancel(request.id(), areaCoordinator).state());
    try (Connection c = database.openConnection();
        Statement s = c.createStatement()) {
      assertTrue(s.executeQuery("SELECT 1 FROM schema_migrations WHERE version='V004'").next());
      assertTrue(
          s.executeQuery(
                  "SELECT 1 FROM pragma_table_info('relief_requests') WHERE"
                      + " name='verification_round'")
              .next());
      assertTrue(
          s.executeQuery(
                  "SELECT 1 FROM sqlite_master WHERE type='index' AND"
                      + " name='idx_active_request_duplicate'")
              .next());
    }
  }

  @Test
  void duplicateDetectionIgnoresDifferentScopeOldTerminalAndSelf() throws Exception {
    ReliefRequest original =
        context
            .reliefRequestService()
            .submit(draft(RequestPriority.NORMAL, 10, 10), areaCoordinator);
    assertFalse(
        context
            .reliefRequestService()
            .checkDuplicates(draft(RequestPriority.NORMAL, 1, 1), original.id(), areaCoordinator)
            .probableDuplicate());
    DisasterEvent otherEvent =
        context
            .disasterEventService()
            .create(
                new DisasterEvent(
                    0,
                    "Other Flood",
                    DisasterType.FLOOD,
                    "",
                    LocalDate.now(),
                    null,
                    DisasterStatus.ACTIVE,
                    0,
                    null),
                admin);
    AffectedArea otherArea =
        context
            .affectedAreaService()
            .create(
                new AffectedArea(
                    0,
                    otherEvent.id(),
                    "Other Ward",
                    "Dhaka",
                    null,
                    null,
                    10,
                    2,
                    Severity.LOW,
                    Accessibility.ACCESSIBLE,
                    MedicalUrgency.LOW,
                    "AVAILABLE",
                    "ACTIVE",
                    null,
                    null,
                    null),
                admin);
    ReliefRequestDraft differentScope =
        new ReliefRequestDraft(
            otherEvent.id(),
            otherArea.id(),
            RequestPriority.NORMAL,
            "Other",
            List.of(new ReliefRequestDraftItem(resource1, 1)));
    assertFalse(
        context
            .reliefRequestService()
            .checkDuplicates(differentScope, null, areaCoordinator)
            .probableDuplicate());
    try (Connection c = database.openConnection();
        PreparedStatement s =
            c.prepareStatement("UPDATE relief_requests SET submitted_at=? WHERE id=?")) {
      s.setString(1, LocalDateTime.now().minusDays(8).toString());
      s.setLong(2, original.id());
      s.executeUpdate();
    }
    assertFalse(
        context
            .reliefRequestService()
            .checkDuplicates(draft(RequestPriority.NORMAL, 1, 1), null, areaCoordinator)
            .probableDuplicate());
    ReliefRequest terminal =
        context.reliefRequestService().submit(draft(RequestPriority.NORMAL, 2, 2), areaCoordinator);
    context.reliefRequestService().cancel(terminal.id(), areaCoordinator);
    assertFalse(
        context
            .reliefRequestService()
            .checkDuplicates(draft(RequestPriority.NORMAL, 1, 1), null, areaCoordinator)
            .probableDuplicate());
  }

  @Test
  void rejectionIsTerminalAndFinalApprovalFailureRollsBackDecision() throws Exception {
    ReliefRequest rejected =
        context.reliefRequestService().submit(draft(RequestPriority.NORMAL, 3, 3), areaCoordinator);
    context
        .verificationService()
        .decide(rejected.id(), VerificationDecision.REJECTED, "Could not verify", volunteer);
    assertEquals(
        RequestStateType.REJECTED,
        new SQLiteReliefRequestRepository(database).findById(rejected.id()).orElseThrow().state());
    assertThrows(
        BusinessRuleException.class,
        () ->
            context
                .verificationService()
                .decide(rejected.id(), VerificationDecision.APPROVED, null, volunteer));

    ReliefRequest request =
        context.reliefRequestService().submit(draft(RequestPriority.NORMAL, 4, 4), areaCoordinator);
    context
        .verificationService()
        .decide(request.id(), VerificationDecision.APPROVED, null, volunteer);
    try (Connection c = database.openConnection();
        Statement s = c.createStatement()) {
      s.execute(
          "CREATE TRIGGER fail_verified BEFORE UPDATE OF state ON relief_requests "
              + "WHEN NEW.state='VERIFIED' BEGIN SELECT RAISE(FAIL,'forced'); END");
    }
    assertThrows(
        PersistenceException.class,
        () ->
            context
                .verificationService()
                .decide(request.id(), VerificationDecision.APPROVED, null, areaCoordinator));
    assertEquals(
        RequestStateType.UNDER_VERIFICATION,
        new SQLiteReliefRequestRepository(database).findById(request.id()).orElseThrow().state());
    assertEquals(
        1, new SQLiteVerificationRecordRepository(database).findByRequestId(request.id()).size());
  }

  private ReliefRequestDraft draft(RequestPriority priority, long q1, long q2) {
    List<ReliefRequestDraftItem> list = new ArrayList<>();
    if (q1 != 0) list.add(new ReliefRequestDraftItem(resource1, q1));
    if (q2 != 0) list.add(new ReliefRequestDraftItem(resource2, q2));
    return new ReliefRequestDraft(eventId, areaId, priority, "Need supplies", list);
  }

  private static UserSession session(SQLiteUserRepository users, String username) {
    User u = users.findByUsername(username).orElseThrow();
    return new UserSession(u.id(), u.fullName(), u.username(), u.role());
  }

  private UserSession adminSessionOther() {
    return new UserSession(999, "Other", "other", Role.AREA_COORDINATOR);
  }
}
