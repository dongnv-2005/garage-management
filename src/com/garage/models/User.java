package com.garage.models;

import com.garage.enums.Role;

public class User {
    private String username;
    private String password;
    private String fullName;
    private Role role; // ADMIN hoặc RECEPTIONIST

    // Constructor không tham số (dùng khi cần)
    public User() {
    }

    // Constructor đầy đủ tham số
    public User(String username, String password, String fullName, Role role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    // Getters và Setters (Đảm bảo tính Đóng gói - Encapsulation)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - Vai trò: %s", fullName, username, role.getDescription());
    }
}