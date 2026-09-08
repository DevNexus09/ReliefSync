package com.reliefsync.service;

import com.reliefsync.model.PlannedAllocation;
import com.reliefsync.model.Shortage;
import java.util.List;

/** Outcome of planning or executing an allocation: the lines plus any shortages. */
public record AllocationResult(List<PlannedAllocation> lines, List<Shortage> shortages) {

    public boolean fullyCovered() {
        return shortages.isEmpty();
    }
}
