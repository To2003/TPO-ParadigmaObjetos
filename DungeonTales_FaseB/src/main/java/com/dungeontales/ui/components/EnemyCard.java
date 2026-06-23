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
        float x, y, alpha, vy, fontSize;
        FloatingNum(String t, Color c, float x, float y, float fontSize) {
            this.text = t; this.color = c;
            this.x = x; this.y = y;
            this.alpha = 1f; this.vy = -3.2f;
            this.fontSize = fontSize;
        }
    }
    private final List<FloatingNum> floatingNums = new ArrayList<>();
    private Timer animTimer;

    public EnemyCard(Enemy enemy) { this(enemy, 0); }

    public EnemyCard(Enemy enemy, int maxWidth) {
        this.enemy = enemy;

        String n = enemy.getName().toLowerCase();
        int bW, bH, bSW, bSH;
        if (n.contains("sombra") || n.contains("rey") || n.contains("señor") || n.contains("tinieblas")) {
            // Jefe principal
            bW = 325; bH = 500; bSW = 316; bSH = 486;
        } else if (n.contains("troll")   || n.contains("ogro")    || n.contains("golem")
                || n.contains("vampiro") || n.contains("eterno")  || n.contains("esqueleto gigante")
                || n.contains("caballero")) {
            // Mini-boss
            bW = 265; bH = 498; bSW = 257; bSH = 484;
        } else if (n.contains("goblin")) {
            // Goblin
            bW = 185; bH = 354; bSW = 179; bSH = 343;
        } else {
            // Enemigo normal
            bW = 225; bH = 438; bSW = 217; bSH = 425;
        }
        if (maxWidth > 0 && bW > maxWidth) {
            double s = (double) maxWidth / bW;
            cardW = maxWidth;         cardH = (int)(bH  * s);
            spriteW = (int)(bSW * s); spriteH = (int)(bSH * s);
        } else {
            cardW = bW; cardH = bH; spriteW = bSW; spriteH = bSH;
        }

        setPreferredSize(new Dimension(cardW, cardH));
        setOpaque(false);
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        sprite = SpriteLoader.getEnemySprite(enemy.getSpriteName(), spriteW, spriteH);

        // Bars kept as non-visual instances for refresh() compatibility
        hpBar      = new AnimatedBar(AnimatedBar.BarType.HP, enemy.getHpMax(), enemy.getHp());
        hpLabel    = new JLabel();
        actionHint = new JLabel();

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
        float size = amount >= 50 ? 30f : amount >= 25 ? 25f : 21f;
        spawnNumber("-" + amount, Theme.DMG_COLOR, size);
    }

    /** Flash verde + número de curación (regen de troll, etc.). */
    public void flashHeal(int amount) {
        startFlash(new Color(30, 160, 70), 14);
        spawnNumber("+" + amount, Theme.HEAL_COLOR, 21f);
    }

    /** Texto de esquive. */
    public void flashDodge() {
        spawnNumber("ESQUIVA", Theme.EVASION_COLOR, 18f);
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

    private void spawnNumber(String text, Color color, float fontSize) {
        float spawnY = cardH - spriteH + spriteH * 0.35f;
        floatingNums.add(new FloatingNum(text, color, cardW / 2f, spawnY, fontSize));
        if (!animTimer.isRunning()) animTimer.start();
    }

    private void tickAnimations() {
        Iterator<FloatingNum> it = floatingNums.iterator();
        while (it.hasNext()) {
            FloatingNum fn = it.next();
            fn.y    += fn.vy;
            fn.vy   *= 0.94f;
            fn.alpha -= 0.020f;
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
                new java.awt.geom.Point2D.Float(w / 2f, h - 50),
                80f,
                new float[]{0f, 1f},
                new Color[]{new Color(200, 40, 40, 140), new Color(0,0,0,0)}
            ));
            g2.fillOval(w/2 - 80, h - 70, 160, 50);
        }

        // Sombra en el suelo — a nivel de los pies del sprite
        Composite savedComp = g2.getComposite();
        int shadowW = (int)(spriteW * 0.75f);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g2.setColor(Color.BLACK);
        g2.fillOval((w - shadowW) / 2, h - 50, shadowW, 16);
        g2.setComposite(savedComp);

        // Underglow ambiental cálido (encima de la sombra)
        int glowR = (int)(spriteW * 0.55f);
        g2.setPaint(new RadialGradientPaint(
            new java.awt.geom.Point2D.Float(w / 2f, h - 50),
            glowR,
            new float[]{0f, 1f},
            new Color[]{new Color(205, 160, 70, 55), new Color(0, 0, 0, 0)}
        ));
        g2.fillOval(w / 2 - glowR, h - 68, glowR * 2, 40);

        // Sprite — pies en la parte baja de la card
        int sx = (w - spriteW) / 2;
        int sy = h - spriteH - 2;
        if (sprite != null) {
            // Rim-glow: copias offset desenfocadas a 20% alpha para contorno luminoso
            Composite orig = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
            g2.drawImage(sprite, sx - 2, sy,     this);
            g2.drawImage(sprite, sx + 2, sy,     this);
            g2.drawImage(sprite, sx,     sy - 2, this);
            g2.drawImage(sprite, sx,     sy + 2, this);
            g2.setComposite(orig);
            g2.drawImage(sprite, sx, sy, this);
        }

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
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (FloatingNum fn : floatingNums) {
            Font f = Theme.labelFont(fn.fontSize).deriveFont(Font.BOLD);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (int)(fn.x - fm.stringWidth(fn.text) / 2f);
            int ty = (int) fn.y;
            int a  = (int)(fn.alpha * 255);
            // outline negro en 8 direcciones
            g2.setColor(new Color(0, 0, 0, Math.min(255, (int)(fn.alpha * 200))));
            for (int ox = -2; ox <= 2; ox += 2)
                for (int oy = -2; oy <= 2; oy += 2)
                    if (ox != 0 || oy != 0) g2.drawString(fn.text, tx + ox, ty + oy);
            // texto coloreado
            g2.setColor(new Color(fn.color.getRed(), fn.color.getGreen(), fn.color.getBlue(), a));
            g2.drawString(fn.text, tx, ty);
        }
    }
}
