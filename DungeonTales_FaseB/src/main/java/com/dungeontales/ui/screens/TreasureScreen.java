package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.util.SpriteLoader;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class TreasureScreen extends JPanel {
    public interface Listener { void onContinue(); }

    private enum ChestState { CLOSED, OPEN }

    private static final int CHEST_SIZE = 300;

    private ChestState chestState = ChestState.CLOSED;
    private final ChestPanel chestPanel;
    private final JLabel rewardLabel;
    private final JPanel rewardPane;
    private final JButton continueBtn;
    private final JLabel hintLabel;
    private final BufferedImage background;

    public TreasureScreen(GameState state, Listener listener) {
        setLayout(new GridBagLayout());
        setBackground(new Color(0x08, 0x05, 0x02));

        background = SpriteLoader.getBackground("treasure");

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Title
        content.add(Box.createVerticalStrut(28));
        content.add(buildTitle());

        // Hint
        hintLabel = new JLabel("Haz click en el cofre para abrirlo", SwingConstants.CENTER);
        hintLabel.setFont(Theme.labelFont(15f));
        hintLabel.setForeground(new Color(0xC8, 0xAA, 0x70, 200));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(16));
        content.add(hintLabel);

        // Chest
        chestPanel = new ChestPanel();
        chestPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        chestPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        chestPanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (chestState == ChestState.CLOSED) openChest(state);
            }
        });
        content.add(Box.createVerticalStrut(10));
        content.add(chestPanel);

        // Reward parchment
        rewardLabel = new JLabel("", SwingConstants.CENTER);
        rewardLabel.setFont(Theme.labelFont(16f));
        rewardLabel.setForeground(new Color(0xFF, 0xE0, 0x80));

        rewardPane = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(0x10, 0x08, 0x02, 190));
                g2.fillRoundRect(0, 0, w, h, 16, 16);
                g2.setColor(new Color(0xC0, 0x90, 0x28, 210));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 16, 16);
                g2.setColor(new Color(0xA0, 0x70, 0x18, 120));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(4, 4, w - 8, h - 8, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        rewardPane.setOpaque(false);
        rewardPane.setLayout(new BoxLayout(rewardPane, BoxLayout.Y_AXIS));
        rewardPane.setBorder(BorderFactory.createEmptyBorder(14, 28, 14, 28));
        rewardPane.add(rewardLabel);
        rewardPane.setMaximumSize(new Dimension(520, 72));
        rewardPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        rewardPane.setVisible(false);

        content.add(Box.createVerticalStrut(16));
        content.add(rewardPane);

        // Continue button
        continueBtn = new JButton("Seguir Adelante");
        Theme.styleMenuButton(continueBtn);
        continueBtn.setEnabled(false);
        continueBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        continueBtn.addActionListener(e -> listener.onContinue());
        content.add(Box.createVerticalStrut(20));
        content.add(continueBtn);
        content.add(Box.createVerticalStrut(28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        add(content, gbc);
    }

    private JPanel buildTitle() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                String text = "SALA DEL TESORO";
                Font f = Theme.titleFont(34f);
                g2.setFont(f);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(text)) / 2;
                int ty = fm.getAscent() + 4;
                int[][] layers = {
                    { 5,  6,  0x10, 0x08, 0x02, 255},
                    { 3,  4,  0x20, 0x10, 0x05, 220},
                    { 2,  2,  0x3A, 0x2A, 0x10, 200},
                    { 1,  1,  0x5A, 0x40, 0x18, 220},
                    { 0,  0,  0x6E, 0x52, 0x20, 255},
                    {-1, -1,  0xD4, 0x9A, 0x28, 255},
                    {-2, -2,  0xFF, 0xE8, 0x80, 180}
                };
                for (int[] l : layers) {
                    g2.setColor(new Color(l[2], l[3], l[4], l[5]));
                    g2.drawString(text, tx + l[0], ty + l[1]);
                }
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(520, 58));
        p.setMaximumSize(new Dimension(520, 58));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        return p;
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
        rewardPane.setVisible(true);
        continueBtn.setEnabled(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        int vw = getWidth(), vh = getHeight();
        int iw = background.getWidth(), ih = background.getHeight();
        double scale = Math.max((double) vw / iw, (double) vh / ih);
        int dw = (int)(iw * scale), dh = (int)(ih * scale);
        int dx = (vw - dw) / 2, dy = (vh - dh) / 2;
        g2.drawImage(background, dx, dy, dw, dh, null);
        // Dark overlay
        g2.setColor(new Color(0, 0, 0, 85));
        g2.fillRect(0, 0, vw, vh);
        // Vignette
        g2.setPaint(new RadialGradientPaint(
            new Point2D.Float(vw / 2f, vh / 2f),
            Math.max(vw, vh) * 0.72f,
            new float[]{0.4f, 1f},
            new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 155)}
        ));
        g2.fillRect(0, 0, vw, vh);
        g2.dispose();
    }

    private class ChestPanel extends JPanel {
        private static final int PAD = 32;
        private final BufferedImage imgClosed;
        private final BufferedImage imgOpen;

        private static final int CHEST_CLOSED_SIZE = 220;

        ChestPanel() {
            setPreferredSize(new Dimension(CHEST_SIZE + PAD * 2, CHEST_SIZE + PAD * 2));
            setMaximumSize(new Dimension(CHEST_SIZE + PAD * 2, CHEST_SIZE + PAD * 2));
            setOpaque(false);
            imgClosed = SpriteLoader.getTreasureIcon("chest",      CHEST_CLOSED_SIZE);
            imgOpen   = SpriteLoader.getTreasureIcon("chest-open", CHEST_SIZE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);

            int cx = PAD, cy = PAD;
            int midX = cx + CHEST_SIZE / 2;
            BufferedImage img = chestState == ChestState.CLOSED ? imgClosed : imgOpen;

            if (chestState == ChestState.OPEN) {
                // Golden radial glow
                g2.setPaint(new RadialGradientPaint(
                    new Point2D.Float(cx + CHEST_SIZE / 2f, cy + CHEST_SIZE / 2f),
                    CHEST_SIZE * 0.78f,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{
                        new Color(0xFF, 0xD0, 0x50, 105),
                        new Color(0xFF, 0xA0, 0x20, 48),
                        new Color(0, 0, 0, 0)
                    }
                ));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Sparkles above the chest
                g2.setColor(new Color(0xFF, 0xFF, 0xC0, 225));
                g2.setStroke(new BasicStroke(1.8f));
                drawSparkle(g2, midX,       cy - 12, 15);
                drawSparkle(g2, midX - 70,  cy + 22, 10);
                drawSparkle(g2, midX + 68,  cy + 16, 10);
                drawSparkle(g2, midX - 30,  cy + 4,   7);
                drawSparkle(g2, midX + 34,  cy + 9,   7);
            } else {
                // Background blur/glow when closed so it stands out from the dark background
                g2.setPaint(new RadialGradientPaint(
                    new Point2D.Float(cx + CHEST_SIZE / 2f, cy + CHEST_SIZE / 2f),
                    CHEST_SIZE * 0.65f,
                    new float[]{0f, 0.4f, 1f},
                    new Color[]{
                        new Color(0xFF, 0xFF, 0xFF, 60),  // Bright center blur
                        new Color(0xC0, 0x88, 0x18, 30),  // Warm mid-tone
                        new Color(0, 0, 0, 0)             // Fade out
                    }
                ));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            if (img != null) {
                if (chestState == ChestState.CLOSED) {
                    int offset = (CHEST_SIZE - CHEST_CLOSED_SIZE) / 2;
                    g2.drawImage(img, cx + offset, cy + offset, CHEST_CLOSED_SIZE, CHEST_CLOSED_SIZE, null);
                } else {
                    g2.drawImage(img, cx, cy, CHEST_SIZE, CHEST_SIZE, null);
                }
            }

            g2.dispose();
        }

        private void drawSparkle(Graphics2D g2, int sx, int sy, int size) {
            g2.drawLine(sx - size, sy, sx + size, sy);
            g2.drawLine(sx, sy - size, sx, sy + size);
            int d = size / 2;
            g2.drawLine(sx - d, sy - d, sx + d, sy + d);
            g2.drawLine(sx - d, sy + d, sx + d, sy - d);
        }
    }
}
