package com.reliefsync.repository;

import com.reliefsync.model.Vehicle;
import com.reliefsync.model.enums.VehicleStatus;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
  Optional<Vehicle> findById(long id);

  Optional<Vehicle> findByRegistrationNo(String registrationNo);

  List<Vehicle> findAll();

  List<Vehicle> findByStatus(VehicleStatus status);

  long save(Vehicle vehicle);

  void update(Vehicle vehicle);
}
