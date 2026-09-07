package com.reliefsync.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefsync.database.DatabaseManager;
import com.reliefsync.exception.PersistenceException;
import com.reliefsync.model.*;
import com.reliefsync.model.Resource;
import com.reliefsync.model.enums.*;
import com.reliefsync.repository.sqlite.*;
import com.reliefsync.support.TestDatabase;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoreRepositoryTest {
  @TempDir Path directory;
  private DatabaseManager manager;
  private SQLiteUserRepository users;

  @BeforeEach
  void setUp() {
    manager = TestDatabase.migrated(directory);
    users = new SQLiteUserRepository(manager);
  }

  @Test
  void givenValidUser_whenSavedFoundAndUpdated_thenValuesPersist() {
    long id = users.save(user(0, "Coordinator", "coord", true));
    User saved = users.findById(id).orElseThrow();
    assertEquals("coord", saved.username());
    assertTrue(users.findByUsername("COORD").isPresent());

    users.update(
        new User(
            id,
            "Updated Coordinator",
            "coord",
            "hash2",
            "salt2",
            Role.RELIEF_COORDINATOR,
            false,
            saved.createdAt()));

    User updated = users.findById(id).orElseThrow();
    assertEquals("Updated Coordinator", updated.fullName());
    assertFalse(updated.active());
    assertTrue(users.existsByUsername("coord"));
    assertEquals(1, users.findAll().size());
  }

  @Test
  void givenExistingUsername_whenCaseVariantSaved_thenUniqueConstraintIsReported() {
    users.save(user(0, "First", "admin", true));
    assertThrows(PersistenceException.class, () -> users.save(user(0, "Second", "ADMIN", true)));
  }

  @Test
  void givenMissingCreator_whenDisasterSaved_thenForeignKeyViolationIsReported() {
    SQLiteDisasterEventRepository disasters = new SQLiteDisasterEventRepository(manager);
    DisasterEvent event =
        new DisasterEvent(
            0,
            "Flood",
            DisasterType.FLOOD,
            null,
            LocalDate.now(),
            null,
            DisasterStatus.ACTIVE,
            999_999,
            LocalDateTime.now());
    assertThrows(PersistenceException.class, () -> disasters.save(event));
  }

  @Test
  void givenValidInventory_whenSaved_thenItCanBeFoundAndAvailableIsDerived() {
    long[] ids = createCenterAndResource();
    SQLiteCenterInventoryRepository inventory = new SQLiteCenterInventoryRepository(manager);
    long id =
        inventory.save(new CenterInventory(0, ids[0], ids[1], 100, 20, 10, LocalDateTime.now()));

    CenterInventory saved = inventory.findByCenterAndResource(ids[0], ids[1]).orElseThrow();
    assertEquals(id, saved.id());
    assertEquals(70, saved.getAvailableQuantity());
    assertEquals(1, inventory.findByReliefCenterId(ids[0]).size());
    assertEquals(1, inventory.findByResourceId(ids[1]).size());
  }

  @Test
  void givenInvalidInventoryQuantities_whenSaved_thenChecksRejectThem() {
    long[] ids = createCenterAndResource();
    SQLiteCenterInventoryRepository inventory = new SQLiteCenterInventoryRepository(manager);
    assertThrows(
        PersistenceException.class,
        () ->
            inventory.save(new CenterInventory(0, ids[0], ids[1], -1, 0, 0, LocalDateTime.now())));
    assertThrows(
        PersistenceException.class,
        () ->
            inventory.save(
                new CenterInventory(0, ids[0], ids[1], 100, 80, 30, LocalDateTime.now())));
  }

  @Test
  void givenExistingCenterResourceInventory_whenDuplicateSaved_thenUniqueIndexRejectsIt() {
    long[] ids = createCenterAndResource();
    SQLiteCenterInventoryRepository inventory = new SQLiteCenterInventoryRepository(manager);
    CenterInventory row = new CenterInventory(0, ids[0], ids[1], 100, 0, 0, LocalDateTime.now());
    inventory.save(row);
    assertThrows(PersistenceException.class, () -> inventory.save(row));
  }

  @Test
  void givenMissingCenter_whenInventorySaved_thenForeignKeyRejectsIt() {
    long resourceId =
        new SQLiteResourceRepository(manager)
            .save(new Resource(0, "Water", "Water", "bottle", 10, true));
    assertThrows(
        PersistenceException.class,
        () ->
            new SQLiteCenterInventoryRepository(manager)
                .save(new CenterInventory(0, 999_999, resourceId, 10, 0, 0, LocalDateTime.now())));
  }

  private long[] createCenterAndResource() {
    long centerId =
        new SQLiteReliefCenterRepository(manager)
            .save(
                new ReliefCenter(
                    0, "Central Depot", "Dhaka", 23.8, 90.4, "0123", true, LocalDateTime.now()));
    long resourceId =
        new SQLiteResourceRepository(manager)
            .save(new Resource(0, "Food", "Food", "package", 10, true));
    return new long[] {centerId, resourceId};
  }

  private User user(long id, String name, String username, boolean active) {
    return new User(
        id, name, username, "hash", "salt", Role.ADMINISTRATOR, active, LocalDateTime.now());
  }
}
