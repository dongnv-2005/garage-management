package com.garage.gui;

import com.garage.config.DatabaseConfig;
import com.garage.services.AuthService;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class ChangePasswordDialog extends JDialog {
    private final JPasswordField txtOldPassword;
    private final JPasswordField txtNewPassword;
    private final JPasswordField txtConfirmPassword;

    public ChangePasswordDialog(Frame parent) {
        super(parent, "ĐỔI MẬT KHẨU TÀI KHOẢN", true);
        setSize(400, 260);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        txtOldPassword = new JPasswordField();
        txtNewPassword = new JPasswordField();
        txtConfirmPassword = new JPasswordField();

        JButton btnSave = new JButton("Lưu Mật Khẩu Mới");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnSave.setFocusPainted(false);
        btnSave.setOpaque(true);
        btnSave.setBorderPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));

        panel.add(createInputRow("Mật khẩu cũ: ", txtOldPassword));
        panel.add(createInputRow("Mật khẩu mới: ", txtNewPassword));
        panel.add(createInputRow("Xác nhận MK: ", txtConfirmPassword));
        panel.add(btnSave);

        btnSave.addActionListener(e -> handleChangePassword());

        add(panel);
    }

    private JPanel createInputRow(String labelText, JComponent component) {
        JPanel p = new JPanel(new BorderLayout(8, 5));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setPreferredSize(new Dimension(100, 25));
        p.add(lbl, BorderLayout.WEST);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    private void handleChangePassword() {
        String oldPass = new String(txtOldPassword.getPassword()).trim();
        String newPass = new String(txtNewPassword.getPassword()).trim();
        String confirmPass = new String(txtConfirmPassword.getPassword()).trim();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không trùng khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String currentUsername = AuthService.getCurrentUser().getUsername();

        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. Kiểm tra mật khẩu cũ
            PreparedStatement checkStmt = conn.prepareStatement("SELECT password FROM users WHERE username = ?");
            checkStmt.setString(1, currentUsername);
            var rs = checkStmt.executeQuery();

            if (rs.next() && rs.getString("password").equals(oldPass)) {
                // 2. Cập nhật mật khẩu mới
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET password = ? WHERE username = ?");
                updateStmt.setString(1, newPass);
                updateStmt.setString(2, currentUsername);
                updateStmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Mật khẩu cũ không chính xác!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}