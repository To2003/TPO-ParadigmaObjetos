package com.dungeontales.ui.components;

import com.dungeontales.core.model.enemy.Enemy;
import com.dungeontales.util.SpriteLoader;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EnemyCard extends JPanel {

    private final int cardW, cardH, spriteW, spriteH;

    private final Enemy       enemy;
    private final AnimatedBar hpBar;
    private final JLabel      hpLabel;
    private final JLabel      actionHint;
    private BufferedImage     sprite;
    private boolean           targeted = false;

    // ── Flash de impacto ─────────────────────────────────────────────────
    private Color  flashColor;
    private float  flashAlpha = 0f;
    private Timer  flashTimer;

    // ── Números flotantes ────────────────────────────────────────────────
    private static class FloatingNum {
        String text; Color color;
        float x, y, alpha, vy;
        FloatingNum(String t, Color c, float x, float y) {
            this.text = t; this.color = c;
            this.x = x; this.y = y;
            this.alpha = 1f; this.vy = -2.2f;
        }
    }
    private final List<FloatingNum> floatingNums = new ArrayList<>();
    private Timer animTimer;

    public EnemyCard(Enemy enemy) {
        this.enemy = enemy;

        String n = enemy.getName().toLowerCase();
        if (n.contains("troll") || n.contains("boss") || n.contains("sombra")
                || n.contains("rey") || n.contains("eterno") || n.contains("señor")) {
            cardW = 310; cardH = 390; spriteW = 260; spriteH = 320;
        } else if (n.contains("goblin")) {
            cardW = 200; cardH = 270; spriteW = 160; spriteH = 210;
        } else {
            cardW = 250; cardH = 325; spriteW = 210; spriteH = 265;
        }

        setPreferredSize(new Dimension(cardW, cardH));
        setOpaque(false);
        setLayout(new BorderLayout(0, 2));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        sprite = SpriteLoader.getEnemySprite(enemy.getSpriteName(), spriteW, spriteH);

        JPanel statsPanel = new JPanel();
        statsPanel.setOpaque(false);
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

        JLabel nameLabel = new JLabel(enemy.getName(), SwingConstants.CENTER);
        nameLabel.setFont(Theme.labelFont(13f));
        nameLabel.setForeground(Theme.TEXT_ENEMY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        hpBar = new AnimatedBar(AnimatedBar.BarType.HP, enemy.getHpMax(), enemy.getHp());
        hpBar.setMaximumSize(new Dimension(140, 6));
        hpBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        actionHint = new JLabel("Próximo: ?");
        actionHint.setFont(Theme.bodyFont(10f));
        actionHint.setForeground(Theme.TEXT_MUTED);
        actionHint.setAlignmentX(Component.CENTER_ALIGNMENT);

        hpLabel = new JLabel(); // Dummy para mantener compatibilidad con refresh

        statsPanel.add(Box.createVerticalStrut(4));
        statsPanel.add(nameLabel);
        statsPanel.add(Box.createVerticalStrut(2));
        statsPanel.add(hpBar);
        statsPanel.add(Box.createVerticalStrut(2));
        statsPanel.add(actionHint);

        add(statsPanel, BorderLayout.SOUTH);

        animTimer = new Timer(16, e -> tickAnimations());
    }

    // ── API pública ──────────────────────────────────────────────────────

    public void setTargeted(boolean targeted) {
        this.targeted = targeted;
        repaint();
    }

    public void refresh() {
        hpLabel.setText("HP " + enemy.getHp() + "/" + enemy.getHpMax());
        hpBar.setValue(enemy.getHp(), enemy.getHpMax());
        repaint();
    }

    public void setActionHint(String hint) {
        actionHint.setText("Próximo: " + hint);
    }

    public Enemy getEnemy() { return enemy; }

    /** Flash rojo + número de daño flotante. */
    public void flashDamage(int amount) {
        startFlash(amount > 30 ? new Color(220, 40, 30) : new Color(180, 60, 50), 18);
        spawnNumber("-" + amount, Theme.DMG_COLOR);
    }

    /** Flash verde + número de curación (regen de troll, etc.). */
    public void flashHeal(int amount) {
        startFlash(new Color(30, 160, 70), 14);
        spawnNumber("+" + amount, Theme.HEAL_COLOR);
    }

    /** Texto de esquive. */
    public void flashDodge() {
        spawnNumber("ESQUIVA", Theme.EVASION_COLOR);
    }

    // ── Animación interna ────────────────────────────────────────────────

    private void startFlash(Color color, int frames) {
        if (flashTimer != null) flashTimer.stop();
        flashColor = color;
        flashAlpha = 0.5f;
        float step = flashAlpha / frames;
        flashTimer = new Timer(16, null);
        flashTimer.addActionListener(e -> {
            flashAlpha = Math.max(0f, flashAlpha - step);
            if (flashAlpha == 0f) { flashColor = null; flashTimer.stop(); }
            repaint();
        });
        flashTimer.start();
    }

    private void spawnNumber(String text, Color color) {
        floatingNums.add(new FloatingNum(text, color, cardW / 2f, cardH / 2f - 20));
        if (!animTimer.isRunning()) animTimer.start();
    }

    private void tickAnimations() {
        Iterator<FloatingNum> it = floatingNums.iterator();
        while (it.hasNext()) {
            FloatingNum fn = it.next();
            fn.y    += fn.vy;
            fn.vy   *= 0.96f;
            fn.alpha -= 0.025f;
            if (fn.alpha <= 0) it.remove();
        }
        if (floatingNums.isEmpty()) animTimer.stop();
        repaint();
    }

    // ── Pintura ──────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Halo rojo/dorado debajo cuando es objetivo seleccionado
        if (targeted) {
            g2.setPaint(new RadialGradientPaint(
                new java.awt.geom.Point2D.Float(w / 2f, h - 60),
                70f,
                new float[]{0f, 1f},
                new Color[]{new Color(200, 40, 40, 100), new Color(0,0,0,0)}
            ));
            g2.fillOval(w/2 - 70, h - 80, 140, 40);
        }

        // Sprite
        int sx = (w - spriteW) / 2;
        int sy = 8;
        if (sprite != null) g2.drawImage(sprite, sx, sy, this);

        // Overlay de muerto
        if (!enemy.isAlive()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            g2.setColor(Color.BLACK);
            g2.fillRect(sx, sy, spriteW, spriteH);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            g2.setColor(Theme.DMG_COLOR);
            g2.setFont(Theme.labelFont(18f));
            FontMetrics fm = g2.getFontMetrics();
            String txt = "☠";
            g2.drawString(txt, (w - fm.stringWidth(txt)) / 2, sy + spriteH / 2);
            return;
        }

        // Flash de impacto
        if (flashColor != null && flashAlpha > 0f) {
            g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(),
                                  (int)(flashAlpha * 220)));
            g2.fillRoundRect(0, 0, w, h, 10, 10);
        }

        // Números flotantes
        for (FloatingNum fn : floatingNums) {
            g2.setFont(Theme.labelFont(15f));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (int)(fn.x - fm.stringWidth(fn.text) / 2f);
            int ty = (int) fn.y;
            // sombra
            g2.setColor(new Color(0, 0, 0, (int)(fn.alpha * 180)));
            g2.drawString(fn.text, tx + 1, ty + 1);
            // texto
            g2.setColor(new Color(fn.color.getRed(), fn.color.getGreen(), fn.color.getBlue(),
                                  (int)(fn.alpha * 255)));
            g2.drawString(fn.text, tx, ty);
        }
    }
}
