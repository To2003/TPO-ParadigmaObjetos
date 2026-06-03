package com.dungeontales.ui.components;

import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Barra de progreso animada para HP o PA.
 * Tiene transición suave cuando el valor cambia.
 */
public class AnimatedBar extends JPanel {

    public enum BarType { HP, PA, EXP }

    private final BarType type;
    private int maxValue;
    private int targetValue;
    private float displayValue; // valor animado
    private Timer animTimer;

    private static final int BAR_HEIGHT = 8;
    private static final float ANIM_SPEED = 0.12f;

    public AnimatedBar(BarType type, int max, int current) {
        this.type         = type;
        this.maxValue     = max;
        this.targetValue  = current;
        this.displayValue = current;

        setPreferredSize(new Dimension(100, BAR_HEIGHT + 2));
        setOpaque(false);

        // Timer de animación a 60fps
        animTimer = new Timer(16, e -> {
            float diff = targetValue - displayValue;
            if (Math.abs(diff) < 0.5f) {
                displayValue = targetValue;
                animTimer.stop();
            } else {
                displayValue += diff * ANIM_SPEED;
            }
            repaint();
        });
    }

    /** Actualiza el valor con animación suave. */
    public void setValue(int current, int max) {
        this.maxValue     = max;
        this.targetValue  = Math.max(0, Math.min(current, max));
        if (!animTimer.isRunning()) animTimer.start();
    }

    /** Actualiza sin animación (para inicialización). */
    public void setValueInstant(int current, int max) {
        this.maxValue     = max;
        this.targetValue  = current;
        this.displayValue = current;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = BAR_HEIGHT;
        int y = (getHeight() - h) / 2;

        // Fondo
        g2.setColor(Theme.BAR_BG);
        g2.fillRoundRect(0, y, w, h, 4, 4);

        // Relleno según ratio
        if (maxValue > 0) {
            float ratio = Math.min(1f, displayValue / maxValue);
            int fillW = Math.max(0, (int)(w * ratio));

            Color barColor = switch (type) {
                case HP  -> getHpColor(ratio);
                case PA  -> Theme.PA_BAR;
                case EXP -> Theme.EXP_BAR;
            };

            g2.setColor(barColor);
            if (fillW > 0)
                g2.fillRoundRect(0, y, fillW, h, 4, 4);
        }

        // Borde sutil
        g2.setColor(new Color(0, 0, 0, 60));
        g2.drawRoundRect(0, y, w - 1, h - 1, 4, 4);
    }

    private Color getHpColor(float ratio) {
        if (ratio > 0.5f) return Theme.HP_FULL;
        if (ratio > 0.25f) return Theme.HP_MED;
        return Theme.HP_LOW;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(100, BAR_HEIGHT + 4);
    }
}
