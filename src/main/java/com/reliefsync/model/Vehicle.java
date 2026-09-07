package com.reliefsync.model;

import com.reliefsync.model.enums.VehicleStatus;

public record Vehicle(
    long id,
    String registrationNo,
    String type,
    long capacity,
    VehicleStatus status,
    Long reliefCenterId,
    boolean active) {}
