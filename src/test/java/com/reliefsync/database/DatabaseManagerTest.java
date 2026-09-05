package com.reliefsync.database;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void selectOneSucceeds() throws Exception {
        DatabaseManager manager = managerForTemporaryDatabase();
        assertTrue(manager.isConnectionValid());
    }

    @Test
    void sqliteVersionIsReported() throws Exception {
        DatabaseManager manager = managerForTemporaryDatabase();
        assertFalse(manager.getSqliteVersion().isBlank());
    }

    private DatabaseManager managerForTemporaryDatabase() {
        Path databasePath = temporaryDirectory.resolve("reliefsync-test.db");
        return new DatabaseManager(new DatabaseConfig(databasePath));
    }
}
