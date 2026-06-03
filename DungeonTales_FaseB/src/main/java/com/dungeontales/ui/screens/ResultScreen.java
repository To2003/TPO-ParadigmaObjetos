package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.core.model.items.Potion;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;

// ─── ResultScreen ─────────────────────────────────────────────────────────────
public class ResultScreen extends JPanel {
    public interface Listener { void onContinue(); }

    public ResultScreen(GameState state, Listener listener) {
        setBackground(Theme.BG_DARK);
        setLayout(new GridBagLayout());

        JPanel content = new JPanel();
        content.setBackground(Theme.BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        addCentered(content, makeLabel("¡¡ VICTORIA !!", 32f, Theme.HEAL_COLOR));
        content.add(Box.createVerticalStrut(20));

        if (state.getLastLevelUps() != null && !state.getLastLevelUps().isEmpty()) {
            addCentered(content, makeLabel("¡¡ SUBIDA DE NIVEL !!", 24f, Theme.HEAL_COLOR));
            content.add(Box.createVerticalStrut(10));
            for (String lu : state.getLastLevelUps()) {
                addCentered(content, makeLabel(lu, 16f, Theme.TEXT_PRIMARY));
            }
            content.add(Box.createVerticalStrut(20));
        }

        // Recompensas
        addCentered(content, makeLabel("RECOMPENSAS", 14f, Theme.TEXT_SECONDARY));
        content.add(Box.createVerticalStrut(10));
        addCentered(content, makeLabel("EXP:  +" + state.getLastExpGained(), 16f, Theme.EXP_COLOR));
        addCentered(content, makeLabel("Oro:  +" + state.getLastGoldGained() + " GP", 16f, Theme.GOLD_COLOR));
        if (state.getLastItemFound() != null)
            addCentered(content, makeLabel("Item: " + state.getLastItemFound(), 16f, new Color(0xC0, 0x60, 0xE0)));

        content.add(Box.createVerticalStrut(20));

        // Estado party
        addCentered(content, makeLabel("ESTADO DE LA PARTY", 13f, Theme.TEXT_SECONDARY));
        content.add(Box.createVerticalStrut(8));
        for (Character c : state.getParty()) {
            String txt = c.getName() + "  —  " +
                (c.isAlive() ? "HP " + c.getHp() + "/" + c.getHpMax() + "  Nv." + c.getLevel() : "CAÍDO");
            Color col = c.isAlive() ? Theme.TEXT_PRIMARY : Theme.TEXT_MUTED;
            addCentered(content, makeLabel(txt, 13f, col));
        }

        content.add(Box.createVerticalStrut(30));

        JButton continueBtn = new JButton("Continuar al mapa");
        Theme.styleMenuButton(continueBtn);
        continueBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        continueBtn.addActionListener(e -> listener.onContinue());
        content.add(continueBtn);

        add(content);
    }

    private JLabel makeLabel(String text, float size, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(size > 20 ? Theme.titleFont(size) : Theme.labelFont(size));
        l.setForeground(color);
        return l;
    }

    private void addCentered(JPanel p, JLabel l) {
        l.setAlignmentX(Component.CENTER_ALIGNMENT); p.add(l);
    }
}
