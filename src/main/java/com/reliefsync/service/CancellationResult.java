package com.reliefsync.service;

/** Summary of a cancellation and any stock reservations released with it. */
public record CancellationResult(int releasedAllocations, long releasedQuantity) {
}
