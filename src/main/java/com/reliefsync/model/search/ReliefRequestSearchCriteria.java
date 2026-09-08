package com.reliefsync.model.search;

import com.reliefsync.model.enums.RequestPriority;
import com.reliefsync.model.enums.RequestStateType;
import java.time.LocalDate;

public record ReliefRequestSearchCriteria(
    Long disasterEventId,
    Long affectedAreaId,
    RequestPriority priority,
    RequestStateType state,
    LocalDate submittedOn) {}
