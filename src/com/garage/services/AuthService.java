package com.garage.services;

import com.garage.config.DatabaseConfig;
import com.garage.enums.Role;
import com.garage.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {
    private static User currentUser;

    public boolean login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                currentUser = new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name"),
                        Role.valueOf(rs.getString("role"))
                );
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static User getCurrentUser() { return currentUser; }
    public static void logout() { currentUser = null; }
}