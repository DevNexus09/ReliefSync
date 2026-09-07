package com.reliefsync.database;

import java.sql.Connection;

@FunctionalInterface
public interface TransactionWork<T> {
  T execute(Connection connection) throws Exception;
}
