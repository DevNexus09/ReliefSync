package com.reliefsync.model;

/** Lifecycle states of a relief request; behavior per state lives in com.reliefsync.state. */
public enum RequestStatus {
    DRAFT, SUBMITTED, VERIFIED, REJECTED, ALLOCATED, DISPATCHED, DELIVERY_FAILED, DELIVERED, CANCELLED
}
