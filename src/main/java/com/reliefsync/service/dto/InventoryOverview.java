package com.reliefsync.service.dto;

public record InventoryOverview(
    long inventoryId,
    long centerId,
    String centerName,
    long resourceId,
    String resourceName,
    String category,
    long total,
    long reserved,
    long dispatched,
    long available,
    long threshold,
    boolean lowStock) {}
