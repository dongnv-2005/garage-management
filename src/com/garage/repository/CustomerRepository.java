package com.garage.repository;

import com.garage.config.DatabaseConfig;
import com.garage.models.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerRepository {
    public List<Customer> findAll() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String phone = rs.getString("phone");
                list.add(new Customer(rs.getString("id"), rs.getString("name"), (phone == null || phone.isEmpty()) ? "---" : phone));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Customer> search(String keyword) {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE id LIKE ? OR name LIKE ? OR phone LIKE ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            pstmt.setString(3, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String phone = rs.getString("phone");
                list.add(new Customer(rs.getString("id"), rs.getString("name"), (phone == null || phone.isEmpty()) ? "---" : phone));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public String checkDuplicatePhone(Connection conn, String phone, String excludeId) throws SQLException {
        if (phone == null || phone.trim().isEmpty() || "---".equals(phone.trim())) {
            return null;
        }
        String sql = (excludeId != null && !excludeId.isEmpty())
                ? "SELECT id, name FROM customers WHERE phone = ? AND id != ? LIMIT 1"
                : "SELECT id, name FROM customers WHERE phone = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone.trim());
            if (excludeId != null && !excludeId.isEmpty()) {
                pstmt.setString(2, excludeId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id") + " - " + rs.getString("name");
                }
            }
        }
        return null;
    }

    public void save(Customer customer) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String dupPhone = checkDuplicatePhone(conn, customer.getPhone(), null);
            if (dupPhone != null) {
                throw new SQLException("Số điện thoại '" + customer.getPhone() + "' đã thuộc về khách hàng: " + dupPhone + "!");
            }

            String sql = "INSERT INTO customers (id, name, phone) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, customer.getId());
                pstmt.setString(2, customer.getName());
                pstmt.setString(3, customer.getPhone().isEmpty() ? "---" : customer.getPhone());
                pstmt.executeUpdate();
            }
        }
    }

    public void update(Customer customer) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String dupPhone = checkDuplicatePhone(conn, customer.getPhone(), customer.getId());
            if (dupPhone != null) {
                throw new SQLException("Số điện thoại '" + customer.getPhone() + "' đã thuộc về khách hàng: " + dupPhone + "!");
            }

            String sql = "UPDATE customers SET name = ?, phone = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, customer.getName());
                pstmt.setString(2, customer.getPhone().isEmpty() ? "---" : customer.getPhone());
                pstmt.setString(3, customer.getId());
                pstmt.executeUpdate();
            }
        }
    }

    public String generateNextId() {
        String sql = "SELECT id FROM customers ORDER BY CAST(SUBSTRING(id, 3) AS UNSIGNED) DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int lastNum = Integer.parseInt(rs.getString("id").replaceAll("[^0-9]", ""));
                return String.format("KH%02d", lastNum + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "KH01";
    }
}