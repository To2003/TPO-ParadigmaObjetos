package com.dungeontales.ui.components;

import com.dungeontales.core.battle.BattleEvent;
import com.dungeontales.util.Theme;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/** Log de combate scrollable con colores por tipo de evento. */
public class BattleLog extends JScrollPane {

    private final JTextPane textPane;
    private final StyledDocument doc;

    public BattleLog() {
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBackground(Theme.BG_ACTION_BAR);
        textPane.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        doc = textPane.getStyledDocument();

        setViewportView(textPane);
        setPreferredSize(new Dimension(0, 90));
        setBorder(BorderFactory.createLineBorder(Theme.SEPARATOR, 1));
        getVerticalScrollBar().setUnitIncrement(12);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /** Agrega un evento al log con el color correspondiente. */
    public void addEvent(BattleEvent event) {
        Color c = switch (event.type) {
            case DAMAGE_DEALT, POISON_TICK     -> Theme.DMG_COLOR;
            case HEAL_APPLIED, REGEN_TICK      -> Theme.HEAL_COLOR;
            case BATTLE_WON                    -> new Color(0x80, 0xFF, 0x80);
            case BATTLE_LOST, CHARACTER_DIED   -> new Color(0xFF, 0x50, 0x50);
            case ENEMY_DIED                    -> new Color(0xFF, 0xA0, 0x40);
            case LEVEL_UP                      -> Theme.GOLD_COLOR;
            case STATUS_APPLIED                -> new Color(0xA0, 0xC0, 0xFF);
            case DAMAGE_DODGED                 -> Theme.EVASION_COLOR;
            case TURN_START                    -> Theme.TEXT_PRIMARY;
            default                            -> Theme.TEXT_SECONDARY;
        };
        appendLine(event.message, c);
    }

    public void addMessage(String msg, Color color) {
        appendLine(msg, color);
    }

    public void clear() {
        try { doc.remove(0, doc.getLength()); }
        catch (BadLocationException ignored) {}
    }

    private void appendLine(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, color);
                StyleConstants.setFontFamily(attrs, "Monospaced");
                StyleConstants.setFontSize(attrs, 11);
                doc.insertString(doc.getLength(), text + "\n", attrs);
                // Auto-scroll al final
                textPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }
}
