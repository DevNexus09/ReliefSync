package com.reliefsync.model;

/** Unfulfillable part of a request item after an allocation plan. */
public record Shortage(String resourceName, int missing) {
}
