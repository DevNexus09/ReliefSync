package com.reliefsync.repository;

import com.reliefsync.db.Database;
import com.reliefsync.model.Vehicle;
import com.reliefsync.model.VehicleStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleRepository {

    private Connection c() {
        return Database.getInstance().connection();
    }

    private Vehicle map(ResultSet rs) throws SQLException {
        return new Vehicle(rs.getLong("id"), rs.getString("registration_number"),
                rs.getString("vehicle_type"), rs.getInt("capacity"),
                VehicleStatus.valueOf(rs.getString("status")), rs.getString("created_at"));
    }

    public List<Vehicle> search(String text) {
        String sql = "SELECT * FROM vehicles WHERE registration_number LIKE ? OR vehicle_type LIKE ?"
                + " ORDER BY registration_number LIMIT 200";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            String value = "%" + (text == null ? "" : text.trim()) + "%";
            ps.setString(1, value);
            ps.setString(2, value);
            try (ResultSet rs = ps.executeQuery()) {
                List<Vehicle> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not search vehicles", e);
        }
    }

    public List<Vehicle> available() {
        return search("").stream().filter(v -> v.status() == VehicleStatus.AVAILABLE).toList();
    }

    public Optional<Vehicle> findById(long id) {
        try (PreparedStatement ps = c().prepareStatement("SELECT * FROM vehicles WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load the vehicle", e);
        }
    }

    public Optional<Vehicle> findByRegistration(String registrationNumber) {
        try (PreparedStatement ps = c().prepareStatement(
                "SELECT * FROM vehicles WHERE registration_number=?")) {
            ps.setString(1, registrationNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load the vehicle", e);
        }
    }

    public long insert(String registrationNumber, String vehicleType, int capacity) {
        String sql = "INSERT INTO vehicles(registration_number, vehicle_type, capacity, status, created_at)"
                + " VALUES (?,?,?,'AVAILABLE',?)";
        try (PreparedStatement ps = c().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, registrationNumber);
            ps.setString(2, vehicleType);
            ps.setInt(3, capacity);
            ps.setString(4, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create vehicle (is the registration unique?)", e);
        }
    }

    public void update(long id, String registrationNumber, String vehicleType, int capacity) {
        String sql = "UPDATE vehicles SET registration_number=?, vehicle_type=?, capacity=?"
                + " WHERE id=? AND status <> 'IN_TRANSIT'";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, registrationNumber);
            ps.setString(2, vehicleType);
            ps.setInt(3, capacity);
            ps.setLong(4, id);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("An in-transit vehicle cannot be edited");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update vehicle (is the registration unique?)", e);
        }
    }

    public boolean claim(long id) {
        try (PreparedStatement ps = c().prepareStatement(
                "UPDATE vehicles SET status='IN_TRANSIT' WHERE id=? AND status='AVAILABLE'")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not claim the vehicle", e);
        }
    }

    public boolean release(long id) {
        try (PreparedStatement ps = c().prepareStatement(
                "UPDATE vehicles SET status='AVAILABLE' WHERE id=? AND status='IN_TRANSIT'")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not release the vehicle", e);
        }
    }

    public void setStatus(long id, VehicleStatus status) {
        String sql = "UPDATE vehicles SET status=? WHERE id=? AND status <> 'IN_TRANSIT'";
        try (PreparedStatement ps = c().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("An in-transit vehicle cannot be changed manually");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not change vehicle status", e);
        }
    }
}
