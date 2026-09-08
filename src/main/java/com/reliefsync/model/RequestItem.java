package com.reliefsync.model;

public record RequestItem(long id, long requestId, long resourceId, String resourceName,
                          int quantityRequested, int quantityAllocated) {

    public int outstanding() {
        return quantityRequested - quantityAllocated;
    }
}
