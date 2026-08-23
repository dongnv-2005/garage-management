package com.garage.repository;

import com.garage.config.DatabaseConfig;
import com.garage.models.Employee;

import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class EmployeeRepository {
    public List<Employee> findAll() {
        List<Employee> list = new ArrayList<>();

        String adminSql = "SELECT full_name FROM users WHERE role = 'ADMIN' LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureAssignmentColumnsExist(conn);

            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(adminSql)) {
                if (rs.next()) {
                    Employee adminEmp = new Employee("ADMIN", rs.getString("full_name"), "---", "---", "Chủ Garage",
                            "Toàn thời gian", 0, 0);
                    adminEmp.setNotes(getNotesByEmpId(conn, "ADMIN"));
                    list.add(adminEmp);
                }
            }

            String sql = "SELECT * FROM employees WHERE id != 'ADMIN' ORDER BY id ASC";
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String empId = rs.getString("id");
                    String name = rs.getString("name");
                    String phone = rs.getString("phone");
                    String cccd = rs.getString("cccd");
                    String role = rs.getString("role");
                    String shift = rs.getString("shift");
                    String notes = rs.getString("notes");
                    String managedBy = null;
                    boolean isNotified = false;
                    try {
                        managedBy = rs.getString("managed_by");
                        isNotified = rs.getBoolean("is_notified");
                    } catch (Exception ignored) {
                    }

                    int shiftCount = getShiftCount(conn, empId);
                    double rate = role.toLowerCase().contains("lễ tân") ? 400000 : 360000;
                    double totalSalary = shiftCount * rate;

                    Employee emp = new Employee(
                            empId, name,
                            (phone == null || phone.isEmpty()) ? "---" : phone,
                            (cccd == null || cccd.isEmpty()) ? "---" : cccd,
                            role, shift, shiftCount, totalSalary);
                    emp.setNotes((notes == null || notes.isEmpty()) ? "---" : notes);
                    emp.setManagedBy(managedBy);
                    emp.setNotified(isNotified);
                    list.add(emp);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void ensureAssignmentColumnsExist(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("ALTER TABLE employees ADD COLUMN IF NOT EXISTS managed_by VARCHAR(50) DEFAULT NULL");
                stmt.execute("ALTER TABLE employees ADD COLUMN IF NOT EXISTS is_notified BOOLEAN DEFAULT FALSE");
            } catch (SQLException e) {
                try {
                    stmt.execute("ALTER TABLE employees ADD COLUMN managed_by VARCHAR(50) DEFAULT NULL");
                } catch (Exception ignored) {
                }
                try {
                    stmt.execute("ALTER TABLE employees ADD COLUMN is_notified BOOLEAN DEFAULT FALSE");
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String getNotesByEmpId(Connection conn, String empId) {
        String sql = "SELECT notes FROM employees WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, empId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String n = rs.getString("notes");
                return (n == null || n.isEmpty()) ? "---" : n;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "---";
    }

    private int getShiftCount(Connection conn, String empId) {
        String sql = "SELECT COUNT(*) FROM attendance_logs WHERE employee_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, empId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String findBestReceptionistForKtv(Connection conn, String shift) {
        List<String> receptionists = new ArrayList<>();

        String sql = "SELECT id FROM employees WHERE role LIKE '%Lễ Tân%' AND shift = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shift);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                receptionists.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (receptionists.isEmpty()) {
            String shiftKeyword = shift.contains("1") ? "%1%" : (shift.contains("2") ? "%2%" : "%");
            String sqlFallback = "SELECT id FROM employees WHERE role LIKE '%Lễ Tân%' AND shift LIKE ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlFallback)) {
                pstmt.setString(1, shiftKeyword);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    receptionists.add(rs.getString("id"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (receptionists.isEmpty()) {
            String sqlAll = "SELECT id FROM employees WHERE role LIKE '%Lễ Tân%'";
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sqlAll)) {
                while (rs.next()) {
                    receptionists.add(rs.getString("id"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (receptionists.isEmpty())
            return null;

        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        int minCount = Integer.MAX_VALUE;
        for (String recId : receptionists) {
            int count = 0;
            String countSql = "SELECT COUNT(*) FROM employees WHERE managed_by = ? AND role LIKE '%Kỹ Thuật%'";
            try (PreparedStatement pCount = conn.prepareStatement(countSql)) {
                pCount.setString(1, recId);
                ResultSet rs = pCount.executeQuery();
                if (rs.next())
                    count = rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            counts.put(recId, count);
            if (count < minCount) {
                minCount = count;
            }
        }

        List<String> minCandidates = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == minCount) {
                minCandidates.add(entry.getKey());
            }
        }

        if (minCandidates.size() == 1) {
            return minCandidates.get(0);
        } else if (!minCandidates.isEmpty()) {
            return minCandidates.get(new java.util.Random().nextInt(minCandidates.size()));
        }
        return receptionists.get(0);
    }

    public void rebalanceKtvsForShift(Connection conn, String shift) {
        if (shift == null || shift.trim().isEmpty()) return;
        try {
            ensureAssignmentColumnsExist(conn);

            List<String> receptionists = new ArrayList<>();
            String shiftLike = shift.contains("1") ? "%1%" : (shift.contains("2") ? "%2%" : "%" + shift + "%");
            String recSql = "SELECT id FROM employees WHERE role LIKE '%Lễ Tân%' AND (shift = ? OR shift LIKE ?) ORDER BY id ASC";
            try (PreparedStatement pstmt = conn.prepareStatement(recSql)) {
                pstmt.setString(1, shift);
                pstmt.setString(2, shiftLike);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("id");
                        if (!receptionists.contains(id)) {
                            receptionists.add(id);
                        }
                    }
                }
            }

            List<String> ktvs = new ArrayList<>();
            String ktvSql = "SELECT id FROM employees WHERE role LIKE '%Kỹ Thuật%' AND (shift = ? OR shift LIKE ?) ORDER BY id ASC";
            try (PreparedStatement pstmt = conn.prepareStatement(ktvSql)) {
                pstmt.setString(1, shift);
                pstmt.setString(2, shiftLike);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("id");
                        if (!ktvs.contains(id)) {
                            ktvs.add(id);
                        }
                    }
                }
            }

            if (receptionists.isEmpty() || ktvs.isEmpty()) {
                return;
            }

            // Trộn ngẫu nhiên danh sách KTV trong ca để phân công công bằng và ngẫu nhiên
            java.util.Collections.shuffle(ktvs, new java.util.Random());

            // Phân bổ KTV cho các Lễ tân trong cùng ca
            String updateSql = "UPDATE employees SET managed_by = ?, is_notified = TRUE WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                for (int i = 0; i < ktvs.size(); i++) {
                    String assignedRecId = receptionists.get(i % receptionists.size());
                    pstmt.setString(1, assignedRecId);
                    pstmt.setString(2, ktvs.get(i));
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void autoAssignUnassignedKtvs() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureAssignmentColumnsExist(conn);
            rebalanceKtvsForShift(conn, "Ca 1 (06:00 - 14:00)");
            rebalanceKtvsForShift(conn, "Ca 2 (14:00 - 22:00)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getUnnotifiedKtvsForReceptionist(String receptionistId) {
        List<String> list = new ArrayList<>();
        if (receptionistId == null || receptionistId.isEmpty())
            return list;

        String sql = "SELECT name FROM employees WHERE managed_by = ? AND is_notified = FALSE AND role LIKE '%Kỹ Thuật%'";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receptionistId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void markKtvsAsNotified(String receptionistId) {
        if (receptionistId == null || receptionistId.isEmpty())
            return;

        String sql = "UPDATE employees SET is_notified = TRUE WHERE managed_by = ? AND is_notified = FALSE";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receptionistId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getEmployeeIdByUsernameOrName(String username, String fullName) {
        if (username == null) return "ADMIN";
        if ("admin".equalsIgnoreCase(username)) return "ADMIN";

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Ưu tiên 1: Tìm theo ID chính xác hoặc tài khoản được ghi trong notes
            String sql1 = "SELECT id FROM employees WHERE id = ? OR notes LIKE ? ORDER BY id DESC LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql1)) {
                pstmt.setString(1, username);
                pstmt.setString(2, "%" + username + "%");
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("id");
                    }
                }
            }

            // Ưu tiên 2: Tìm theo họ tên
            if (fullName != null && !fullName.trim().isEmpty()) {
                String sql2 = "SELECT id FROM employees WHERE name = ? ORDER BY id DESC LIMIT 1";
                try (PreparedStatement pstmt = conn.prepareStatement(sql2)) {
                    pstmt.setString(1, fullName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("id");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return username;
    }

    public static String removeDiacritics(String str) {
        if (str == null) return "";
        String n = str.replace("đ", "d").replace("Đ", "D");
        String nfd = Normalizer.normalize(n, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").toLowerCase().trim();
    }

    public static String generateBaseUsername(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "letan";
        }
        String normalized = removeDiacritics(fullName);
        String clean = normalized.replaceAll("[^a-z0-9\\s]", " ").trim();
        String[] parts = clean.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "letan";
        }
        if (parts.length == 1) {
            return parts[0];
        }

        StringBuilder sb = new StringBuilder(parts[parts.length - 1]);
        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(parts[i].charAt(0));
            }
        }
        return sb.toString();
    }

    public String findUniqueUsername(Connection conn, String baseUsername) throws SQLException {
        Set<String> existingUsernames = new HashSet<>();
        String sql = "SELECT username FROM users WHERE username = ? OR username LIKE ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, baseUsername);
            pstmt.setString(2, baseUsername + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    existingUsernames.add(rs.getString("username").toLowerCase());
                }
            }
        }

        if (!existingUsernames.contains(baseUsername.toLowerCase())) {
            return baseUsername;
        }

        int counter = 1;
        while (true) {
            String candidate = String.format("%s%03d", baseUsername, counter);
            if (!existingUsernames.contains(candidate.toLowerCase())) {
                return candidate;
            }
            counter++;
        }
    }

    public String createReceptionistUser(Connection conn, String fullName) throws SQLException {
        String baseUsername = generateBaseUsername(fullName);
        String uniqueUsername = findUniqueUsername(conn, baseUsername);

        String insertUserSql = "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertUserSql)) {
            pstmt.setString(1, uniqueUsername);
            pstmt.setString(2, "123456");
            pstmt.setString(3, fullName);
            pstmt.setString(4, "RECEPTIONIST");
            pstmt.executeUpdate();
        }
        return uniqueUsername;
    }

    public String checkDuplicatePhone(Connection conn, String phone, String excludeEmpId) throws SQLException {
        if (phone == null || phone.trim().isEmpty() || "---".equals(phone.trim())) {
            return null;
        }
        String sql = (excludeEmpId != null && !excludeEmpId.isEmpty())
                ? "SELECT id, name FROM employees WHERE phone = ? AND id != ? LIMIT 1"
                : "SELECT id, name FROM employees WHERE phone = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone.trim());
            if (excludeEmpId != null && !excludeEmpId.isEmpty()) {
                pstmt.setString(2, excludeEmpId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id") + " - " + rs.getString("name");
                }
            }
        }
        return null;
    }

    public String checkDuplicateCccd(Connection conn, String cccd, String excludeEmpId) throws SQLException {
        if (cccd == null || cccd.trim().isEmpty() || "---".equals(cccd.trim())) {
            return null;
        }
        String sql = (excludeEmpId != null && !excludeEmpId.isEmpty())
                ? "SELECT id, name FROM employees WHERE cccd = ? AND id != ? LIMIT 1"
                : "SELECT id, name FROM employees WHERE cccd = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cccd.trim());
            if (excludeEmpId != null && !excludeEmpId.isEmpty()) {
                pstmt.setString(2, excludeEmpId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id") + " - " + rs.getString("name");
                }
            }
        }
        return null;
    }

    public String save(Employee employee) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureAssignmentColumnsExist(conn);

            String dupPhone = checkDuplicatePhone(conn, employee.getPhone(), null);
            if (dupPhone != null) {
                throw new SQLException("Số điện thoại '" + employee.getPhone() + "' đã tồn tại (thuộc về nhân viên: " + dupPhone + ")!");
            }

            String dupCccd = checkDuplicateCccd(conn, employee.getCccd(), null);
            if (dupCccd != null) {
                throw new SQLException("Số CCCD '" + employee.getCccd() + "' đã tồn tại (thuộc về nhân viên: " + dupCccd + ")!");
            }

            String managedBy = employee.getManagedBy();
            boolean isNotified = employee.isNotified();

            if (employee.getRole() != null && employee.getRole().toLowerCase().contains("kỹ thuật")) {
                if (managedBy == null || managedBy.isEmpty()) {
                    managedBy = findBestReceptionistForKtv(conn, employee.getShift());
                    isNotified = false;
                }
            }

            String createdUsername = null;
            if (employee.getRole() != null && (employee.getRole().toLowerCase().contains("lễ tân") || employee.getRole().toLowerCase().contains("receptionist"))) {
                createdUsername = createReceptionistUser(conn, employee.getName());
                if (employee.getNotes() == null || employee.getNotes().trim().isEmpty() || "---".equals(employee.getNotes().trim())) {
                    employee.setNotes("Tài khoản: " + createdUsername);
                }
            }

            String sql = "INSERT INTO employees (id, name, phone, cccd, role, shift, notes, managed_by, is_notified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, employee.getId());
                pstmt.setString(2, employee.getName());
                pstmt.setString(3, employee.getPhone().isEmpty() ? "---" : employee.getPhone());
                pstmt.setString(4, employee.getCccd().isEmpty() ? "---" : employee.getCccd());
                pstmt.setString(5, employee.getRole());
                pstmt.setString(6, employee.getShift());
                pstmt.setString(7, employee.getNotes().isEmpty() ? "---" : employee.getNotes());
                pstmt.setString(8, managedBy);
                pstmt.setBoolean(9, isNotified);
                pstmt.executeUpdate();
            }

            if (employee.getRole() != null && (employee.getRole().toLowerCase().contains("lễ tân") || employee.getRole().toLowerCase().contains("kỹ thuật"))) {
                rebalanceKtvsForShift(conn, employee.getShift());
            }

            return createdUsername;
        }
    }

    public void update(Employee employee) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureAssignmentColumnsExist(conn);

            String dupPhone = checkDuplicatePhone(conn, employee.getPhone(), employee.getId());
            if (dupPhone != null) {
                throw new SQLException("Số điện thoại '" + employee.getPhone() + "' đã tồn tại (thuộc về nhân viên: " + dupPhone + ")!");
            }

            String dupCccd = checkDuplicateCccd(conn, employee.getCccd(), employee.getId());
            if (dupCccd != null) {
                throw new SQLException("Số CCCD '" + employee.getCccd() + "' đã tồn tại (thuộc về nhân viên: " + dupCccd + ")!");
            }

            String sql = "UPDATE employees SET name = ?, phone = ?, cccd = ?, role = ?, shift = ?, notes = ?, managed_by = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, employee.getName());
                pstmt.setString(2, employee.getPhone().isEmpty() ? "---" : employee.getPhone());
                pstmt.setString(3, employee.getCccd().isEmpty() ? "---" : employee.getCccd());
                pstmt.setString(4, employee.getRole());
                pstmt.setString(5, employee.getShift());
                pstmt.setString(6, employee.getNotes().isEmpty() ? "---" : employee.getNotes());
                pstmt.setString(7, employee.getManagedBy());
                pstmt.setString(8, employee.getId());
                pstmt.executeUpdate();
            }

            if (employee.getRole() != null && (employee.getRole().toLowerCase().contains("lễ tân") || employee.getRole().toLowerCase().contains("kỹ thuật"))) {
                rebalanceKtvsForShift(conn, employee.getShift());
            }
        }
    }

    public void delete(String id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String role = null;
            String shift = null;
            String sqlFind = "SELECT role, shift FROM employees WHERE id = ?";
            try (PreparedStatement pFind = conn.prepareStatement(sqlFind)) {
                pFind.setString(1, id);
                try (ResultSet rs = pFind.executeQuery()) {
                    if (rs.next()) {
                        role = rs.getString("role");
                        shift = rs.getString("shift");
                    }
                }
            }

            String sql = "DELETE FROM employees WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.executeUpdate();
            }

            if (role != null && (role.toLowerCase().contains("lễ tân") || role.toLowerCase().contains("kỹ thuật")) && shift != null) {
                rebalanceKtvsForShift(conn, shift);
            }
        }
    }

    public boolean addAttendance(String targetInput, String operatorFullName) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (targetInput == null || targetInput.trim().isEmpty()) return false;

            String sqlSelect = "SELECT id, name, shift FROM employees WHERE id = ? OR notes LIKE ? OR (name = ? AND id != 'ADMIN') LIMIT 1";
            try (PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect)) {
                pstmtSelect.setString(1, targetInput);
                pstmtSelect.setString(2, "%" + targetInput + "%");
                pstmtSelect.setString(3, targetInput);
                try (ResultSet rsSelect = pstmtSelect.executeQuery()) {
                    if (rsSelect.next()) {
                        String empId = rsSelect.getString("id");
                        String empName = rsSelect.getString("name");
                        String shift = rsSelect.getString("shift");

                        String sqlInsert = "INSERT INTO attendance_logs (employee_id, employee_name, shift) VALUES (?, ?, ?)";
                        try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
                            pstmtInsert.setString(1, empId);
                            pstmtInsert.setString(2, empName);
                            pstmtInsert.setString(3, shift);
                            pstmtInsert.executeUpdate();
                        }
                        return true;
                    }
                }
            }

            if ("ADMIN".equalsIgnoreCase(targetInput) || "admin".equalsIgnoreCase(targetInput)) {
                String sqlAdmin = "INSERT IGNORE INTO employees (id, name, phone, cccd, role, shift, notes) VALUES ('ADMIN', ?, '---', '---', 'Chủ Garage', 'Toàn thời gian', 'Quản lý')";
                try (PreparedStatement pAdmin = conn.prepareStatement(sqlAdmin)) {
                    pAdmin.setString(1, operatorFullName != null ? operatorFullName : "Chủ Garage");
                    pAdmin.executeUpdate();
                }

                String sqlInsert = "INSERT INTO attendance_logs (employee_id, employee_name, shift) VALUES ('ADMIN', ?, 'Toàn thời gian')";
                try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
                    pstmtInsert.setString(1, operatorFullName != null ? operatorFullName : "Chủ Garage");
                    pstmtInsert.executeUpdate();
                }
                return true;
            }
        }
        return false;
    }

    public void deleteAttendanceLog(int logId) throws SQLException {
        String sql = "DELETE FROM attendance_logs WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, logId);
            pstmt.executeUpdate();
        }
    }

    public boolean resetPassword(String targetInput) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql1 = "UPDATE users SET password = '123456' WHERE username = ? OR full_name = ?";
            try (PreparedStatement pstmt1 = conn.prepareStatement(sql1)) {
                pstmt1.setString(1, targetInput);
                pstmt1.setString(2, targetInput);
                if (pstmt1.executeUpdate() > 0)
                    return true;
            }

            String findEmpSql = "SELECT name FROM employees WHERE id = ?";
            try (PreparedStatement pEmp = conn.prepareStatement(findEmpSql)) {
                pEmp.setString(1, targetInput);
                ResultSet rs = pEmp.executeQuery();
                if (rs.next()) {
                    String empName = rs.getString("name");
                    String sql2 = "UPDATE users SET password = '123456' WHERE full_name = ?";
                    try (PreparedStatement pstmt2 = conn.prepareStatement(sql2)) {
                        pstmt2.setString(1, empName);
                        return pstmt2.executeUpdate() > 0;
                    }
                }
            }
        }
        return false;
    }

    public List<Object[]> findAttendanceLogs() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT * FROM attendance_logs ORDER BY check_in_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[] {
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

    public List<Object[]> findSelfAttendanceLogs(String empId, String username) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT * FROM attendance_logs WHERE employee_id = ? OR employee_id = ? ORDER BY check_in_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, empId != null ? empId : "");
            pstmt.setString(2, username != null ? username : "");
            ResultSet rs = pstmt.executeQuery();
            int stt = 1;
            while (rs.next()) {
                list.add(new Object[] {
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

    public List<Object[]> findGroupKtvByReceptionist(String receptionistId, String fallbackShift) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT * FROM employees WHERE managed_by = ? AND role LIKE '%Kỹ Thuật%' ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, receptionistId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    int stt = 1;
                    while (rs.next()) {
                        String ktvId = rs.getString("id");
                        String ktvName = rs.getString("name");
                        String lastCheckin = getLastCheckinTime(conn, ktvId, ktvName);
                        list.add(new Object[] { stt++, ktvId, ktvName, rs.getString("role"), rs.getString("shift"), lastCheckin });
                    }
                }
            }

            if (list.isEmpty() && fallbackShift != null && !fallbackShift.isEmpty()) {
                rebalanceKtvsForShift(conn, fallbackShift);
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, receptionistId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            String ktvId = rs.getString("id");
                            String ktvName = rs.getString("name");
                            String lastCheckin = getLastCheckinTime(conn, ktvId, ktvName);
                            list.add(new Object[] { stt++, ktvId, ktvName, rs.getString("role"), rs.getString("shift"), lastCheckin });
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Object[]> findKtvAttendanceHistoryByReceptionist(String receptionistId, String fallbackShift) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT a.id, a.employee_id, a.employee_name, a.check_in_time, a.shift " +
                "FROM attendance_logs a " +
                "JOIN employees e ON a.employee_id = e.id " +
                "WHERE e.managed_by = ? AND e.role LIKE '%Kỹ Thuật%' " +
                "ORDER BY a.check_in_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receptionistId);
            try (ResultSet rs = pstmt.executeQuery()) {
                int stt = 1;
                while (rs.next()) {
                    list.add(new Object[] {
                            stt++,
                            rs.getString("employee_id"),
                            rs.getString("employee_name"),
                            rs.getString("check_in_time"),
                            rs.getString("shift")
                    });
                }
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
                list.add(new Object[] { stt++, ktvId, ktvName, rs.getString("role"), rs.getString("shift"),
                        lastCheckin });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Object[]> findAllEmployeesForCheckin() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT * FROM employees ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            int stt = 1;
            while (rs.next()) {
                String empId = rs.getString("id");
                String empName = rs.getString("name");
                String lastCheckin = getLastCheckinTime(conn, empId, empName);
                list.add(new Object[] { stt++, empId, empName, rs.getString("role"), rs.getString("shift"),
                        lastCheckin });
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
                "JOIN employees e ON a.employee_id = e.id " +
                "WHERE e.shift = ? AND e.role LIKE '%Kỹ Thuật%' " +
                "ORDER BY a.check_in_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shift);
            ResultSet rs = pstmt.executeQuery();
            int stt = 1;
            while (rs.next()) {
                list.add(new Object[] {
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
        String empId = getEmployeeIdByUsernameOrName(username, fullName);
        String sql = "SELECT shift FROM employees WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, empId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return rs.getString("shift");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Ca 1 (06:00 - 14:00)";
    }

    private String getLastCheckinTime(Connection conn, String ktvId, String ktvName) {
        String sql = "SELECT check_in_time FROM attendance_logs WHERE employee_id = ? ORDER BY check_in_time DESC LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ktvId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return rs.getString("check_in_time");
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