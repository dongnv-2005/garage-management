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
    // Kết nối tổng tới MySQL Server (dùng để khởi tạo Database)
    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&allowMultiQueries=true";
    
    // Kết nối tới CSDL garage_db chính thức
    private static final String DB_URL = "jdbc:mysql://localhost:3306/garage_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    
    private static final String USER = "root"; 
    private static final String PASS = "123456"; 

    // Hàm lấy kết nối CSDL garage_db cho các Repository
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    // Hàm tự động thực thi file schema.sql để khởi tạo CSDL
    public static void initDatabase() {
        System.out.println("Đang kiểm tra và khởi tạo Cơ sở dữ liệu từ file schema.sql...");
        
        // Kết nối vào MySQL Server (cho phép chạy nhiều lệnh bằng allowMultiQueries=true)
        try (Connection conn = DriverManager.getConnection(SERVER_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            // Đọc file schema.sql
            String sqlScript = readSqlFile("schema.sql");
            if (sqlScript != null && !sqlScript.trim().isEmpty()) {
                // Thực thi toàn bộ script SQL
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

    // Đọc file .sql thành Chuỗi String
    private static String readSqlFile(String filePath) {
        StringBuilder builder = new StringBuilder();
        
        // Ưu tiên đọc file từ resources/classpath, nếu không thấy thì đọc từ file ngoài
        try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream(filePath);
             BufferedReader reader = (is != null) 
                     ? new BufferedReader(new InputStreamReader(is))
                     : new BufferedReader(new FileReader(filePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Bỏ qua các dòng comment trong SQL
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