package com.reliefsync.model;

public record AffectedArea(long id, String name, String district, int population, int severity, boolean active) {

    @Override
    public String toString() {
        return name + " (" + district + ")";
    }
}
