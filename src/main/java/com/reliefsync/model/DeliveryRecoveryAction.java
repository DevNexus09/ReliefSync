package com.reliefsync.model;

public enum DeliveryRecoveryAction {
    RETRY("Retry delivery"),
    REALLOCATE("Return for reallocation");

    private final String label;

    DeliveryRecoveryAction(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
