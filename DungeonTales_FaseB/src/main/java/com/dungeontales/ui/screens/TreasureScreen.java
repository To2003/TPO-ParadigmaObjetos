package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class TreasureScreen extends JPanel {
    public interface Listener { void onContinue(); }

    private enum ChestState { CLOSED, OPEN }

    private ChestState chestState = ChestState.CLOSED;
    private final ChestPanel chestPanel;
    private final JLabel rewardLabel;
    private final JButton continueBtn;
    private final JLabel hintLabel;

    public TreasureScreen(GameState state, Listener listener) {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        // ── Encabezado ───────────────────────────────────────────────────────
        JLabel title = new JLabel("SALA DEL TESORO", SwingConstants.CENTER);
        title.setFont(Theme.titleFont(30f));
        title.setForeground(Theme.GOLD_COLOR);
        title.setBorder(BorderFactory.createEmptyBorder(32, 0, 8, 0));
        add(title, BorderLayout.NORTH);

        // ── Centro: cofre + recompensa ────────────────────────────────────────
        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(Theme.BG_DARK);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        hintLabel = new JLabel("Haz click en el cofre para abrirlo", SwingConstants.CENTER);
        hintLabel.setFont(Theme.bodyFont(13f));
        hintLabel.setForeground(Theme.TEXT_MUTED);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        chestPanel = new ChestPanel();
        chestPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        chestPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        chestPanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (chestState == ChestState.CLOSED) openChest(state);
            }
        });

        rewardLabel = new JLabel("", SwingConstants.CENTER);
        rewardLabel.setFont(Theme.labelFont(17f));
        rewardLabel.setForeground(Theme.GOLD_COLOR);
        rewardLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rewardLabel.setVisible(false);

        centerPanel.add(Box.createVerticalStrut(16));
        centerPanel.add(hintLabel);
        centerPanel.add(Box.createVerticalStrut(24));
        centerPanel.add(chestPanel);
        centerPanel.add(Box.createVerticalStrut(28));
        centerPanel.add(rewardLabel);

        add(centerPanel, BorderLayout.CENTER);

        // ── Botón inferior ───────────────────────────────────────────────────
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(Theme.BG_DARK);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 32, 0));

        continueBtn = new JButton("Seguir Adelante");
        Theme.styleMenuButton(continueBtn);
        continueBtn.setEnabled(false);
        continueBtn.addActionListener(e -> listener.onContinue());
        bottomPanel.add(continueBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void openChest(GameState state) {
        int roll = (int)(Math.random() * 4);
        String reward = switch (roll) {
            case 0 -> { state.addGold(30);
                yield "¡Encontraste 30 monedas de oro!"; }
            case 1 -> { state.getAliveParty().forEach(c -> c.healHp(25));
                yield "Una magia ancestral cura 25 HP a toda la party!"; }
            case 2 -> { state.getAliveParty().forEach(c -> c.gainExp(50));
                yield "Tablillas de sabiduría otorgan +50 EXP a todos!"; }
            default -> { state.addGold(50);
                yield "¡Un cofre lleno de monedas! +50 GP"; }
        };

        chestState = ChestState.OPEN;
        hintLabel.setVisible(false);
        chestPanel.repaint();
        rewardLabel.setText(reward);
        rewardLabel.setVisible(true);
        continueBtn.setEnabled(true);
    }

    // ── Panel del cofre (pintado con Java2D) ──────────────────────────────────
    private class ChestPanel extends JPanel {
        private static final int W = 220;
        private static final int H = 190;

        ChestPanel() {
            setPreferredSize(new Dimension(W, H));
            setMaximumSize(new Dimension(W, H));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            if (chestState == ChestState.CLOSED) drawClosed(g2);
            else drawOpen(g2);

            g2.dispose();
        }

        private void drawClosed(Graphics2D g2) {
            int bx = 20, by = 95, bw = 180, bh = 75; // cuerpo
            int lx = 20, ly = 58, lw = 180, lh = 45; // tapa

            // Sombra
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRoundRect(bx + 6, by + 6, bw, bh, 10, 10);

            // Cuerpo del cofre
            g2.setPaint(new GradientPaint(bx, by, new Color(0x5E, 0x32, 0x12),
                bx, by + bh, new Color(0x3A, 0x1E, 0x08)));
            g2.fillRoundRect(bx, by, bw, bh, 10, 10);
            g2.setColor(new Color(0x90, 0x65, 0x1A));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx, by, bw, bh, 10, 10);

            // Tapa cerrada
            g2.setPaint(new GradientPaint(lx, ly, new Color(0x78, 0x45, 0x18),
                lx, ly + lh, new Color(0x5E, 0x32, 0x12)));
            g2.fillRoundRect(lx, ly, lw, lh, 10, 10);
            g2.setColor(new Color(0x90, 0x65, 0x1A));
            g2.drawRoundRect(lx, ly, lw, lh, 10, 10);

            // Banda metálica horizontal central
            g2.setPaint(new GradientPaint(bx, 93, new Color(0xB0, 0x80, 0x20),
                bx, 103, new Color(0x80, 0x58, 0x10)));
            g2.fillRect(bx, 93, bw, 10);
            g2.setColor(new Color(0xA0, 0x70, 0x18));
            g2.drawRect(bx, 93, bw, 10);

            // Bandas verticales en el cuerpo
            for (int vx : new int[]{bx + 45, bx + bw - 45}) {
                g2.setPaint(new GradientPaint(vx, by, new Color(0xA0, 0x70, 0x18),
                    vx + 6, by, new Color(0x70, 0x48, 0x10)));
                g2.fillRect(vx, by, 6, bh);
                g2.setColor(new Color(0x90, 0x65, 0x1A));
                g2.drawRect(vx, by, 6, bh);
            }

            // Remaches en las esquinas
            g2.setColor(new Color(0xC0, 0x90, 0x28));
            int[] rx = {bx + 8, bx + bw - 12, bx + 8, bx + bw - 12};
            int[] ry = {by + 8, by + 8, by + bh - 12, by + bh - 12};
            for (int i = 0; i < 4; i++) {
                g2.fillOval(rx[i], ry[i], 8, 8);
                g2.setColor(new Color(0x80, 0x58, 0x10));
                g2.drawOval(rx[i], ry[i], 8, 8);
                g2.setColor(new Color(0xC0, 0x90, 0x28));
            }

            // Cerradura
            int lockX = 94, lockY = 104;
            g2.setColor(new Color(0xD8, 0xAA, 0x22));
            g2.fillOval(lockX, lockY, 32, 26);
            g2.setColor(new Color(0xFF, 0xD8, 0x50));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(lockX, lockY, 32, 26);
            // Ojo de la cerradura
            g2.setColor(new Color(0x30, 0x18, 0x05));
            g2.fillOval(lockX + 10, lockY + 7, 12, 10);
            g2.fillRect(lockX + 14, lockY + 15, 4, 6);

            // Runas en la tapa (puntos brillantes)
            g2.setColor(new Color(0xFF, 0xE0, 0x80, 160));
            int[] runX = {40, 100, 160, 70, 150};
            int[] runY = {68, 63, 68, 80, 80};
            for (int i = 0; i < 5; i++) {
                g2.fillOval(runX[i] - 3, runY[i] - 3, 6, 6);
            }
        }

        private void drawOpen(Graphics2D g2) {
            int bx = 20, by = 95, bw = 180, bh = 75;

            // Resplandor dorado de fondo
            RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(110, 90), 110f,
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                    new Color(0xFF, 0xD0, 0x50, 70),
                    new Color(0xFF, 0xA0, 0x20, 30),
                    new Color(0, 0, 0, 0)
                }
            );
            g2.setPaint(glow);
            g2.fillRect(0, 0, W, H);

            // Sombra del cuerpo
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRoundRect(bx + 6, by + 6, bw, bh, 10, 10);

            // Tapa abierta hacia atrás (perspectiva)
            int[] lidX = {bx, bx + bw, bx + bw - 10, bx + 10};
            int[] lidY = {by, by, by - 52, by - 52};
            g2.setPaint(new GradientPaint(bx, by - 52, new Color(0x78, 0x45, 0x18),
                bx, by, new Color(0x5E, 0x32, 0x12)));
            g2.fillPolygon(lidX, lidY, 4);
            g2.setColor(new Color(0xA0, 0x70, 0x20));
            g2.setStroke(new BasicStroke(2f));
            g2.drawPolygon(lidX, lidY, 4);

            // Interior de la tapa (parte visible interior)
            int[] innerLidX = {bx + 4, bx + bw - 4, bx + bw - 13, bx + 13};
            int[] innerLidY = {by - 2, by - 2, by - 48, by - 48};
            g2.setColor(new Color(0x40, 0x22, 0x08));
            g2.fillPolygon(innerLidX, innerLidY, 4);

            // Cuerpo del cofre
            g2.setPaint(new GradientPaint(bx, by, new Color(0x5E, 0x32, 0x12),
                bx, by + bh, new Color(0x3A, 0x1E, 0x08)));
            g2.fillRoundRect(bx, by, bw, bh, 10, 10);
            g2.setColor(new Color(0x90, 0x65, 0x1A));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx, by, bw, bh, 10, 10);

            // Interior brillante del cofre
            g2.setPaint(new GradientPaint(bx + 8, by + 2, new Color(0xD0, 0x90, 0x10),
                bx + 8, by + 38, new Color(0x80, 0x50, 0x08)));
            g2.fillRoundRect(bx + 8, by + 2, bw - 16, 38, 6, 6);

            // Monedas dentro del cofre
            Color coinTop   = new Color(0xFF, 0xCC, 0x22);
            Color coinSide  = new Color(0xC0, 0x88, 0x10);
            Color coinEdge  = new Color(0xA0, 0x70, 0x08);
            for (int i = 0; i < 9; i++) {
                int cx = bx + 14 + i * 17;
                int cy = by + 14;
                g2.setColor(coinSide);
                g2.fillOval(cx, cy + 3, 14, 8);
                g2.setColor(coinTop);
                g2.fillOval(cx, cy, 14, 10);
                g2.setColor(coinEdge);
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(cx, cy, 14, 10);
            }

            // Bandas metálicas en el cuerpo
            g2.setColor(new Color(0x90, 0x65, 0x1A));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(bx, by + 40, bx + bw, by + 40);
            g2.drawLine(bx, by + 55, bx + bw, by + 55);

            // Bandas verticales en el cuerpo
            for (int vx : new int[]{bx + 45, bx + bw - 45}) {
                g2.setPaint(new GradientPaint(vx, by, new Color(0xA0, 0x70, 0x18),
                    vx + 6, by, new Color(0x70, 0x48, 0x10)));
                g2.fillRect(vx, by + 38, 6, bh - 38);
                g2.setColor(new Color(0x90, 0x65, 0x1A));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(vx, by + 38, 6, bh - 38);
            }

            // Destellos
            g2.setColor(new Color(0xFF, 0xFF, 0xC0, 220));
            drawSparkle(g2, 110, 22, 12);
            drawSparkle(g2, 62,  42, 8);
            drawSparkle(g2, 160, 38, 8);
            drawSparkle(g2, 85,  55, 6);
            drawSparkle(g2, 145, 60, 6);
        }

        private void drawSparkle(Graphics2D g2, int cx, int cy, int size) {
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawLine(cx - size, cy, cx + size, cy);
            g2.drawLine(cx, cy - size, cx, cy + size);
            int d = size / 2;
            g2.drawLine(cx - d, cy - d, cx + d, cy + d);
            g2.drawLine(cx - d, cy + d, cx + d, cy - d);
        }
    }
}
