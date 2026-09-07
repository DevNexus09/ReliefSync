package com.reliefsync.database;

import com.reliefsync.exception.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class TransactionManager {
  private final DatabaseManager databaseManager;

  public TransactionManager(DatabaseManager databaseManager) {
    this.databaseManager =
        Objects.requireNonNull(databaseManager, "Database manager must not be null.");
  }

  public <T> T execute(TransactionWork<T> work) {
    Objects.requireNonNull(work, "Transaction work must not be null.");
    try (Connection connection = databaseManager.openConnection()) {
      connection.setAutoCommit(false);
      try {
        T result = work.execute(connection);
        connection.commit();
        return result;
      } catch (Exception exception) {
        rollback(connection, exception);
        if (exception instanceof PersistenceException persistenceException) {
          throw persistenceException;
        }
        throw new PersistenceException("Database transaction failed.", exception);
      }
    } catch (SQLException exception) {
      throw new PersistenceException("Unable to execute database transaction.", exception);
    }
  }

  private void rollback(Connection connection, Exception original) {
    try {
      connection.rollback();
    } catch (SQLException rollbackFailure) {
      original.addSuppressed(rollbackFailure);
    }
  }
}
