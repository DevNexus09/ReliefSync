package com.reliefsync.model.search;

import com.reliefsync.model.enums.Accessibility;
import com.reliefsync.model.enums.Severity;

public record AffectedAreaSearchCriteria(
    Long disasterEventId,
    String district,
    Severity severity,
    Accessibility accessibility,
    String status) {}
