package com.garage.gui;

import com.garage.models.Invoice;
import com.garage.models.Vehicle;
import com.garage.services.BillingManager;
import com.garage.services.VehicleManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceChartReportPanel extends BaseReportPanel {

    static class ServiceStat {
        int count = 0;
        double revenue = 0;
    }

    private final ChartCanvas chartCanvas;

    public ServiceChartReportPanel(BillingManager billingManager, VehicleManager vehicleManager) {
        super(billingManager, vehicleManager);

        chartCanvas = new ChartCanvas();
        contentContainer.add(chartCanvas, BorderLayout.CENTER);
    }

    @Override
    protected void renderReportContent(List<Invoice> invoices, List<Vehicle> vehicles, int selectedMonth, int selectedYear) {
        Map<String, ServiceStat> serviceStats = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Invoice inv : invoices) {
            boolean isMatch = false;
            if (inv.getCreatedAt() != null) {
                try {
                    LocalDateTime dt = LocalDateTime.parse(inv.getCreatedAt(), formatter);
                    if (dt.getMonthValue() == selectedMonth && dt.getYear() == selectedYear) isMatch = true;
                } catch (Exception e) {
                    try {
                        String dateOnly = inv.getCreatedAt().split(" ")[0];
                        String[] parts = dateOnly.split("-");
                        int y = Integer.parseInt(parts[0]);
                        int m = Integer.parseInt(parts[1]);
                        if (m == selectedMonth && y == selectedYear) isMatch = true;
                    } catch (Exception ignored) {}
                }
            }

            if (isMatch) {
                String sName = inv.getServiceName();
                ServiceStat stat = serviceStats.getOrDefault(sName, new ServiceStat());
                stat.count++;
                stat.revenue += inv.getTotalAmount();
                serviceStats.put(sName, stat);
            }
        }

        chartCanvas.updateData(serviceStats, "Tháng " + selectedMonth + "/" + selectedYear);
    }

    private static class ChartCanvas extends JPanel {
        private Map<String, ServiceStat> data = new HashMap<>();
        private String timeTitle = "";

        private Point mousePos = null;
        private String hoveredText = null;

        public ChartCanvas() {
            java.awt.event.MouseAdapter mouseHandler = new java.awt.event.MouseAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    mousePos = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    mousePos = null;
                    hoveredText = null;
                    repaint();
                }
            };

            addMouseMotionListener(mouseHandler);
            addMouseListener(mouseHandler);
        }

        public void updateData(Map<String, ServiceStat> data, String timeTitle) {
            this.data = data;
            this.timeTitle = timeTitle;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            if (data == null || data.isEmpty()) {
                g2d.setColor(Color.GRAY);
                g2d.setFont(new Font("SansSerif", Font.ITALIC, 14));
                g2d.drawString("Không có dữ liệu hóa đơn cho " + timeTitle, width / 2 - 130, height / 2);
                return;
            }

            int padding = 50;
            int bottomPadding = 60;
            int topPadding = 60;

            double maxRev = data.values().stream().mapToDouble(v -> v.revenue).max().orElse(1);
            int barCount = data.size();
            int barSpace = (width - 2 * padding) / barCount;
            int barWidth = Math.min(barSpace - 25, 80);

            g2d.setColor(Color.DARK_GRAY);
            g2d.drawLine(padding, height - bottomPadding, width - padding, height - bottomPadding);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2d.setColor(new Color(41, 128, 185));
            g2d.drawString("Thống kê Doanh thu & Số lượng Hóa đơn theo Dịch vụ (" + timeTitle + ")", padding, topPadding - 25);

            int x = padding + (barSpace - barWidth) / 2;
            hoveredText = null; 

            for (Map.Entry<String, ServiceStat> entry : data.entrySet()) {
                ServiceStat stat = entry.getValue();

                int barHeight = (int) ((stat.revenue / maxRev) * (height - topPadding - bottomPadding - 40));
                if (barHeight < 40) barHeight = 40;

                int y = height - bottomPadding - barHeight;

                Rectangle barBounds = new Rectangle(x, y, barWidth, barHeight);
                Rectangle labelBounds = new Rectangle(x - 10, height - bottomPadding, barWidth + 20, bottomPadding);

                boolean isHovered = mousePos != null && (barBounds.contains(mousePos) || labelBounds.contains(mousePos));

                if (isHovered) {
                    hoveredText = entry.getKey() + " | " + stat.count + " HĐ | Doanh thu: " + String.format("%,.0f VNĐ", stat.revenue);
                }

                g2d.setColor(isHovered ? new Color(41, 128, 185) : new Color(52, 152, 219));
                g2d.fillRect(x, y, barWidth, barHeight);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, barWidth, barHeight);

                g2d.setColor(new Color(192, 57, 43));
                g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
                String countTxt = stat.count + " HĐ";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(countTxt, x + (barWidth - fm.stringWidth(countTxt)) / 2, y - 8);

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
                String revTxt = String.format("%,.0f VNĐ", stat.revenue);
                fm = g2d.getFontMetrics();

                java.awt.geom.AffineTransform orig = g2d.getTransform();
                g2d.translate(x + barWidth / 2 + fm.getAscent() / 3, y + barHeight - 12);
                g2d.rotate(-Math.PI / 2);
                g2d.drawString(revTxt, 0, 0);
                g2d.setTransform(orig);

                g2d.setColor(isHovered ? new Color(142, 68, 173) : Color.BLACK);
                g2d.setFont(new Font("SansSerif", isHovered ? Font.BOLD : Font.PLAIN, 11));
                String sName = entry.getKey();
                if (sName.length() > 16) sName = sName.substring(0, 14) + "..";
                fm = g2d.getFontMetrics();
                g2d.drawString(sName, x + (barWidth - fm.stringWidth(sName)) / 2, height - bottomPadding + 20);

                x += barSpace;
            }

            if (mousePos != null && hoveredText != null) {
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
                FontMetrics fm = g2d.getFontMetrics();

                int tooltipWidth = fm.stringWidth(hoveredText) + 24;
                int tooltipHeight = 32;


                int tx = mousePos.x + 15;
                int ty = mousePos.y + 15;

                if (tx + tooltipWidth > width - 10) tx = mousePos.x - tooltipWidth - 10;
                if (ty + tooltipHeight > height - 10) ty = mousePos.y - tooltipHeight - 10;

                g2d.setColor(new Color(0, 0, 0, 40));
                g2d.fillRoundRect(tx + 2, ty + 2, tooltipWidth, tooltipHeight, 16, 16);

                g2d.setColor(new Color(33, 33, 33, 230));
                g2d.fillRoundRect(tx, ty, tooltipWidth, tooltipHeight, 16, 16);

                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.drawRoundRect(tx, ty, tooltipWidth, tooltipHeight, 16, 16);

                g2d.setColor(Color.WHITE);
                g2d.drawString(hoveredText, tx + 12, ty + (tooltipHeight + fm.getAscent()) / 2 - 2);
            }
        }
    }
}