package com.reliefsync.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Application;
import org.junit.jupiter.api.Test;

class ReliefSyncApplicationTest {
    @Test
    void applicationIsAJavaFxApplication() {
        assertTrue(Application.class.isAssignableFrom(ReliefSyncApplication.class));
    }

    @Test
    void requiredViewAndStyleResourcesExist() {
        assertNotNull(ReliefSyncApplication.class.getResource(View.LOGIN.getFxmlPath()));
        assertNotNull(ReliefSyncApplication.class.getResource(View.DASHBOARD.getFxmlPath()));
        assertNotNull(ReliefSyncApplication.class.getResource("/css/application.css"));
    }
}
