package com.reliefsync.strategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry of available allocation strategies, keyed by display name. */
public final class AllocationStrategies {

    private static final Map<String, AllocationStrategy> BY_NAME = new LinkedHashMap<>();

    static {
        register(new ConcentratedStrategy());
        register(new BalancedStrategy());
    }

    private AllocationStrategies() {
    }

    private static void register(AllocationStrategy strategy) {
        BY_NAME.put(strategy.name(), strategy);
    }

    public static List<String> names() {
        return List.copyOf(BY_NAME.keySet());
    }

    public static AllocationStrategy byName(String name) {
        AllocationStrategy strategy = BY_NAME.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown allocation strategy: " + name);
        }
        return strategy;
    }
}
