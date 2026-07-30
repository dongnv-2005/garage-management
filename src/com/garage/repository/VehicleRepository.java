package com.garage.repository;

import com.garage.config.DatabaseConfig;
import com.garage.enums.RepairStatus;
import com.garage.models.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleRepository {
    public List<Vehicle> findAll() {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT v.*, COALESCE(c.name, '---') AS owner_name FROM vehicles v LEFT JOIN customers c ON v.owner_id = c.id";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String model = rs.getString("model");
                list.add(new Vehicle(
                        rs.getString("license_plate"),
                        rs.getString("brand"),
                        (model == null || model.isEmpty()) ? "---" : model,
                        rs.getString("owner_id"),
                        rs.getString("owner_name"),
                        RepairStatus.valueOf(rs.getString("status"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Vehicle> search(String keyword) {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT v.*, COALESCE(c.name, '---') AS owner_name FROM vehicles v LEFT JOIN customers c ON v.owner_id = c.id " +
                     "WHERE v.license_plate LIKE ? OR v.brand LIKE ? OR v.model LIKE ? OR v.owner_id LIKE ? OR c.name LIKE ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            pstmt.setString(3, "%" + keyword + "%");
            pstmt.setString(4, "%" + keyword + "%");
            pstmt.setString(5, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String model = rs.getString("model");
                list.add(new Vehicle(
                        rs.getString("license_plate"),
                        rs.getString("brand"),
                        (model == null || model.isEmpty()) ? "---" : model,
                        rs.getString("owner_id"),
                        rs.getString("owner_name"),
                        RepairStatus.valueOf(rs.getString("status"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void save(Vehicle vehicle) throws SQLException {
        String sql = "INSERT INTO vehicles (license_plate, brand, model, owner_id, status) VALUES (?, ?, ?, ?, 'WAITING')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vehicle.getLicensePlate());
            pstmt.setString(2, vehicle.getBrand());
            pstmt.setString(3, vehicle.getModel().isEmpty() ? "---" : vehicle.getModel());
            pstmt.setString(4, vehicle.getOwnerId());
            pstmt.executeUpdate();
        }
    }

    public void updateStatus(String licensePlate, RepairStatus status) throws SQLException {
        String sql = "UPDATE vehicles SET status = ? WHERE license_plate = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, licensePlate);
            pstmt.executeUpdate();
        }
    }
}