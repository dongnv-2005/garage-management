package com.garage.repository;

import com.garage.config.DatabaseConfig;
import com.garage.models.Employee;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRepository {
    public List<Employee> findAll() {
        List<Employee> list = new ArrayList<>();

        String adminSql = "SELECT full_name FROM users WHERE role = 'ADMIN' LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(adminSql)) {
            if (rs.next()) {
                list.add(new Employee("ADMIN", rs.getString("full_name"), "---", "---", "Chủ Garage", "Toàn thời gian", 0, 0));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sql = "SELECT * FROM employees ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String empId = rs.getString("id");
                String name = rs.getString("name");
                String phone = rs.getString("phone");
                String cccd = rs.getString("cccd");
                String role = rs.getString("role");
                String shift = rs.getString("shift");

                int shiftCount = getShiftCount(conn, empId, name);
                double rate = role.toLowerCase().contains("lễ tân") ? 400000 : 360000;
                double totalSalary = shiftCount * rate;

                list.add(new Employee(
                        empId, name,
                        (phone == null || phone.isEmpty()) ? "---" : phone,
                        (cccd == null || cccd.isEmpty()) ? "---" : cccd,
                        role, shift, shiftCount, totalSalary
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private int getShiftCount(Connection conn, String empId, String name) {
        String sql = "SELECT COUNT(*) FROM attendance_logs WHERE employee_id = ? OR employee_name LIKE ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, empId);
            pstmt.setString(2, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void save(Employee employee) throws SQLException {
        String sql = "INSERT INTO employees (id, name, phone, cccd, role, shift) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, employee.getId());
            pstmt.setString(2, employee.getName());
            pstmt.setString(3, employee.getPhone().isEmpty() ? "---" : employee.getPhone());
            pstmt.setString(4, employee.getCccd().isEmpty() ? "---" : employee.getCccd());
            pstmt.setString(5, employee.getRole());
            pstmt.setString(6, employee.getShift());
            pstmt.executeUpdate();
        }
    }

    public boolean addAttendance(String targetInput, String operatorFullName) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sqlSelect = "SELECT id, name, shift FROM employees WHERE id = ? OR name LIKE ? OR ? LIKE CONCAT('%', name, '%')";
            PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect);
            pstmtSelect.setString(1, targetInput);
            pstmtSelect.setString(2, "%" + targetInput + "%");
            pstmtSelect.setString(3, operatorFullName != null ? operatorFullName : targetInput);
            ResultSet rs = pstmtSelect.executeQuery();

            if (rs.next()) {
                String empId = rs.getString("id");
                String empName = rs.getString("name");
                String shift = rs.getString("shift");

                String sqlInsert = "INSERT INTO attendance_logs (employee_id, employee_name, shift) VALUES (?, ?, ?)";
                PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert);
                pstmtInsert.setString(1, empId);
                pstmtInsert.setString(2, empName);
                pstmtInsert.setString(3, shift);
                pstmtInsert.executeUpdate();
                return true;
            } else if ("admin".equalsIgnoreCase(targetInput) || (operatorFullName != null && operatorFullName.contains("Chủ Garage"))) {
                String sqlAdmin = "INSERT IGNORE INTO employees (id, name, phone, cccd, role, shift) VALUES ('ADMIN', ?, '---', '---', 'Chủ Garage', 'Toàn thời gian')";
                PreparedStatement pAdmin = conn.prepareStatement(sqlAdmin);
                pAdmin.setString(1, operatorFullName);
                pAdmin.executeUpdate();

                String sqlInsert = "INSERT INTO attendance_logs (employee_id, employee_name, shift) VALUES ('ADMIN', ?, 'Toàn thời gian')";
                PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert);
                pstmtInsert.setString(1, operatorFullName);
                pstmtInsert.executeUpdate();
                return true;
            }
        }
        return false;
    }

    public boolean resetPassword(String targetInput) throws SQLException {
        String sql = "UPDATE users SET password = '123456' WHERE username = ? OR full_name LIKE ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, targetInput);
            pstmt.setString(2, "%" + targetInput + "%");
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Object[]> findAttendanceLogs() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT * FROM attendance_logs ORDER BY check_in_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                        rs.getInt("id"),
                        rs.getString("employee_id"),
                        rs.getString("employee_name"),
                        rs.getString("check_in_time"),
                        rs.getString("shift")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Object[]> findGroupKtvByShift(String shift) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT * FROM employees WHERE shift = ? AND role LIKE '%Kỹ Thuật%'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shift);
            ResultSet rs = pstmt.executeQuery();
            int stt = 1;
            while (rs.next()) {
                String ktvId = rs.getString("id");
                String ktvName = rs.getString("name");
                String lastCheckin = getLastCheckinTime(conn, ktvId, ktvName);
                list.add(new Object[]{ stt++, ktvId, ktvName, rs.getString("role"), rs.getString("shift"), lastCheckin });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Object[]> findKtvAttendanceHistoryByShift(String shift) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT a.id, a.employee_id, a.employee_name, a.check_in_time, a.shift " +
                     "FROM attendance_logs a " +
                     "JOIN employees e ON (a.employee_id = e.id OR a.employee_name LIKE CONCAT('%', e.name, '%')) " +
                     "WHERE e.shift = ? AND e.role LIKE '%Kỹ Thuật%' " +
                     "ORDER BY a.check_in_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shift);
            ResultSet rs = pstmt.executeQuery();
            int stt = 1;
            while (rs.next()) {
                list.add(new Object[]{
                        stt++,
                        rs.getString("employee_id"),
                        rs.getString("employee_name"),
                        rs.getString("check_in_time"),
                        rs.getString("shift")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public String getEmployeeShift(String username, String fullName) {
        String sql = "SELECT shift FROM employees WHERE id = ? OR name LIKE ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, "%" + fullName + "%");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("shift");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Ca 1 (06:00 - 14:00)";
    }

    private String getLastCheckinTime(Connection conn, String ktvId, String ktvName) {
        String sql = "SELECT check_in_time FROM attendance_logs WHERE employee_id = ? OR employee_name LIKE ? ORDER BY check_in_time DESC LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ktvId);
            pstmt.setString(2, "%" + ktvName + "%");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("check_in_time");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Chưa chấm công";
    }

    public String generateNextId() {
        String sql = "SELECT id FROM employees WHERE id LIKE 'NV%' ORDER BY CAST(SUBSTRING(id, 3) AS UNSIGNED) DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int lastNum = Integer.parseInt(rs.getString("id").replaceAll("[^0-9]", ""));
                return String.format("NV%02d", lastNum + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NV01";
    }
}