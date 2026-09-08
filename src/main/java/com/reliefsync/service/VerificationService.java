package com.reliefsync.service;

import com.reliefsync.exception.*;
import com.reliefsync.model.*;
import com.reliefsync.model.enums.*;
import com.reliefsync.model.search.ReliefRequestSearchCriteria;
import com.reliefsync.model.verification.*;
import com.reliefsync.pattern.chain.*;
import com.reliefsync.pattern.state.*;
import com.reliefsync.repository.*;
import com.reliefsync.security.*;
import com.reliefsync.service.dto.VerificationDetails;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class VerificationService {
  private final ReliefRequestRepository requests;
  private final ReliefRequestItemRepository items;
  private final AffectedAreaRepository areas;
  private final VerificationRecordRepository records;
  private final RequestWorkflowTransactionRepository transactions;
  private final AuthorizationService authorization;
  private final VerificationPolicy policy;
  private final VerificationChainBuilder chains;
  private final ReliefRequestStateRegistry states;

  public VerificationService(
      ReliefRequestRepository requests,
      ReliefRequestItemRepository items,
      AffectedAreaRepository areas,
      VerificationRecordRepository records,
      RequestWorkflowTransactionRepository transactions,
      AuthorizationService authorization,
      VerificationPolicy policy,
      VerificationChainBuilder chains,
      ReliefRequestStateRegistry states) {
    this.requests = requests;
    this.items = items;
    this.areas = areas;
    this.records = records;
    this.transactions = transactions;
    this.authorization = authorization;
    this.policy = policy;
    this.chains = chains;
    this.states = states;
  }

  public VerificationDetails details(long id, UserSession session) {
    authorization.require(session, Permission.VERIFY_RELIEF_REQUEST);
    ReliefRequest request = required(id);
    AffectedArea area =
        areas
            .findById(request.affectedAreaId())
            .orElseThrow(() -> new NotFoundException("Affected area was not found."));
    List<ReliefRequestItem> requestItems = items.findByRequestId(id);
    List<VerificationRecord> history = records.findByRequestId(id);
    VerificationTier tier = policy.determine(request, area, requestItems);
    VerificationLevel next = nextLevel(request, tier, history);
    return new VerificationDetails(request, area, requestItems, history, tier, next);
  }

  public List<VerificationDetails> pending(UserSession session) {
    authorization.require(session, Permission.VERIFY_RELIEF_REQUEST);
    List<ReliefRequest> candidates = new ArrayList<>();
    candidates.addAll(
        requests.search(
            new ReliefRequestSearchCriteria(null, null, null, RequestStateType.SUBMITTED, null),
            SearchLimits.DEFAULT));
    candidates.addAll(
        requests.search(
            new ReliefRequestSearchCriteria(
                null, null, null, RequestStateType.UNDER_VERIFICATION, null),
            SearchLimits.DEFAULT));
    return candidates.stream()
        .limit(SearchLimits.DEFAULT)
        .map(r -> details(r.id(), session))
        .filter(d -> canProcess(d, session))
        .toList();
  }

  public VerificationResult decide(
      long requestId, VerificationDecision decision, String reason, UserSession session) {
    authorization.require(session, Permission.VERIFY_RELIEF_REQUEST);
    return transactions.execute(
        u -> {
          ReliefRequest request =
              u.findRequest(requestId)
                  .orElseThrow(
                      () -> new NotFoundException("Relief request was not found: " + requestId));
          AffectedArea area =
              u.findArea(request.affectedAreaId())
                  .orElseThrow(() -> new NotFoundException("Affected area was not found."));
          if (request.state() != RequestStateType.SUBMITTED
              && request.state() != RequestStateType.UNDER_VERIFICATION)
            throw new BusinessRuleException("This request is not awaiting verification.");
          List<ReliefRequestItem> requestItems = u.findItems(requestId);
          List<VerificationRecord> history =
              u.findVerifications(requestId, request.verificationRound());
          Set<VerificationLevel> approved = approved(history);
          VerificationTier tier = policy.determine(request, area, requestItems);
          ReliefRequestContext state = new ReliefRequestContext(request, states);
          if (request.state() == RequestStateType.SUBMITTED) state.state().beginVerification(state);
          VerificationResult result =
              chains
                  .build(tier)
                  .handle(new VerificationContext(session, decision, reason, approved));
          if (result.outcome() == VerificationOutcome.VERIFIED) state.state().markVerified(state);
          else if (result.outcome() == VerificationOutcome.RETURNED)
            state.state().returnForCorrection(state);
          else if (result.outcome() == VerificationOutcome.REJECTED) state.state().reject(state);
          LocalDateTime now = LocalDateTime.now();
          VerificationRecord record =
              new VerificationRecord(
                  0,
                  requestId,
                  result.processedLevel().name(),
                  session.userId(),
                  decision,
                  clean(reason),
                  request.verificationRound(),
                  now);
          u.saveVerification(record);
          ReliefRequest changed =
              new ReliefRequest(
                  request.id(),
                  request.disasterEventId(),
                  request.affectedAreaId(),
                  request.requestedBy(),
                  request.priority(),
                  state.stateType(),
                  request.description(),
                  request.submittedAt(),
                  result.outcome() == VerificationOutcome.VERIFIED ? now : request.verifiedAt(),
                  request.verificationRound(),
                  request.createdAt(),
                  now);
          u.updateRequest(changed);
          return result;
        });
  }

  public List<VerificationRecord> history(long id, UserSession session) {
    authorization.require(session, Permission.VIEW_RELIEF_REQUESTS);
    required(id);
    return records.findByRequestId(id);
  }

  private VerificationLevel nextLevel(
      ReliefRequest r, VerificationTier tier, List<VerificationRecord> history) {
    if (r.state() != RequestStateType.SUBMITTED && r.state() != RequestStateType.UNDER_VERIFICATION)
      return null;
    Set<VerificationLevel> approved =
        history.stream()
            .filter(
                x ->
                    x.verificationRound() == r.verificationRound()
                        && x.decision() == VerificationDecision.APPROVED)
            .map(x -> VerificationLevel.valueOf(x.level()))
            .collect(Collectors.toSet());
    return chains
        .build(tier)
        .nextRequired(
            new VerificationContext(
                new UserSession(0, "", "", Role.ADMINISTRATOR),
                VerificationDecision.APPROVED,
                null,
                approved));
  }

  private boolean canProcess(VerificationDetails d, UserSession session) {
    if (d.nextRequiredLevel() == null) return false;
    try {
      chains
          .build(d.tier())
          .handle(
              new VerificationContext(
                  session,
                  VerificationDecision.APPROVED,
                  null,
                  approved(
                      d.history().stream()
                          .filter(x -> x.verificationRound() == d.request().verificationRound())
                          .toList())));
      return true;
    } catch (AuthorizationException e) {
      return false;
    }
  }

  private static Set<VerificationLevel> approved(List<VerificationRecord> history) {
    return history.stream()
        .filter(r -> r.decision() == VerificationDecision.APPROVED)
        .map(r -> VerificationLevel.valueOf(r.level()))
        .collect(Collectors.toSet());
  }

  private ReliefRequest required(long id) {
    return requests
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Relief request was not found: " + id));
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
