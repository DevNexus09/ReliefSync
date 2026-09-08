package com.reliefsync.model;

public record Resource(long id, String name, String unit, int lowStockThreshold, boolean active) {

    @Override
    public String toString() {
        return name + " (" + unit + ")";
    }
}
