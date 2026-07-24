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
    
    private static final String USER = "root"; 
    private static final String PASS = "123456"; 

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static void initDatabase() {
        System.out.println("Đang kiểm tra và khởi tạo Cơ sở dữ liệu từ file schema.sql...");
        
        try (Connection conn = DriverManager.getConnection(SERVER_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            String sqlScript = readSqlFile("schema.sql");
            if (sqlScript != null && !sqlScript.trim().isEmpty()) {
                stmt.execute(sqlScript);
                System.out.println("Tự động tạo Database & Tables từ file script .sql thành công!");
            } else {
                System.err.println("File schema.sql rỗng hoặc không tìm thấy!");
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi tự động chạy script khởi tạo Database!");
            e.printStackTrace();
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