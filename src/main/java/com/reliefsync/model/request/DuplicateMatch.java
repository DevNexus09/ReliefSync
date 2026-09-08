package com.reliefsync.model.request;

import com.reliefsync.model.enums.RequestPriority;
import com.reliefsync.model.enums.RequestStateType;
import java.time.LocalDateTime;
import java.util.Set;

public record DuplicateMatch(
    long requestId,
    RequestStateType state,
    RequestPriority priority,
    LocalDateTime submittedAt,
    double overlapRatio,
    Set<Long> commonResourceIds,
    boolean mergeable) {}
