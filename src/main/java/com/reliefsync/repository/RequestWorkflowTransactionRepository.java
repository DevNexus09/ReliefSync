package com.reliefsync.repository;

public interface RequestWorkflowTransactionRepository {
  <T> T execute(Work<T> work);

  @FunctionalInterface
  interface Work<T> {
    T execute(RequestWorkflowUnitOfWork unit);
  }
}
