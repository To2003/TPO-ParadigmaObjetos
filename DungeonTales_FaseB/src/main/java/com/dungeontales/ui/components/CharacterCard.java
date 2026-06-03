package com.dungeontales.ui.components;

import com.dungeontales.core.model.StatusEffect;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.util.SpriteLoader;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CharacterCard extends JPanel {

    private static final int CARD_W   = 250;
    private static final int CARD_H   = 370;
    private static final int SPRITE_W = 210;
    private static final int SPRITE_H = 270;

    // ── Modelo ───────────────────────────────────────────────────────────
    private final Character   character;
    private final AnimatedBar hpBar;
    private final AnimatedBar expBar;
    private final JLabel      hpLabel;
    private       JLabel      classLabel;
    private       JLabel      paLabel;
    private boolean isActive = false;
    private boolean isDead   = false;
    private BufferedImage sprite;

    // ── Animación de sprites ─────────────────────────────────────────────
    private final CharacterAnimator animator;
    private boolean deathOverlayReady = false;

    // ── Flash de impacto ─────────────────────────────────────────────────
    private Color   flashColor;
    private float   flashAlpha = 0f;
    private Timer   flashTimer;

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

    public CharacterCard(Character character) {
        this.character = character;

        setPreferredSize(new Dimension(CARD_W, CARD_H));
        setOpaque(true);
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        sprite   = SpriteLoader.getCharacterSprite(character.getSpriteName(), SPRITE_W, SPRITE_H);
        animator = new CharacterAnimator(character.getSpriteName(), SPRITE_W, SPRITE_H);
        animator.setOnFrameChange(this::repaint);

        // ── Panel superior: nombre y clase ───────────────────────────────
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));

        JLabel nameLabel = new JLabel(character.getName());
        nameLabel.setFont(Theme.labelFont(14f));
        nameLabel.setForeground(Theme.TEXT_PRIMARY);

        classLabel = new JLabel(character.getClassName() + "  Nv." + character.getLevel());
        classLabel.setFont(Theme.bodyFont(11f));
        classLabel.setForeground(Theme.TEXT_SECONDARY);

        topPanel.add(nameLabel);
        topPanel.add(classLabel);

        // ── Panel inferior: HP y EXP (PA se muestra con pips en paint) ──
        JPanel statsPanel = new JPanel();
        statsPanel.setOpaque(false);
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 6, 4));

        hpLabel = makeStatLabel("HP " + character.getHp() + "/" + character.getHpMax());
        hpBar   = new AnimatedBar(AnimatedBar.BarType.HP, character.getHpMax(), character.getHp());
        paLabel = makeStatLabel("PA " + character.getPa() + "/" + character.getPaMax());
        expBar  = new AnimatedBar(AnimatedBar.BarType.EXP, character.getExpToNext(), character.getExp());

        hpBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 7));
        expBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));

        statsPanel.add(hpLabel);
        statsPanel.add(Box.createVerticalStrut(1));
        statsPanel.add(hpBar);
        statsPanel.add(Box.createVerticalStrut(5));
        statsPanel.add(paLabel);
        statsPanel.add(Box.createVerticalStrut(6));
        statsPanel.add(expBar);

        add(topPanel,   BorderLayout.NORTH);
        add(statsPanel, BorderLayout.SOUTH);

        // Timer de animación a ~60fps para números flotantes
        animTimer = new Timer(16, e -> tickAnimations());
    }

    // ── API pública ──────────────────────────────────────────────────────

    public String getCharacterName() { return character.getName(); }

    public void setActive(boolean active) {
        this.isActive = active;
        repaint();
    }

    public void refresh() {
        isDead = !character.isAlive();
        hpLabel.setText("HP " + character.getHp() + "/" + character.getHpMax());
        paLabel.setText("PA " + character.getPa() + "/" + character.getPaMax());
        classLabel.setText(character.getClassName() + "  Nv." + character.getLevel());
        hpBar.setValue(character.getHp(), character.getHpMax());
        expBar.setValue(character.getExp(), character.getExpToNext());
        repaint();
    }

    /** Flash rojo + número de daño flotante. */
    public void flashDamage(int amount) {
        startFlash(amount > 30 ? new Color(220, 40, 30) : new Color(180, 60, 50), 18);
        spawnNumber("-" + amount, Theme.DMG_COLOR);
    }

    /** Flash verde + número de curación flotante. */
    public void flashHeal(int amount) {
        startFlash(new Color(30, 160, 70), 14);
        spawnNumber("+" + amount, Theme.HEAL_COLOR);
    }

    /** Flash morado para efectos de estado. */
    public void flashStatus(String label) {
        startFlash(new Color(100, 50, 160), 12);
        spawnNumber(label, Theme.STUN_COLOR);
    }

    /** Sin flash, solo texto de esquive. */
    public void flashDodge() {
        spawnNumber("ESQUIVA", Theme.EVASION_COLOR);
    }

    // ── Animaciones de sprite ────────────────────────────────────────────

    public void playAttack() { animator.play(CharacterAnimator.State.ATTACK); }
    public void playSkill()  { animator.play(CharacterAnimator.State.SKILL); }
    public void playHit()    { animator.play(CharacterAnimator.State.HIT); }

    public void playDeath() {
        deathOverlayReady = false;
        animator.setOnAnimComplete(() -> {
            deathOverlayReady = true;
            repaint();
        });
        animator.play(CharacterAnimator.State.DEATH);
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
        floatingNums.add(new FloatingNum(text, color, CARD_W / 2f, CARD_H / 2f - 20));
        if (!animTimer.isRunning()) animTimer.start();
    }

    private void tickAnimations() {
        Iterator<FloatingNum> it = floatingNums.iterator();
        while (it.hasNext()) {
            FloatingNum fn = it.next();
            fn.y    += fn.vy;
            fn.vy   *= 0.96f;          // desacelera
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

        // Fondo
        g2.setColor(Theme.BG_CARD);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

        // Borde adaptativo
        float hpRatio = character.getHpMax() > 0 ? (float) character.getHp() / character.getHpMax() : 0f;
        Color borderColor = isActive        ? Theme.BORDER_ACTIVE
                          : hpRatio < 0.25f ? Theme.HP_LOW
                          :                   Theme.BORDER;
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(isActive ? 2.5f : 1f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
        g2.setStroke(new BasicStroke(1f));

        // Sprite (animado si hay sheet, estático como fallback)
        int sx = (w - SPRITE_W) / 2;
        int sy = (h - SPRITE_H) / 2 - 40;
        BufferedImage frame = animator.getCurrentFrame();
        g2.drawImage(frame != null ? frame : sprite, sx, sy, this);

        // Overlay de muerto: espera a que la animación de muerte complete
        if (isDead && deathOverlayReady) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(0, 0, w, h, 10, 10);
            g2.setColor(Theme.DMG_COLOR);
            g2.setFont(Theme.labelFont(13f));
            FontMetrics fm = g2.getFontMetrics();
            String txt = "CAÍDO";
            g2.drawString(txt, (w - fm.stringWidth(txt)) / 2, h / 2 + fm.getAscent() / 2);
            return;
        }

        // Chips de efectos de estado
        if (!character.getEffects().isEmpty()) {
            int ex = 6, ey = sy + SPRITE_H - 18;
            g2.setFont(Theme.bodyFont(9f));
            for (StatusEffect e : character.getEffects()) {
                if (!e.isActive()) continue;
                Color c = switch (e.getType()) {
                    case POISON     -> Theme.POISON_COLOR;
                    case STUN       -> Theme.STUN_COLOR;
                    case DEFENSE_UP -> Theme.SHIELD_COLOR;
                    case ATTACK_UP  -> e.getValue() >= 0 ? Theme.FURY_COLOR : Theme.DMG_COLOR;
                    case EVASION    -> Theme.EVASION_COLOR;
                };
                String txt = e.getDisplayName() + "(" + e.getDuration() + ")";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(txt) + 6;
                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillRoundRect(ex, ey, tw, 13, 4, 4);
                g2.setColor(c);
                g2.drawString(txt, ex + 3, ey + 10);
                ex += tw + 3;
                if (ex > w - 20) break;
            }
        }

        // PA pips — superpuestos en la zona baja del sprite
        drawPAPips(g2, w, sy);

        // Flash de impacto
        if (flashColor != null && flashAlpha > 0f) {
            g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(),
                                  (int)(flashAlpha * 220)));
            g2.fillRoundRect(0, 0, w, h, 10, 10);
        }

        // Números flotantes (encima de todo)
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

    /** Dibuja los pips de PA superpuestos en la parte baja del sprite. */
    private void drawPAPips(Graphics2D g2, int cardW, int spriteY) {
        int pa    = character.getPa();
        int paMax = character.getPaMax();
        if (paMax <= 0) return;

        // Tamaño de pip según cantidad máxima
        int pipSize = paMax <= 6 ? 14 : paMax <= 10 ? 11 : 9;
        int gap     = 4;
        int totalW  = paMax * pipSize + (paMax - 1) * gap;
        int startX  = (cardW - totalW) / 2;
        int startY  = spriteY + SPRITE_H - 28;  // sobre el sprite, encima de los chips de estado

        for (int i = 0; i < paMax; i++) {
            boolean filled = i < pa;

            // Sombra del pip
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillOval(startX + 1, startY + 1, pipSize, pipSize);

            // Relleno
            if (filled) {
                // Pip lleno: gradiente simulado con dos círculos
                Color pipFill  = isActive ? new Color(50, 160, 220) : Theme.PA_BAR;
                Color pipGlow  = isActive ? new Color(90, 200, 255) : new Color(60, 140, 200);
                g2.setColor(pipFill);
                g2.fillOval(startX, startY, pipSize, pipSize);
                // brillo interior
                g2.setColor(new Color(pipGlow.getRed(), pipGlow.getGreen(), pipGlow.getBlue(), 120));
                g2.fillOval(startX + 2, startY + 1, pipSize / 2, pipSize / 2);
            } else {
                // Pip vacío: oscuro con borde sutil
                g2.setColor(new Color(20, 20, 35, 200));
                g2.fillOval(startX, startY, pipSize, pipSize);
            }

            // Borde del pip
            g2.setColor(filled
                ? new Color(80, 180, 230, 180)
                : new Color(50, 50, 70, 160));
            g2.drawOval(startX, startY, pipSize, pipSize);

            startX += pipSize + gap;
        }
    }

    private JLabel makeStatLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bodyFont(10f));
        l.setForeground(Theme.TEXT_SECONDARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
