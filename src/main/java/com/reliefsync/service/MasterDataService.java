package com.reliefsync.service;

import com.reliefsync.model.AffectedArea;
import com.reliefsync.model.ReliefCenter;
import com.reliefsync.model.Resource;
import com.reliefsync.repository.AreaRepository;
import com.reliefsync.repository.CenterRepository;
import com.reliefsync.repository.ResourceRepository;
import java.util.List;

/** CRUD with validation for the three master-data entities. */
public class MasterDataService {

    private final AreaRepository areas = new AreaRepository();
    private final CenterRepository centers = new CenterRepository();
    private final ResourceRepository resources = new ResourceRepository();

    // ---- Affected areas ----

    public List<AffectedArea> searchAreas(String text) {
        return areas.search(text);
    }

    public List<AffectedArea> activeAreas() {
        return areas.findActive();
    }

    public void saveArea(Long idOrNull, String name, String district, int population, int severity) {
        requireText(name, "Area name");
        requireText(district, "District");
        requireRange(population, 0, Integer.MAX_VALUE, "Population");
        requireRange(severity, 1, 5, "Severity (1-5)");
        if (idOrNull == null) {
            areas.insert(name.trim(), district.trim(), population, severity);
        } else {
            areas.update(idOrNull, name.trim(), district.trim(), population, severity);
        }
    }

    public void setAreaActive(long id, boolean active) {
        areas.setActive(id, active);
    }

    // ---- Relief centers ----

    public List<ReliefCenter> searchCenters(String text) {
        return centers.search(text);
    }

    public List<ReliefCenter> activeCenters() {
        return centers.findActive();
    }

    public void saveCenter(Long idOrNull, String name, String location, int capacity) {
        requireText(name, "Center name");
        requireText(location, "Location");
        requireRange(capacity, 0, Integer.MAX_VALUE, "Capacity");
        if (idOrNull == null) {
            centers.insert(name.trim(), location.trim(), capacity);
        } else {
            centers.update(idOrNull, name.trim(), location.trim(), capacity);
        }
    }

    public void setCenterActive(long id, boolean active) {
        centers.setActive(id, active);
    }

    // ---- Resources ----

    public List<Resource> searchResources(String text) {
        return resources.search(text);
    }

    public List<Resource> activeResources() {
        return resources.findActive();
    }

    public void saveResource(Long idOrNull, String name, String unit, int lowStockThreshold) {
        requireText(name, "Resource name");
        requireText(unit, "Unit");
        requireRange(lowStockThreshold, 0, Integer.MAX_VALUE, "Low-stock threshold");
        if (idOrNull == null) {
            resources.insert(name.trim(), unit.trim(), lowStockThreshold);
        } else {
            resources.update(idOrNull, name.trim(), unit.trim(), lowStockThreshold);
        }
    }

    public void setResourceActive(long id, boolean active) {
        resources.setActive(id, active);
    }

    // ---- Validation helpers ----

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
    }

    private static void requireRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " is out of range");
        }
    }
}
