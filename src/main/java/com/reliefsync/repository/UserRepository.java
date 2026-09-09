package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.Role;
import com.reliefsync.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserRepository {

    private Connection c() {
        return Database.getInstance().connection();
    }

    public int count() {
        try (Statement st = c().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count users", e);
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, full_name, role FROM users WHERE username = ?";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new User(rs.getLong("id"), rs.getString("username"),
                        rs.getString("full_name"), Role.valueOf(rs.getString("role"))));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load user", e);
        }
    }

    public Optional<String> passwordHash(String username) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load credentials", e);
        }
    }

    public long insert(String username, String fullName, Role role, String passwordHash) {
        String sql = "INSERT INTO users(username, full_name, role, password_hash, created_at) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = c().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, fullName);
            ps.setString(3, role.name());
            ps.setString(4, passwordHash);
            ps.setString(5, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Could not obtain the new user id");
                }
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create user '" + username + "'", e);
        }
    }
}
