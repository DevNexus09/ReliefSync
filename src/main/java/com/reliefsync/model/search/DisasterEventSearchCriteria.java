package com.reliefsync.model.search;

import com.reliefsync.model.enums.DisasterStatus;
import com.reliefsync.model.enums.DisasterType;
import java.time.LocalDate;

public record DisasterEventSearchCriteria(
    String name,
    DisasterType type,
    DisasterStatus status,
    LocalDate startFrom,
    LocalDate startTo) {}
