package com.garage.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&allowMultiQueries=true";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/garage_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    
    private static String USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
    private static String PASS = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "123456";
    private static final String[] COMMON_PASSWORDS = {"123456", "", "root", "admin", "password"};

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        // Thử mật khẩu cấu hình hiện tại trước
        try {
            return DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            // Nếu thất bại, thử các mật khẩu phổ biến khác
            for (String p : COMMON_PASSWORDS) {
                try {
                    Connection c = DriverManager.getConnection(DB_URL, USER, p);
                    PASS = p; // Lưu mật khẩu đúng để dùng cho các lần sau
                    return c;
                } catch (SQLException ignored) {}
            }
            throw e;
        }
    }

    public static void initDatabase() {
        System.out.println("Đang kiểm tra và khởi tạo Cơ sở dữ liệu từ file schema.sql...");
        
        Connection conn = null;
        try {
            try {
                conn = DriverManager.getConnection(SERVER_URL, USER, PASS);
            } catch (SQLException e) {
                for (String p : COMMON_PASSWORDS) {
                    try {
                        conn = DriverManager.getConnection(SERVER_URL, USER, p);
                        PASS = p;
                        break;
                    } catch (SQLException ignored) {}
                }
                if (conn == null) throw e;
            }

            try (Statement stmt = conn.createStatement()) {
                String sqlScript = readSqlFile("schema.sql");
                if (sqlScript != null && !sqlScript.trim().isEmpty()) {
                    stmt.execute(sqlScript);
                    System.out.println("Tự động tạo Database & Tables từ file script .sql thành công!");
                } else {
                    System.err.println("File schema.sql rỗng hoặc không tìm thấy!");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tự động chạy script khởi tạo Database: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static String readSqlFile(String filePath) {
        StringBuilder builder = new StringBuilder();
        
        try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream(filePath);
             BufferedReader reader = (is != null) 
                     ? new BufferedReader(new InputStreamReader(is))
                     : new BufferedReader(new FileReader(filePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("--") && !line.trim().startsWith("//")) {
                    builder.append(line).append("\n");
                }
            }
            return builder.toString();

        } catch (Exception e) {
            System.err.println("Không đọc được file: " + filePath);
            e.printStackTrace();
            return null;
        }
    }
}