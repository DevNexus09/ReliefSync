package com.reliefsync.validation;

import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.enums.RequestStateType;
import com.reliefsync.model.request.*;
import com.reliefsync.repository.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public final class DuplicateRequestDetector {
  public static final Duration DEFAULT_WINDOW = Duration.ofDays(7);
  public static final double DEFAULT_OVERLAP = 0.50;
  private final ReliefRequestRepository requests;
  private final ReliefRequestItemRepository items;
  private final Duration window;
  private final double threshold;

  public DuplicateRequestDetector(
      ReliefRequestRepository requests, ReliefRequestItemRepository items) {
    this(requests, items, DEFAULT_WINDOW, DEFAULT_OVERLAP);
  }

  public DuplicateRequestDetector(
      ReliefRequestRepository requests,
      ReliefRequestItemRepository items,
      Duration window,
      double threshold) {
    this.requests = requests;
    this.items = items;
    this.window = window;
    this.threshold = threshold;
  }

  public DuplicateCheckResult check(ReliefRequestDraft draft, Long excludeRequestId) {
    Set<Long> incoming =
        draft.items().stream().map(ReliefRequestDraftItem::resourceId).collect(Collectors.toSet());
    if (incoming.isEmpty()) return new DuplicateCheckResult(List.of());
    List<ReliefRequest> candidates =
        requests.findPotentialDuplicates(
            draft.disasterEventId(),
            draft.affectedAreaId(),
            LocalDateTime.now().minus(window),
            SearchLimits.DEFAULT);
    Map<Long, Set<Long>> existing =
        items.findResourceIdsByRequestIds(candidates.stream().map(ReliefRequest::id).toList());
    List<DuplicateMatch> matches = new ArrayList<>();
    for (ReliefRequest candidate : candidates) {
      if (Objects.equals(excludeRequestId, candidate.id())) continue;
      Set<Long> common = new HashSet<>(existing.getOrDefault(candidate.id(), Set.of()));
      common.retainAll(incoming);
      double ratio = (double) common.size() / incoming.size();
      if (ratio >= threshold)
        matches.add(
            new DuplicateMatch(
                candidate.id(),
                candidate.state(),
                candidate.priority(),
                candidate.submittedAt(),
                ratio,
                Set.copyOf(common),
                candidate.state() == RequestStateType.SUBMITTED
                    || candidate.state() == RequestStateType.RETURNED));
    }
    return new DuplicateCheckResult(matches);
  }
}
