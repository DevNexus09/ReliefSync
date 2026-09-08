package com.reliefsync.service;

import com.reliefsync.exception.*;
import com.reliefsync.model.*;
import com.reliefsync.model.enums.*;
import com.reliefsync.model.request.*;
import com.reliefsync.model.search.ReliefRequestSearchCriteria;
import com.reliefsync.pattern.state.*;
import com.reliefsync.repository.*;
import com.reliefsync.security.*;
import com.reliefsync.service.dto.ReliefRequestDetails;
import com.reliefsync.validation.*;
import java.time.LocalDateTime;
import java.util.*;

public final class ReliefRequestService {
  private final ReliefRequestRepository requests;
  private final ReliefRequestItemRepository items;
  private final RequestWorkflowTransactionRepository transactions;
  private final ReliefRequestValidator validator;
  private final DuplicateRequestDetector duplicates;
  private final AuthorizationService authorization;
  private final ReliefRequestStateRegistry states;

  public ReliefRequestService(
      ReliefRequestRepository requests,
      ReliefRequestItemRepository items,
      RequestWorkflowTransactionRepository transactions,
      ReliefRequestValidator validator,
      DuplicateRequestDetector duplicates,
      AuthorizationService authorization,
      ReliefRequestStateRegistry states) {
    this.requests = requests;
    this.items = items;
    this.transactions = transactions;
    this.validator = validator;
    this.duplicates = duplicates;
    this.authorization = authorization;
    this.states = states;
  }

  public DuplicateCheckResult checkDuplicates(
      ReliefRequestDraft draft, Long excludeId, UserSession session) {
    authorization.require(session, Permission.CREATE_RELIEF_REQUEST);
    validator.validate(draft);
    return duplicates.check(draft, excludeId);
  }

  public ReliefRequest submit(
      ReliefRequestDraft draft,
      DuplicateResolution resolution,
      Long mergeTargetId,
      UserSession session) {
    authorization.require(session, Permission.CREATE_RELIEF_REQUEST);
    validator.validate(draft);
    if (resolution == null) throw new ValidationException("Duplicate resolution is required.");
    if (resolution == DuplicateResolution.CANCEL) return null;
    DuplicateCheckResult check = duplicates.check(draft, null);
    if (resolution == DuplicateResolution.MERGE) {
      if (mergeTargetId == null) throw new ValidationException("Select a request to merge.");
      DuplicateMatch match =
          check.matches().stream()
              .filter(m -> m.requestId() == mergeTargetId)
              .findFirst()
              .orElseThrow(
                  () -> new BusinessRuleException("Selected request is not a current duplicate."));
      if (!match.mergeable()) throw new BusinessRuleException("This request is not safe to merge.");
      return merge(mergeTargetId, draft, session);
    }
    LocalDateTime now = LocalDateTime.now();
    ReliefRequest value =
        new ReliefRequest(
            0,
            draft.disasterEventId(),
            draft.affectedAreaId(),
            session.userId(),
            draft.priority(),
            RequestStateType.SUBMITTED,
            clean(draft.description()),
            now,
            null,
            1,
            now,
            now);
    return transactions.execute(
        u -> {
          long id = u.saveRequest(value);
          u.saveItems(id, toItems(id, draft.items()));
          return copyId(value, id);
        });
  }

  public ReliefRequest submit(ReliefRequestDraft draft, UserSession session) {
    return submit(draft, DuplicateResolution.CONTINUE_ANYWAY, null, session);
  }

  public ReliefRequest edit(long id, ReliefRequestDraft draft, UserSession session) {
    authorization.require(session, Permission.CREATE_RELIEF_REQUEST);
    validator.validate(draft);
    ReliefRequest current = required(id);
    requireOwner(current, session);
    if (draft.disasterEventId() != current.disasterEventId()
        || draft.affectedAreaId() != current.affectedAreaId()) {
      throw new BusinessRuleException(
          "The disaster and affected area cannot be changed after submission.");
    }
    if (current.state() != RequestStateType.SUBMITTED
        && current.state() != RequestStateType.RETURNED)
      throw new BusinessRuleException(
          "This request can no longer be edited because verification has started.");
    ReliefRequest changed =
        new ReliefRequest(
            id,
            draft.disasterEventId(),
            draft.affectedAreaId(),
            current.requestedBy(),
            draft.priority(),
            current.state(),
            clean(draft.description()),
            current.submittedAt(),
            current.verifiedAt(),
            current.verificationRound(),
            current.createdAt(),
            LocalDateTime.now());
    return transactions.execute(
        u -> {
          u.updateRequest(changed);
          u.replaceItems(id, toItems(id, draft.items()));
          return changed;
        });
  }

  public ReliefRequest merge(long id, ReliefRequestDraft incoming, UserSession session) {
    authorization.require(session, Permission.CREATE_RELIEF_REQUEST);
    ReliefRequest current = required(id);
    requireOwner(current, session);
    if (current.state() != RequestStateType.SUBMITTED
        && current.state() != RequestStateType.RETURNED)
      throw new BusinessRuleException("Only submitted or returned requests may be merged.");
    Map<Long, Long> quantities = new LinkedHashMap<>();
    items.findByRequestId(id).forEach(i -> quantities.put(i.resourceId(), i.requestedQuantity()));
    try {
      incoming
          .items()
          .forEach(i -> quantities.merge(i.resourceId(), i.requestedQuantity(), Math::addExact));
    } catch (ArithmeticException exception) {
      throw new ValidationException("Merged request quantity is too large.");
    }
    ReliefRequestDraft merged =
        new ReliefRequestDraft(
            current.disasterEventId(),
            current.affectedAreaId(),
            current.priority(),
            current.description(),
            quantities.entrySet().stream()
                .map(e -> new ReliefRequestDraftItem(e.getKey(), e.getValue()))
                .toList());
    return edit(id, merged, session);
  }

  public ReliefRequest resubmit(long id, UserSession session) {
    authorization.require(session, Permission.CREATE_RELIEF_REQUEST);
    ReliefRequest current = required(id);
    requireOwner(current, session);
    ReliefRequestContext context = new ReliefRequestContext(current, states);
    context.state().submit(context);
    LocalDateTime now = LocalDateTime.now();
    ReliefRequest changed =
        new ReliefRequest(
            current.id(),
            current.disasterEventId(),
            current.affectedAreaId(),
            current.requestedBy(),
            current.priority(),
            context.stateType(),
            current.description(),
            now,
            null,
            current.verificationRound() + 1,
            current.createdAt(),
            now);
    return transactions.execute(
        u -> {
          u.updateRequest(changed);
          return changed;
        });
  }

  public ReliefRequest cancel(long id, UserSession session) {
    authorization.require(session, Permission.CREATE_RELIEF_REQUEST);
    ReliefRequest current = required(id);
    requireOwner(current, session);
    ReliefRequestContext context = new ReliefRequestContext(current, states);
    context.state().cancel(context);
    ReliefRequest changed = withState(current, context.stateType(), null, LocalDateTime.now());
    return transactions.execute(
        u -> {
          u.updateRequest(changed);
          return changed;
        });
  }

  public ReliefRequestDetails details(long id, UserSession session) {
    authorization.require(session, Permission.VIEW_RELIEF_REQUESTS);
    ReliefRequest request = required(id);
    return new ReliefRequestDetails(request, items.findByRequestId(id));
  }

  public List<ReliefRequest> search(ReliefRequestSearchCriteria criteria, UserSession session) {
    authorization.require(session, Permission.VIEW_RELIEF_REQUESTS);
    return requests.search(criteria, SearchLimits.DEFAULT);
  }

  private ReliefRequest required(long id) {
    return requests
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Relief request was not found: " + id));
  }

  private void requireOwner(ReliefRequest r, UserSession s) {
    if (r.requestedBy() != s.userId() && s.role() != Role.ADMINISTRATOR)
      throw new AuthorizationException("Only the original requester may modify this request.");
  }

  private static List<ReliefRequestItem> toItems(long id, List<ReliefRequestDraftItem> draft) {
    return draft.stream()
        .map(i -> new ReliefRequestItem(0, id, i.resourceId(), i.requestedQuantity(), 0, 0))
        .toList();
  }

  private static ReliefRequest copyId(ReliefRequest r, long id) {
    return new ReliefRequest(
        id,
        r.disasterEventId(),
        r.affectedAreaId(),
        r.requestedBy(),
        r.priority(),
        r.state(),
        r.description(),
        r.submittedAt(),
        r.verifiedAt(),
        r.verificationRound(),
        r.createdAt(),
        r.updatedAt());
  }

  private static ReliefRequest withState(
      ReliefRequest r, RequestStateType state, LocalDateTime verified, LocalDateTime now) {
    return new ReliefRequest(
        r.id(),
        r.disasterEventId(),
        r.affectedAreaId(),
        r.requestedBy(),
        r.priority(),
        state,
        r.description(),
        r.submittedAt(),
        verified,
        r.verificationRound(),
        r.createdAt(),
        now);
  }

  private static String clean(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }
}
