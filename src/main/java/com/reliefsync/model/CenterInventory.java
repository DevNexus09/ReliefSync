package com.reliefsync.model;

import java.time.LocalDateTime;

public record CenterInventory(
    long id,
    long reliefCenterId,
    long resourceId,
    long totalQuantity,
    long reservedQuantity,
    long dispatchedQuantity,
    LocalDateTime updatedAt) {
  public long getAvailableQuantity() {
    return totalQuantity - reservedQuantity - dispatchedQuantity;
  }
}
