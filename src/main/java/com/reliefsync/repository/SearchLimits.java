package com.reliefsync.repository;

import com.reliefsync.exception.ValidationException;

public final class SearchLimits {
  public static final int DEFAULT = 200;
  public static final int MAXIMUM = 200;

  private SearchLimits() {}

  public static int requireValid(int limit) {
    if (limit < 1 || limit > MAXIMUM) {
      throw new ValidationException("Search limit must be between 1 and " + MAXIMUM + ".");
    }
    return limit;
  }
}
