package com.reliefsync.model;

public record ReliefRequestItem(
    long id,
    long requestId,
    long resourceId,
    long requestedQuantity,
    long allocatedQuantity,
    long deliveredQuantity) {
  public long getRemainingQuantity() {
    return Math.max(0, requestedQuantity - deliveredQuantity);
  }

  public double getFulfillmentPercentage() {
    return requestedQuantity == 0 ? 0.0 : deliveredQuantity * 100.0 / requestedQuantity;
  }
}
