package com.reliefsync.model;

public record AllocationItem(
    long id,
    long allocationId,
    long reliefCenterId,
    long resourceId,
    long quantity,
    long dispatchedQuantity,
    long deliveredQuantity) {}
