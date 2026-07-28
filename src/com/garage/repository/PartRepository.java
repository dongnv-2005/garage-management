package com.garage.repository;

import com.garage.config.DatabaseConfig;
import com.garage.models.Part;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PartRepository {
    public List<Part> findAll() {
        List<Part> list = new ArrayList<>();
        String sql = "SELECT * FROM parts";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Part(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getDouble("import_price"),
                        rs.getDouble("export_price"),
                        rs.getInt("stock_quantity")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Part findById(String id) {
        String sql = "SELECT * FROM parts WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Part(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getDouble("import_price"),
                        rs.getDouble("export_price"),
                        rs.getInt("stock_quantity")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Part findByNameAndImportPrice(String name, double importPrice) {
        String sql = "SELECT * FROM parts WHERE LOWER(name) = LOWER(?) AND import_price = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setDouble(2, importPrice);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Part(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getDouble("import_price"),
                        rs.getDouble("export_price"),
                        rs.getInt("stock_quantity")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void save(Part part) throws SQLException {
        String sql = "INSERT INTO parts (id, name, import_price, export_price, stock_quantity) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, part.getId());
            pstmt.setString(2, part.getName());
            pstmt.setDouble(3, part.getImportPrice());
            pstmt.setDouble(4, part.getExportPrice());
            pstmt.setInt(5, part.getStockQuantity());
            pstmt.executeUpdate();
        }
    }

    public void addStock(String id, int quantity, double exportPrice) throws SQLException {
        String sql = "UPDATE parts SET stock_quantity = stock_quantity + ?, export_price = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setDouble(2, exportPrice);
            pstmt.setString(3, id);
            pstmt.executeUpdate();
        }
    }

    public void reduceStock(String id, int quantity) throws SQLException {
        String sql = "UPDATE parts SET stock_quantity = stock_quantity - ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        }
    }

    public void logPartTransaction(String partId, String partName, int quantity, String usedForVehicle, String createdBy) throws SQLException {
        String sql = "INSERT INTO part_usage_logs (part_id, part_name, quantity, used_for_vehicle, created_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, partId);
            pstmt.setString(2, partName);
            pstmt.setInt(3, quantity);
            pstmt.setString(4, usedForVehicle);
            pstmt.setString(5, createdBy);
            pstmt.executeUpdate();
        }
    }

    public List<Object[]> findUsageLogs() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT p.id, p.part_id, p.part_name, p.quantity, p.used_for_vehicle, COALESCE(u.full_name, p.created_by) AS creator_name, p.created_at " +
                     "FROM part_usage_logs p LEFT JOIN users u ON p.created_by = u.username " +
                     "ORDER BY p.created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String usedFor = rs.getString("used_for_vehicle");
                boolean isImport = "NHẬP KHO".equalsIgnoreCase(usedFor);
                list.add(new Object[]{
                        rs.getInt("id"),
                        rs.getString("part_id"),
                        rs.getString("part_name"),
                        isImport ? "NHẬP KHO" : "XUẤT KHO",
                        rs.getInt("quantity"),
                        isImport ? "---" : usedFor,
                        rs.getString("creator_name"),
                        rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public String generateNextId() {
        String sql = "SELECT id FROM parts ORDER BY CAST(SUBSTRING(id, 3) AS UNSIGNED) DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int lastNum = Integer.parseInt(rs.getString("id").replaceAll("[^0-9]", ""));
                return String.format("PT%02d", lastNum + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "PT01";
    }
}