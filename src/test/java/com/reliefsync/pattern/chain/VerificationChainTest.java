package com.reliefsync.pattern.chain;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefsync.exception.*;
import com.reliefsync.model.*;
import com.reliefsync.model.enums.*;
import com.reliefsync.model.verification.*;
import com.reliefsync.security.UserSession;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;

class VerificationChainTest {
  private final VerificationChainBuilder builder = new VerificationChainBuilder();

  @Test
  void tiersContainExactHumanLevels() {
    assertChain(
        VerificationTier.NORMAL,
        List.of(VerificationLevel.VOLUNTEER, VerificationLevel.AREA_COORDINATOR));
    assertChain(
        VerificationTier.HIGH,
        List.of(
            VerificationLevel.VOLUNTEER,
            VerificationLevel.AREA_COORDINATOR,
            VerificationLevel.RELIEF_COORDINATOR));
    assertChain(
        VerificationTier.CRITICAL,
        List.of(
            VerificationLevel.VOLUNTEER,
            VerificationLevel.AREA_COORDINATOR,
            VerificationLevel.RELIEF_COORDINATOR,
            VerificationLevel.ADMINISTRATOR));
  }

  @Test
  void eachCallProcessesOnlyOneHumanAndSkipsCompletedLevels() {
    VerificationResult first =
        builder.build(VerificationTier.NORMAL).handle(ctx(Role.VOLUNTEER, Set.of()));
    assertEquals(VerificationOutcome.STEP_APPROVED, first.outcome());
    assertEquals(VerificationLevel.AREA_COORDINATOR, first.nextRequiredLevel());
    VerificationResult second =
        builder
            .build(VerificationTier.NORMAL)
            .handle(ctx(Role.AREA_COORDINATOR, Set.of(VerificationLevel.VOLUNTEER)));
    assertEquals(VerificationOutcome.VERIFIED, second.outcome());
  }

  @Test
  void levelsCannotBeSkippedOrRepeated() {
    assertThrows(
        AuthorizationException.class,
        () -> builder.build(VerificationTier.HIGH).handle(ctx(Role.AREA_COORDINATOR, Set.of())));
    assertThrows(
        AuthorizationException.class,
        () ->
            builder
                .build(VerificationTier.HIGH)
                .handle(ctx(Role.VOLUNTEER, Set.of(VerificationLevel.VOLUNTEER))));
  }

  @Test
  void returnAndRejectStopAtCurrentHandler() {
    VerificationResult returned =
        builder
            .build(VerificationTier.CRITICAL)
            .handle(
                new VerificationContext(
                    session(Role.VOLUNTEER), VerificationDecision.RETURNED, "fix", Set.of()));
    assertEquals(VerificationOutcome.RETURNED, returned.outcome());
    VerificationResult rejected =
        builder
            .build(VerificationTier.CRITICAL)
            .handle(
                new VerificationContext(
                    session(Role.VOLUNTEER), VerificationDecision.REJECTED, "invalid", Set.of()));
    assertEquals(VerificationOutcome.REJECTED, rejected.outcome());
    assertThrows(
        ValidationException.class,
        () ->
            builder
                .build(VerificationTier.NORMAL)
                .handle(
                    new VerificationContext(
                        session(Role.VOLUNTEER), VerificationDecision.RETURNED, "", Set.of())));
  }

  @Test
  void policyEscalatesFromEachConfiguredInput() {
    VerificationPolicy policy = new VerificationPolicy(VerificationPolicyConfig.defaults());
    assertEquals(
        VerificationTier.NORMAL,
        policy.determine(
            request(RequestPriority.NORMAL), area(Severity.LOW, MedicalUrgency.LOW), items(100)));
    assertEquals(
        VerificationTier.HIGH,
        policy.determine(
            request(RequestPriority.HIGH), area(Severity.LOW, MedicalUrgency.LOW), items(100)));
    assertEquals(
        VerificationTier.CRITICAL,
        policy.determine(
            request(RequestPriority.NORMAL),
            area(Severity.CRITICAL, MedicalUrgency.LOW),
            items(100)));
    assertEquals(
        VerificationTier.CRITICAL,
        policy.determine(
            request(RequestPriority.NORMAL),
            area(Severity.LOW, MedicalUrgency.CRITICAL),
            items(100)));
    assertEquals(
        VerificationTier.HIGH,
        policy.determine(
            request(RequestPriority.NORMAL), area(Severity.LOW, MedicalUrgency.LOW), items(500)));
    assertEquals(
        VerificationTier.CRITICAL,
        policy.determine(
            request(RequestPriority.NORMAL), area(Severity.LOW, MedicalUrgency.LOW), items(1000)));
  }

  private void assertChain(VerificationTier tier, List<VerificationLevel> expected) {
    VerificationHandler chain = builder.build(tier);
    Set<VerificationLevel> done = new LinkedHashSet<>();
    for (VerificationLevel level : expected) {
      assertEquals(level, chain.nextRequired(ctx(Role.ADMINISTRATOR, done)));
      done.add(level);
    }
    assertNull(chain.nextRequired(ctx(Role.ADMINISTRATOR, done)));
  }

  private VerificationContext ctx(Role role, Set<VerificationLevel> approved) {
    return new VerificationContext(session(role), VerificationDecision.APPROVED, null, approved);
  }

  private UserSession session(Role role) {
    return new UserSession(1, "Test", role.name(), role);
  }

  private ReliefRequest request(RequestPriority priority) {
    LocalDateTime n = LocalDateTime.now();
    return new ReliefRequest(
        1, 1, 1, 1, priority, RequestStateType.SUBMITTED, null, n, null, 1, n, n);
  }

  private AffectedArea area(Severity severity, MedicalUrgency medical) {
    LocalDateTime n = LocalDateTime.now();
    return new AffectedArea(
        1,
        1,
        "Area",
        "D",
        null,
        null,
        1,
        1,
        severity,
        Accessibility.ACCESSIBLE,
        medical,
        "YES",
        "ACTIVE",
        null,
        n,
        n);
  }

  private List<ReliefRequestItem> items(long quantity) {
    return List.of(new ReliefRequestItem(1, 1, 1, quantity, 0, 0));
  }
}
