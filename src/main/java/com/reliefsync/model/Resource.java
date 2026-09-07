package com.reliefsync.model;

public record Resource(
    long id,
    String name,
    String category,
    String unit,
    long minimumStockThreshold,
    boolean active) {}
