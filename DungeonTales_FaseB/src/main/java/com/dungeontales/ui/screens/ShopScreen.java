package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.core.model.items.Potion;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;

public class ShopScreen extends JPanel {
    public interface Listener { void onDone(); }

    private static final Object[][] SHOP_ITEMS = {
        { Potion.smallHeal(),      20  },
        { Potion.largeHeal(),      40  },
        { Potion.antidote(),       15  },
        { Potion.strengthPotion(), 35  },
        { Potion.revivePotion(),   120 },
    };

    public ShopScreen(GameState state, Listener listener) {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setBackground(Theme.BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel title = new JLabel("TIENDA DEL MERCADER", SwingConstants.CENTER);
        title.setFont(Theme.labelFont(20f));
        title.setForeground(new Color(0xA0, 0x60, 0xE0));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);

        JLabel goldLabel = new JLabel("Oro disponible: " + state.getGold() + " GP");
        goldLabel.setFont(Theme.labelFont(13f));
        goldLabel.setForeground(Theme.GOLD_COLOR);
        goldLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(10));
        content.add(goldLabel);
        content.add(Box.createVerticalStrut(20));

        for (Object[] entry : SHOP_ITEMS) {
            Potion p = (Potion) entry[0];
            int cost = (int) entry[1];
            JButton btn = new JButton(p.getName() + "  —  " + p.getDescription() + "   [" + cost + " GP]");
            Theme.styleButton(btn);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(480, 40));
            btn.setEnabled(state.getGold() >= cost);
            btn.addActionListener(e -> {
                if (!state.spendGold(cost)) return;
                state.getInventory().addItem(p);
                goldLabel.setText("Oro disponible: " + state.getGold() + " GP");
                btn.setEnabled(state.getGold() >= cost);
                JOptionPane.showMessageDialog(this,
                    p.getName() + " añadida al inventario.", "Compra", JOptionPane.PLAIN_MESSAGE);
            });
            content.add(btn);
            content.add(Box.createVerticalStrut(8));
        }

        content.add(Box.createVerticalStrut(20));
        JButton done = new JButton("Salir de la tienda");
        Theme.styleMenuButton(done);
        done.setAlignmentX(Component.CENTER_ALIGNMENT);
        done.addActionListener(e -> listener.onDone());
        content.add(done);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(Theme.BG_DARK);
        scroll.getViewport().setBackground(Theme.BG_DARK);
        add(scroll, BorderLayout.CENTER);
    }
}
