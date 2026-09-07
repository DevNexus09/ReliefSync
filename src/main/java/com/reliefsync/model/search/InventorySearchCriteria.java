package com.reliefsync.model.search;

public record InventorySearchCriteria(
    Long reliefCenterId, Long resourceId, String category, boolean lowStockOnly) {}
