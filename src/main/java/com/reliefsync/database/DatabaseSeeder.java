package com.reliefsync.database;

import com.reliefsync.exception.PersistenceException;
import com.reliefsync.security.PasswordHash;
import com.reliefsync.security.PasswordHasher;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Objects;

public final class DatabaseSeeder {
  public static final String DEMO_USERNAME = "admin";
  public static final String DEMO_PASSWORD = "ReliefSync@2026";
  public static final String CENTER_MANAGER_USERNAME = "center_manager";
  public static final String TRANSPORT_USERNAME = "transport";
  public static final String AREA_COORDINATOR_USERNAME = "area_coordinator";
  public static final String VOLUNTEER_USERNAME = "volunteer";
  public static final String RELIEF_COORDINATOR_USERNAME = "relief_coordinator";
  private static final String BASE_SEED = "/database/seed/base_seed.sql";

  private final TransactionManager transactionManager;
  private final PasswordHasher passwordHasher;

  public DatabaseSeeder(TransactionManager transactionManager, PasswordHasher passwordHasher) {
    this.transactionManager = Objects.requireNonNull(transactionManager);
    this.passwordHasher = Objects.requireNonNull(passwordHasher);
  }

  public void seedDemoData() {
    transactionManager.execute(
        connection -> {
          executeBaseSeed(connection);
          seedUser(connection, "ReliefSync Administrator", DEMO_USERNAME, "ADMINISTRATOR");
          seedUser(
              connection, "Demo Center Manager", CENTER_MANAGER_USERNAME, "RELIEF_CENTER_MANAGER");
          seedUser(
              connection,
              "Demo Transport Coordinator",
              TRANSPORT_USERNAME,
              "TRANSPORT_COORDINATOR");
          seedUser(
              connection, "Demo Area Coordinator", AREA_COORDINATOR_USERNAME, "AREA_COORDINATOR");
          seedUser(connection, "Demo Volunteer", VOLUNTEER_USERNAME, "VOLUNTEER");
          seedUser(
              connection,
              "Demo Relief Coordinator",
              RELIEF_COORDINATOR_USERNAME,
              "RELIEF_COORDINATOR");
          return null;
        });
  }

  private void seedUser(Connection connection, String fullName, String username, String role)
      throws SQLException {
    if (userExists(connection, username)) return;

    PasswordHash credentials = passwordHasher.hash(DEMO_PASSWORD.toCharArray());
    String sql =
"""
INSERT INTO users(full_name, username, password_hash, password_salt, role, active, created_at)
VALUES (?, ?, ?, ?, ?, 1, ?)
""";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, fullName);
      statement.setString(2, username);
      statement.setString(3, credentials.hash());
      statement.setString(4, credentials.salt());
      statement.setString(5, role);
      statement.setString(6, LocalDateTime.now().toString());
      statement.executeUpdate();
    }
  }

  private boolean userExists(Connection connection, String username) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
      statement.setString(1, username);
      try (ResultSet result = statement.executeQuery()) {
        return result.next();
      }
    }
  }

  private void executeBaseSeed(Connection connection) throws SQLException {
    String script = readSeedResource();
    for (String statementText : script.split(";")) {
      String sql = statementText.trim();
      if (!sql.isEmpty()) {
        try (Statement statement = connection.createStatement()) {
          statement.execute(sql);
        }
      }
    }
  }

  private String readSeedResource() {
    try (InputStream stream = DatabaseSeeder.class.getResourceAsStream(BASE_SEED)) {
      if (stream == null)
        throw new PersistenceException("Seed resource was not found: " + BASE_SEED);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new PersistenceException("Unable to read base seed data.", exception);
    }
  }
}
