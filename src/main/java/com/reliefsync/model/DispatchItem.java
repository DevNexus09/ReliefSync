package com.reliefsync.model;

public record DispatchItem(
    long id,
    long dispatchId,
    long allocationItemId,
    long resourceId,
    long quantityDispatched,
    long quantityDelivered) {}
