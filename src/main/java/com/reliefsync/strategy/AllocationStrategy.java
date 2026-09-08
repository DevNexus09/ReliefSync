package com.reliefsync.strategy;

import com.reliefsync.model.PlannedAllocation;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.StockView;
import java.util.List;

/**
 * Strategy pattern: how limited stock is split across relief centers.
 *
 * Strategies are pure planning functions — they never touch the database — so
 * they can be previewed in the UI, unit tested in isolation, and a new policy
 * (e.g. nearest-center-first once distances are modeled) can be added without
 * changing the allocation service.
 */
public interface AllocationStrategy {

    String name();

    String description();

    List<PlannedAllocation> plan(List<RequestItem> items, List<StockView> stock);
}
