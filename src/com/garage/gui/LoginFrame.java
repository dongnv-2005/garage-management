package com.garage.gui;

import com.garage.services.AuthService;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField txtUsername;
    private final JPasswordField txtPassword;
    private final AuthService authService;

    public LoginFrame() {
        this.authService = new AuthService();

        setTitle("ĐĂNG NHẬP HỆ THỐNG GARA");
        setSize(420, 270);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));

        JLabel lblTitle = new JLabel("ĐĂNG NHẬP HỆ THỐNG", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitle.setForeground(new Color(44, 62, 80));

        txtUsername = new JTextField();
        txtUsername.setFont(new Font("SansSerif", Font.PLAIN, 13));

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JButton btnLogin = new JButton("ĐĂNG NHẬP");
        btnLogin.setBackground(new Color(52, 152, 219));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.setFocusPainted(false);
        btnLogin.setOpaque(true);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel p1 = new JPanel(new BorderLayout(8, 5));
        JLabel lblUser = new JLabel("Tài khoản: ");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 12));
        p1.add(lblUser, BorderLayout.WEST);
        p1.add(txtUsername, BorderLayout.CENTER);

        JPanel p2 = new JPanel(new BorderLayout(8, 5));
        JLabel lblPass = new JLabel("Mật khẩu:  ");
        lblPass.setFont(new Font("SansSerif", Font.BOLD, 12));
        p2.add(lblPass, BorderLayout.WEST);
        p2.add(txtPassword, BorderLayout.CENTER);

        panel.add(lblTitle);
        panel.add(p1);
        panel.add(p2);
        panel.add(btnLogin);

        btnLogin.addActionListener(e -> handleLogin());
        txtPassword.addActionListener(e -> handleLogin());

        add(panel);
    }

    private void handleLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ tài khoản và mật khẩu!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (authService.login(user, pass)) {
            JOptionPane.showMessageDialog(this, "Đăng nhập thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            this.dispose();

            SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
        } else {
            JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!", "Lỗi Đăng Nhập", JOptionPane.ERROR_MESSAGE);
        }
    }
}