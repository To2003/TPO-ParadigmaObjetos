package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;

public class RestScreen extends JPanel {
    public interface Listener { void onDone(); }

    public RestScreen(GameState state, Listener listener) {
        setBackground(Theme.BG_DARK);
        setLayout(new GridBagLayout());

        JPanel content = new JPanel();
        content.setBackground(Theme.BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JLabel title = new JLabel("CAMPAMENTO DE DESCANSO", SwingConstants.CENTER);
        title.setFont(Theme.labelFont(20f));
        title.setForeground(new Color(0x40, 0x80, 0xFF));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(30));

        JButton healAll = new JButton("Curar toda la party  (+30% HP)");
        Theme.styleMenuButton(healAll);
        healAll.setAlignmentX(Component.CENTER_ALIGNMENT);
        healAll.addActionListener(e -> {
            state.getAliveParty().forEach(c -> c.healHp((int)(c.getHpMax() * 0.30)));
            listener.onDone();
        });

        JButton meditate = new JButton("Meditar  (un personaje +50% HP)");
        Theme.styleMenuButton(meditate);
        meditate.setAlignmentX(Component.CENTER_ALIGNMENT);
        meditate.addActionListener(e -> {
            var alive = state.getAliveParty();
            if (alive.size() == 1) {
                alive.get(0).healHp((int)(alive.get(0).getHpMax() * 0.50));
            } else {
                String[] names = alive.stream().map(c ->
                    c.getName() + " (" + c.getHp() + "/" + c.getHpMax() + " HP)").toArray(String[]::new);
                String choice = (String) JOptionPane.showInputDialog(this,
                    "Elegir personaje:", "Meditar", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
                if (choice != null) {
                    for (int i = 0; i < names.length; i++) {
                        if (names[i].equals(choice)) {
                            alive.get(i).healHp((int)(alive.get(i).getHpMax() * 0.50));
                            break;
                        }
                    }
                }
            }
            listener.onDone();
        });

        content.add(healAll);
        content.add(Box.createVerticalStrut(14));
        content.add(meditate);

        add(content);
    }
}
