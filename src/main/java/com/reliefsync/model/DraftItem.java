package com.reliefsync.model;

/** One line of an in-memory request draft, before anything is persisted. */
public record DraftItem(long resourceId, String resourceName, int quantity) {
}
