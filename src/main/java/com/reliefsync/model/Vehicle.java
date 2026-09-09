package com.reliefsync.model;

public record Vehicle(long id, String registrationNumber, String vehicleType,
                      int capacity, VehicleStatus status, String createdAt) {

    @Override
    public String toString() {
        return registrationNumber + " — " + vehicleType + " — capacity " + capacity;
    }
}
