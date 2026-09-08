package com.reliefsync.model.request;

import java.util.List;

public record DuplicateCheckResult(List<DuplicateMatch> matches) {
  public DuplicateCheckResult {
    matches = List.copyOf(matches);
  }

  public boolean probableDuplicate() {
    return !matches.isEmpty();
  }
}
