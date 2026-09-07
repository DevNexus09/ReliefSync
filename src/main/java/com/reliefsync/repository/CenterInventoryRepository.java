package com.reliefsync.repository;

import com.reliefsync.model.CenterInventory;
import java.util.List;
import java.util.Optional;

public interface CenterInventoryRepository {
  Optional<CenterInventory> findById(long id);

  Optional<CenterInventory> findByCenterAndResource(long reliefCenterId, long resourceId);

  List<CenterInventory> findByReliefCenterId(long reliefCenterId);

  List<CenterInventory> findByResourceId(long resourceId);

  List<CenterInventory> findAll();

  long save(CenterInventory inventory);

  void update(CenterInventory inventory);
}
