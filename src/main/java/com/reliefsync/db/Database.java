package com.reliefsync.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton owner of the SQLite connection.
 *
 * A single shared connection is sufficient for a desktop application and keeps
 * transaction handling simple: {@link #inTransaction(SqlWork)} toggles
 * auto-commit on the shared connection so every repository call inside the
 * work block participates in the same transaction.
 */
public final class Database {

    private static Database instance;

    private final Connection connection;

    private Database(String jdbcUrl) {
        try {
            connection = DriverManager.getConnection(jdbcUrl);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open database: " + jdbcUrl, e);
        }
    }

    /** Opens (or re-opens) the database at the given URL and applies migrations. */
    public static synchronized Database init(String jdbcUrl) {
        reset();
        instance = new Database(jdbcUrl);
        Migrations.apply(instance.connection);
        return instance;
    }

    /** Opens the standard application database under data/. */
    public static synchronized Database initDefault() {
        new File("data").mkdirs();
        return init("jdbc:sqlite:data/reliefsync.db");
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Database.init(...) has not been called");
        }
        return instance;
    }

    /** Closes and clears the singleton (used on shutdown and between tests). */
    public static synchronized void reset() {
        if (instance != null) {
            try {
                instance.connection.close();
            } catch (SQLException ignored) {
                // closing on shutdown; nothing sensible to do
            }
            instance = null;
        }
    }

    public Connection connection() {
        return connection;
    }

    /** Runs the given work in one transaction, rolling back on any exception. */
    public <T> T inTransaction(SqlWork<T> work) {
        try {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException(e.getMessage(), e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Transaction failed: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws Exception;
    }
}
