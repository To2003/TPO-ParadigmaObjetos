package com.dungeontales.util;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

/**
 * Paleta de colores y fuentes estilo Darkest Dungeon 2.
 * Todos los componentes de UI referencian estas constantes.
 */
public class Theme {

    // ── Fondos ────────────────────────────────────────────────────────────
    public static final Color BG_DARK       = new Color(0x00, 0x00, 0x00);  // negro puro para camuflar sprites
    public static final Color BG_PANEL      = new Color(0x1A, 0x14, 0x10);  // panel oscuro
    public static final Color BG_CARD       = new Color(0x22, 0x1A, 0x14);  // card de personaje
    public static final Color BG_CARD_ENEMY = new Color(0x1E, 0x12, 0x0E);  // card de enemigo
    public static final Color BG_ACTION_BAR = new Color(0x14, 0x10, 0x0C);  // barra de acciones
    public static final Color BG_BUTTON          = new Color(0x2A, 0x1E, 0x16);  // botón normal
    public static final Color BG_BUTTON_HOV      = new Color(0x3A, 0x2A, 0x1E);  // botón hover
    public static final Color BG_BUTTON_PA       = new Color(0x12, 0x2A, 0x3A);  // botón habilidad PA
    public static final Color BG_BUTTON_ATTACK   = new Color(0x3D, 0x14, 0x0C);  // botón ataque
    public static final Color BG_BUTTON_ATK_HOV  = new Color(0x5C, 0x20, 0x12);  // botón ataque hover
    public static final Color BG_BUTTON_ITEM     = new Color(0x0E, 0x28, 0x18);  // botón ítem
    public static final Color BG_BUTTON_ITEM_HOV = new Color(0x1A, 0x3E, 0x26);  // botón ítem hover

    // ── Bordes ────────────────────────────────────────────────────────────
    public static final Color BORDER        = new Color(0x4A, 0x3A, 0x2E);  // borde estándar
    public static final Color BORDER_ACTIVE = new Color(0xC8, 0xA0, 0x60);  // borde personaje activo
    public static final Color BORDER_ENEMY  = new Color(0x7A, 0x2A, 0x1A);  // borde enemigo

    // ── Textos ────────────────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY   = new Color(0xD4, 0xC0, 0x90);  // dorado principal
    public static final Color TEXT_SECONDARY = new Color(0x8A, 0x70, 0x50);  // texto secundario
    public static final Color TEXT_MUTED     = new Color(0x5A, 0x48, 0x38);  // texto atenuado
    public static final Color TEXT_WHITE     = new Color(0xE8, 0xE0, 0xD0);  // blanco cálido
    public static final Color TEXT_ENEMY     = new Color(0xE0, 0x70, 0x60);  // nombre enemigo

    // ── Barras ────────────────────────────────────────────────────────────
    public static final Color HP_FULL   = new Color(0xC0, 0x35, 0x28);  // HP alto
    public static final Color HP_MED    = new Color(0xD0, 0x80, 0x20);  // HP medio
    public static final Color HP_LOW    = new Color(0xE0, 0x30, 0x20);  // HP bajo (más rojo)
    public static final Color PA_BAR    = new Color(0x28, 0x80, 0xB0);  // barra PA azul
    public static final Color BAR_BG    = new Color(0x2A, 0x20, 0x1A);  // fondo de barras
    public static final Color EXP_BAR   = new Color(0x80, 0xA0, 0x30);  // barra experiencia

    // ── Efectos de estado ─────────────────────────────────────────────────
    public static final Color POISON_COLOR  = new Color(0x40, 0xC0, 0x60);
    public static final Color STUN_COLOR    = new Color(0xE0, 0xD0, 0x40);
    public static final Color SHIELD_COLOR  = new Color(0x40, 0x80, 0xE0);
    public static final Color FURY_COLOR    = new Color(0xE0, 0x50, 0x30);
    public static final Color EVASION_COLOR = new Color(0xA0, 0x60, 0xE0);

    // ── Colores de acción / feedback ──────────────────────────────────────
    public static final Color DMG_COLOR     = new Color(0xFF, 0x60, 0x40);  // número de daño
    public static final Color HEAL_COLOR    = new Color(0x60, 0xE0, 0x80);  // número de curación
    public static final Color GOLD_COLOR    = new Color(0xF0, 0xC0, 0x40);  // oro
    public static final Color EXP_COLOR     = new Color(0x80, 0xC0, 0xF0);  // experiencia

    // ── Separador ─────────────────────────────────────────────────────────
    public static final Color SEPARATOR     = new Color(0x3A, 0x2A, 0x20);

    // ── Fuentes ───────────────────────────────────────────────────────────
    private static Font baseFont;
    private static Font titleFont;

    static {
        loadFonts();
    }

    private static void loadFonts() {
        // Intenta cargar fuente personalizada (Medieval Sharp de Google Fonts)
        // Si no existe en resources/fonts/, usa fallback del sistema
        try {
            InputStream is = Theme.class.getResourceAsStream("/fonts/MedievalSharp.ttf");
            if (is != null) {
                titleFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(24f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(titleFont);
            }
        } catch (Exception e) {
            titleFont = null;
        }

        try {
            InputStream is = Theme.class.getResourceAsStream("/fonts/Cinzel-Regular.ttf");
            if (is != null) {
                baseFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(14f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
            }
        } catch (Exception e) {
            baseFont = null;
        }
    }

    /** Fuente para títulos grandes (logo, jefes). */
    public static Font titleFont(float size) {
        if (titleFont != null) return titleFont.deriveFont(size);
        return new Font("Serif", Font.BOLD, (int) size);
    }

    /** Fuente para nombres y etiquetas importantes. */
    public static Font labelFont(float size) {
        if (baseFont != null) return baseFont.deriveFont(size);
        return new Font("SansSerif", Font.BOLD, (int) size);
    }

    /** Fuente para texto de body/descripción. */
    public static Font bodyFont(float size) {
        return new Font("SansSerif", Font.PLAIN, (int) size);
    }

    /** Fuente monospace para logs de combate. */
    public static Font logFont(float size) {
        return new Font("Monospaced", Font.PLAIN, (int) size);
    }

    // ── Helpers de estilo para botones ────────────────────────────────────
    public static void styleButton(JButton btn) {
        btn.setBackground(BG_BUTTON);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFont(labelFont(12f));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON_HOV);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_ACTIVE, 1),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }
        });
    }

    public static void stylePAButton(JButton btn) {
        styleButton(btn);
        btn.setBackground(BG_BUTTON_PA);
        btn.setForeground(new Color(0x70, 0xC0, 0xE8));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2A, 0x5A, 0x80), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
    }

    /** Botón de ataque básico — rojo/naranja, grande. */
    public static void styleAttackButton(JButton btn) {
        btn.setBackground(BG_BUTTON_ATTACK);
        btn.setForeground(new Color(0xFF, 0x90, 0x60));
        btn.setFont(labelFont(13f));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x8A, 0x2E, 0x16), 1),
            BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON_ATK_HOV);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xC0, 0x50, 0x28), 1),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON_ATTACK);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x8A, 0x2E, 0x16), 1),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
        });
    }

    /** Botón de habilidad PA — azul, grande. */
    public static void stylePAActionButton(JButton btn) {
        btn.setBackground(BG_BUTTON_PA);
        btn.setForeground(new Color(0x70, 0xC0, 0xE8));
        btn.setFont(labelFont(13f));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2A, 0x5A, 0x80), 1),
            BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(0x1C, 0x3E, 0x56));
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x40, 0x80, 0xB0), 1),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON_PA);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x2A, 0x5A, 0x80), 1),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
        });
    }

    /** Botón de ítem/poción — verde, grande. */
    public static void styleItemButton(JButton btn) {
        btn.setBackground(BG_BUTTON_ITEM);
        btn.setForeground(new Color(0x70, 0xD0, 0x90));
        btn.setFont(labelFont(13f));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x28, 0x60, 0x38), 1),
            BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON_ITEM_HOV);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x40, 0x90, 0x55), 1),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON_ITEM);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x28, 0x60, 0x38), 1),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
        });
    }

    /** Botón de terminar turno — atenuado, grande. */
    public static void styleEndTurnButton(JButton btn) {
        btn.setBackground(BG_BUTTON);
        btn.setForeground(TEXT_MUTED);
        btn.setFont(labelFont(13f));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SEPARATOR, 1),
            BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON_HOV);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SEPARATOR, 1),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
        });
    }

    public static void styleMenuButton(JButton btn) {
        btn.setBackground(BG_BUTTON);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFont(labelFont(16f));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(12, 32, 12, 32)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON_HOV);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_ACTIVE, 1),
                    BorderFactory.createEmptyBorder(12, 32, 12, 32)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(12, 32, 12, 32)
                ));
            }
        });
    }
}
