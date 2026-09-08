package com.reliefsync.model;

/** Requested vs. allocated totals for one affected area. */
public record AreaFulfillment(String areaName, int requested, int allocated) {

    public String percent() {
        if (requested == 0) {
            return "-";
        }
        return (allocated * 100 / requested) + "%";
    }
}
