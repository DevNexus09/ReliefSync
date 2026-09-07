package com.reliefsync.repository;

public interface InventoryTransactionRepository {
  <T> T execute(InventoryWork<T> work);

  @FunctionalInterface
  interface InventoryWork<T> {
    T execute(InventoryUnitOfWork unitOfWork);
  }
}
