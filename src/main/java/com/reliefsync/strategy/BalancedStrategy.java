package com.reliefsync.strategy;

import com.reliefsync.model.PlannedAllocation;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.StockView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spreads each item across every center that has stock, so no single center is
 * drained and later requests can still be served locally.
 */
public final class BalancedStrategy implements AllocationStrategy {

    @Override
    public String name() {
        return "Balanced Across Centers";
    }

    @Override
    public String description() {
        return "Split each item evenly across all stocked centers to preserve local reserves.";
    }

    @Override
    public List<PlannedAllocation> plan(List<RequestItem> items, List<StockView> stock) {
        List<PlannedAllocation> plan = new ArrayList<>();
        for (RequestItem item : items) {
            int need = item.outstanding();
            if (need <= 0) {
                continue;
            }
            List<StockView> centers = new ArrayList<>(stock.stream()
                    .filter(s -> s.resourceId() == item.resourceId() && s.quantity() > 0)
                    .sorted(Comparator.<StockView>comparingInt(s -> -s.quantity())
                            .thenComparingLong(StockView::centerId))
                    .toList());
            int[] remaining = new int[centers.size()];
            for (int i = 0; i < centers.size(); i++) {
                remaining[i] = centers.get(i).quantity();
            }
            Map<Integer, Integer> takenByCenter = new LinkedHashMap<>();
            while (need > 0) {
                int stocked = 0;
                for (int r : remaining) {
                    if (r > 0) {
                        stocked++;
                    }
                }
                if (stocked == 0) {
                    break;
                }
                int share = (int) Math.ceil(need / (double) stocked);
                for (int i = 0; i < centers.size() && need > 0; i++) {
                    if (remaining[i] <= 0) {
                        continue;
                    }
                    int take = Math.min(Math.min(share, remaining[i]), need);
                    takenByCenter.merge(i, take, Integer::sum);
                    remaining[i] -= take;
                    need -= take;
                }
            }
            for (Map.Entry<Integer, Integer> e : takenByCenter.entrySet()) {
                StockView center = centers.get(e.getKey());
                plan.add(new PlannedAllocation(center.centerId(), center.centerName(),
                        item.resourceId(), item.resourceName(), e.getValue()));
            }
        }
        return plan;
    }
}
