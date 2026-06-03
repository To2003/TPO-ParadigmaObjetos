package com.dungeontales;

import com.dungeontales.ui.GameWindow;
import com.dungeontales.util.Theme;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Activar renderizado de texto suavizado
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Usar Look & Feel del sistema para que los diálogos se vean bien
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            // Sobreescribir colores globales de Swing con los del Theme
            UIManager.put("Panel.background",          Theme.BG_DARK);
            UIManager.put("OptionPane.background",     Theme.BG_PANEL);
            UIManager.put("OptionPane.messageForeground", Theme.TEXT_PRIMARY);
            UIManager.put("Button.background",         Theme.BG_BUTTON);
            UIManager.put("Button.foreground",         Theme.TEXT_PRIMARY);
            UIManager.put("ScrollPane.background",     Theme.BG_DARK);
            UIManager.put("ScrollBar.background",      Theme.BG_PANEL);
            UIManager.put("ScrollBar.thumb",           Theme.BORDER);
        } catch (Exception ignored) {}

        // Arrancar en el Event Dispatch Thread
        SwingUtilities.invokeLater(GameWindow::new);
    }
}
