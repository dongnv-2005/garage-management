package com.garage.repository;

import com.garage.config.DatabaseConfig;
import com.garage.models.Invoice;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceRepository {
    public List<Invoice> findAll() {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT i.*, COALESCE(u.full_name, i.created_by) AS creator_name, COALESCE(c.name, '---') AS customer_name " +
                     "FROM invoices i " +
                     "LEFT JOIN users u ON i.created_by = u.username " +
                     "LEFT JOIN vehicles v ON i.license_plate = v.license_plate " +
                     "LEFT JOIN customers c ON v.owner_id = c.id " +
                     "ORDER BY i.created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Invoice> search(String keyword) {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT i.*, COALESCE(u.full_name, i.created_by) AS creator_name, COALESCE(c.name, '---') AS customer_name " +
                     "FROM invoices i " +
                     "LEFT JOIN users u ON i.created_by = u.username " +
                     "LEFT JOIN vehicles v ON i.license_plate = v.license_plate " +
                     "LEFT JOIN customers c ON v.owner_id = c.id " +
                     "WHERE i.license_plate LIKE ? OR u.full_name LIKE ? OR i.created_by LIKE ? " +
                     "ORDER BY i.created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            pstmt.setString(3, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void save(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (id, license_plate, service_name, part_info, notes, created_by, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, invoice.getId());
            pstmt.setString(2, invoice.getLicensePlate());
            pstmt.setString(3, invoice.getServiceName());
            pstmt.setString(4, invoice.getPartInfo());
            pstmt.setString(5, invoice.getNotes());
            pstmt.setString(6, invoice.getCreatedBy());
            pstmt.setDouble(7, invoice.getTotalAmount());
            pstmt.executeUpdate();
        }
    }

    private Invoice mapResultSet(ResultSet rs) throws SQLException {
        return new Invoice(
                rs.getString("id"),
                rs.getString("license_plate"),
                rs.getString("customer_name"),
                rs.getString("service_name"),
                rs.getString("part_info"),
                rs.getString("notes"),
                rs.getString("creator_name"),
                rs.getDouble("total_amount"),
                rs.getString("created_at")
        );
    }
}