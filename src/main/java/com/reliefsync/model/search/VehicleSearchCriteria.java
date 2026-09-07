package com.reliefsync.model.search;

import com.reliefsync.model.enums.VehicleStatus;

public record VehicleSearchCriteria(
    String registration, String type, VehicleStatus status, Long reliefCenterId, Boolean active) {}
