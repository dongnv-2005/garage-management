package com.garage.gui;

import com.garage.config.DatabaseConfig;
import com.garage.models.User;
import com.garage.services.AuthService;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AccountManagementDialog extends JDialog {
    private JTextField txtUsername, txtFullName, txtPhone;

    public AccountManagementDialog(JFrame parent) {
        super(parent, "QUẢN LÝ TÀI KHOẢN CÁ NHÂN", true);
        setSize(430, 260);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        User user = AuthService.getCurrentUser();

        JPanel mainPanel = new JPanel(new GridLayout(3, 2, 10, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        mainPanel.add(new JLabel("Tên đăng nhập:"));
        txtUsername = new JTextField(user.getUsername());
        txtUsername.setEditable(false);
        txtUsername.setBackground(new Color(238, 238, 238));
        mainPanel.add(txtUsername);

        mainPanel.add(new JLabel("Họ & Tên:"));
        txtFullName = new JTextField(user.getFullName());
        mainPanel.add(txtFullName);

        mainPanel.add(new JLabel("Số điện thoại:"));
        txtPhone = new JTextField();
        mainPanel.add(txtPhone);

        loadEmployeePhone(user.getUsername(), user.getFullName());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton btnSave = new JButton("Lưu Thay Đổi");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnSave.setFocusPainted(false);
        btnSave.setOpaque(true);
        btnSave.setContentAreaFilled(true);
        btnSave.setBorderPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setPreferredSize(new Dimension(130, 35));

        JButton btnOpenChangePass = new JButton("Đổi Mật Khẩu");
        btnOpenChangePass.setBackground(new Color(52, 152, 219));
        btnOpenChangePass.setForeground(Color.WHITE);
        btnOpenChangePass.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnOpenChangePass.setFocusPainted(false);
        btnOpenChangePass.setOpaque(true);
        btnOpenChangePass.setContentAreaFilled(true);
        btnOpenChangePass.setBorderPainted(false);
        btnOpenChangePass.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOpenChangePass.setPreferredSize(new Dimension(130, 35));

        btnPanel.add(btnSave);
        btnPanel.add(btnOpenChangePass);

        add(mainPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> {
            String newFullName = txtFullName.getText().trim();
            String newPhone = txtPhone.getText().trim();

            if (newFullName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Họ tên không được để trống!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = DatabaseConfig.getConnection()) {
                PreparedStatement p1 = conn.prepareStatement("UPDATE users SET full_name = ? WHERE username = ?");
                p1.setString(1, newFullName);
                p1.setString(2, user.getUsername());
                p1.executeUpdate();

                // Cập nhật chính xác nhân viên tương ứng
                PreparedStatement p2 = conn.prepareStatement("UPDATE employees SET name = ?, phone = ? WHERE id = ? OR name = ?");
                p2.setString(1, newFullName);
                p2.setString(2, newPhone.isEmpty() ? "---" : newPhone);
                p2.setString(3, user.getUsername());
                p2.setString(4, user.getFullName());
                p2.executeUpdate();

                // Cập nhật lại session người dùng hiện tại
                user.setFullName(newFullName);

                JOptionPane.showMessageDialog(this, "Cập nhật thông tin tài khoản thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi cập nhật: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnOpenChangePass.addActionListener(e -> {
            new ChangePasswordDialog(parent).setVisible(true);
        });
    }

    private void loadEmployeePhone(String username, String fullName) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT phone FROM employees WHERE id = ? OR name = ?")) {
            pstmt.setString(1, username);
            pstmt.setString(2, fullName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String phone = rs.getString("phone");
                txtPhone.setText((phone == null || phone.equals("---")) ? "" : phone);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}