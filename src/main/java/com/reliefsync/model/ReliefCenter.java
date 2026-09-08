package com.reliefsync.model;

public record ReliefCenter(long id, String name, String location, int capacity, boolean active) {

    @Override
    public String toString() {
        return name;
    }
}
