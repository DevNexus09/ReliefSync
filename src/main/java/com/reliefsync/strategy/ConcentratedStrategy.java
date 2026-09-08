package com.reliefsync.strategy;

import com.reliefsync.model.PlannedAllocation;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.StockView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fills each item from the centers holding the most stock first, so a delivery
 * touches as few centers (and vehicles) as possible.
 */
public final class ConcentratedStrategy implements AllocationStrategy {

    @Override
    public String name() {
        return "Fewest Centers";
    }

    @Override
    public String description() {
        return "Take from the largest stockpiles first to minimize the number of pickup points.";
    }

    @Override
    public List<PlannedAllocation> plan(List<RequestItem> items, List<StockView> stock) {
        List<PlannedAllocation> plan = new ArrayList<>();
        for (RequestItem item : items) {
            int need = item.outstanding();
            if (need <= 0) {
                continue;
            }
            List<StockView> centers = stock.stream()
                    .filter(s -> s.resourceId() == item.resourceId() && s.quantity() > 0)
                    .sorted(Comparator.<StockView>comparingInt(s -> -s.quantity())
                            .thenComparingLong(StockView::centerId))
                    .toList();
            for (StockView center : centers) {
                if (need == 0) {
                    break;
                }
                int take = Math.min(need, center.quantity());
                plan.add(new PlannedAllocation(center.centerId(), center.centerName(),
                        item.resourceId(), item.resourceName(), take));
                need -= take;
            }
        }
        return plan;
    }
}
