package com.reliefsync.model;

/** Flattened request row for tables (area and creator resolved to names). */
public record RequestRow(long id, String areaName, Priority priority, RequestStatus status,
                         String createdAt, String createdByName, String note) {
}
