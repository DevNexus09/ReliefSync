package com.reliefsync.model;

public enum Role {
    ADMIN("Administrator"),
    AREA_COORDINATOR("Area Coordinator"),
    CENTER_MANAGER("Relief Center Manager"),
    TRANSPORT_COORDINATOR("Transport Coordinator"),
    VOLUNTEER("Volunteer"),
    RELIEF_COORDINATOR("Relief Coordinator");

    private final String label;

    Role(String label) {
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
