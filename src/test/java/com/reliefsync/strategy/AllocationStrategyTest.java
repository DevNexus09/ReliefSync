package com.reliefsync.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.model.PlannedAllocation;
import com.reliefsync.model.RequestItem;
import com.reliefsync.model.StockView;
import java.util.List;
import org.junit.jupiter.api.Test;

class AllocationStrategyTest {

    private static final long WATER = 10;

    private static RequestItem need(int quantity) {
        return new RequestItem(1, 1, WATER, "Water", quantity, 0);
    }

    private static StockView stock(long centerId, String name, int quantity) {
        return new StockView(centerId, name, WATER, "Water", quantity, 5);
    }

    private static int totalFor(List<PlannedAllocation> plan, long centerId) {
        return plan.stream().filter(p -> p.centerId() == centerId)
                .mapToInt(PlannedAllocation::quantity).sum();
    }

    @Test
    void concentratedDrainsLargestCenterFirst() {
        List<PlannedAllocation> plan = new ConcentratedStrategy().plan(
                List.of(need(120)),
                List.of(stock(1, "A", 100), stock(2, "B", 50)));
        assertEquals(100, totalFor(plan, 1));
        assertEquals(20, totalFor(plan, 2));
    }

    @Test
    void balancedSplitsAcrossCenters() {
        List<PlannedAllocation> plan = new BalancedStrategy().plan(
                List.of(need(60)),
                List.of(stock(1, "A", 100), stock(2, "B", 50)));
        assertEquals(30, totalFor(plan, 1));
        assertEquals(30, totalFor(plan, 2));
    }

    @Test
    void balancedTopsUpWhenACenterRunsDry() {
        List<PlannedAllocation> plan = new BalancedStrategy().plan(
                List.of(need(90)),
                List.of(stock(1, "A", 100), stock(2, "B", 20)));
        assertEquals(70, totalFor(plan, 1));
        assertEquals(20, totalFor(plan, 2));
    }

    @Test
    void shortagesLeaveThePlanPartial() {
        for (AllocationStrategy strategy : List.of(new ConcentratedStrategy(), new BalancedStrategy())) {
            List<PlannedAllocation> plan = strategy.plan(
                    List.of(need(500)),
                    List.of(stock(1, "A", 40), stock(2, "B", 10)));
            int total = plan.stream().mapToInt(PlannedAllocation::quantity).sum();
            assertEquals(50, total, strategy.name() + " should allocate all available stock");
        }
    }

    @Test
    void alreadyAllocatedItemsAreSkipped() {
        RequestItem satisfied = new RequestItem(1, 1, WATER, "Water", 50, 50);
        for (AllocationStrategy strategy : List.of(new ConcentratedStrategy(), new BalancedStrategy())) {
            assertTrue(strategy.plan(List.of(satisfied), List.of(stock(1, "A", 100))).isEmpty());
        }
    }
}
