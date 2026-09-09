package com.reliefsync.service;

import com.reliefsync.model.User;
import com.reliefsync.model.Vehicle;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.repository.VehicleRepository;
import java.util.List;

public class VehicleService {

    private final VehicleRepository vehicles = new VehicleRepository();

    public List<Vehicle> search(User actor, String text) {
        AccessControl.require(actor, Feature.VEHICLES);
        return vehicles.search(text);
    }

    public List<Vehicle> available(User actor) {
        AccessControl.require(actor, Feature.TRANSPORT);
        return vehicles.available();
    }

    public long save(User actor, Long idOrNull, String registrationNumber,
                     String vehicleType, int capacity) {
        AccessControl.require(actor, Feature.VEHICLES);
        String registration = requireText(registrationNumber, "Registration number").toUpperCase();
        String type = requireText(vehicleType, "Vehicle type");
        if (capacity <= 0) {
            throw new IllegalArgumentException("Vehicle capacity must be greater than zero");
        }
        if (idOrNull == null) {
            return vehicles.insert(registration, type, capacity);
        }
        vehicles.findById(idOrNull)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle #" + idOrNull + " does not exist"));
        vehicles.update(idOrNull, registration, type, capacity);
        return idOrNull;
    }

    public void setStatus(User actor, long vehicleId, VehicleStatus status) {
        AccessControl.require(actor, Feature.VEHICLES);
        if (status == null || status == VehicleStatus.IN_TRANSIT) {
            throw new IllegalArgumentException("IN_TRANSIT is controlled by dispatch and delivery");
        }
        vehicles.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle #" + vehicleId + " does not exist"));
        vehicles.setStatus(vehicleId, status);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return value.trim();
    }
}
