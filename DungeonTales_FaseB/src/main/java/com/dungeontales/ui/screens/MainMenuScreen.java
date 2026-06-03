package com.dungeontales.ui.screens;

import com.dungeontales.save.SaveManager;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;

/** Pantalla de inicio con logo y opciones de partida. */
public class MainMenuScreen extends JPanel {

    public interface Listener {
        void onNewGame();
        void onLoadGame();
    }

    public MainMenuScreen(Listener listener) {
        setBackground(Theme.BG_DARK);
        setLayout(new GridBagLayout());

        JPanel content = new JPanel();
        content.setBackground(Theme.BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Logo
        JLabel subtitle = new JLabel("PARADIGMA ORIENTADO A OBJETOS  —  TPO", SwingConstants.CENTER);
        subtitle.setFont(Theme.bodyFont(11f));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("DUNGEON TALES", SwingConstants.CENTER);
        title.setFont(Theme.titleFont(52f));
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("RPG POR TURNOS", SwingConstants.CENTER);
        tagline.setFont(Theme.labelFont(16f));
        tagline.setForeground(Theme.TEXT_SECONDARY);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Separador
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(240, 1));
        sep.setForeground(Theme.SEPARATOR);

        // Botones
        JButton newGameBtn = new JButton("Nueva Partida");
        Theme.styleMenuButton(newGameBtn);
        newGameBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        newGameBtn.addActionListener(e -> listener.onNewGame());

        JButton loadBtn = new JButton("Cargar Partida");
        Theme.styleMenuButton(loadBtn);
        loadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadBtn.setEnabled(SaveManager.saveExists());
        if (!SaveManager.saveExists())
            loadBtn.setForeground(Theme.TEXT_MUTED);
        loadBtn.addActionListener(e -> listener.onLoadGame());

        // Version
        JLabel version = new JLabel("v1.0  —  Fase B", SwingConstants.CENTER);
        version.setFont(Theme.bodyFont(10f));
        version.setForeground(Theme.TEXT_MUTED);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Ensamblar
        content.add(Box.createVerticalStrut(40));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(12));
        content.add(title);
        content.add(Box.createVerticalStrut(6));
        content.add(tagline);
        content.add(Box.createVerticalStrut(40));
        content.add(sep);
        content.add(Box.createVerticalStrut(40));
        content.add(newGameBtn);
        content.add(Box.createVerticalStrut(14));
        content.add(loadBtn);
        content.add(Box.createVerticalStrut(60));
        content.add(version);

        add(content);
    }
}
