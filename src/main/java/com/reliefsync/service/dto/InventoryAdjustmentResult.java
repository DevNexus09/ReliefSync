package com.reliefsync.service.dto;

import com.reliefsync.model.CenterInventory;

public record InventoryAdjustmentResult(
    CenterInventory inventory, boolean lowStock, long threshold) {}
