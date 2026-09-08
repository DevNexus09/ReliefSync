package com.reliefsync.model;

/** One audit entry of a request's status transition. */
public record StatusChange(String fromStatus, String toStatus, String changedBy, String changedAt) {
}
