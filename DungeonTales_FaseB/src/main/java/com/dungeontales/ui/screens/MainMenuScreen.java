package com.dungeontales.ui.screens;

import com.dungeontales.save.SaveManager;
import com.dungeontales.util.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** Pantalla principal del juego con fondo atmosférico y título tallado. */
public class MainMenuScreen extends JPanel {

    public interface Listener {
        void onNewGame();
        void onLoadGame();
    }

    private BufferedImage background;

    public MainMenuScreen(Listener listener) {
        loadBackground();
        setLayout(new GridBagLayout());
        setOpaque(true);
        setBackground(new Color(0x04, 0x04, 0x06));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel subtitle   = shadowLabel("PARADIGMA ORIENTADO A OBJETOS  —  TPO",
                                        Theme.labelFont(11f),
                                        new Color(0xD4, 0xC0, 0x88));
        JLabel tagline    = shadowLabel("RPG POR TURNOS",
                                        Theme.labelFont(15f),
                                        new Color(0xF0, 0xD8, 0x90));
        JPanel stoneTitle = buildStoneTitle();
        stoneTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Panel con fondo semitransparente detrás del bloque título
        JPanel titleBlock = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x00, 0x00, 0x00, 110));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            }
        };
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBorder(BorderFactory.createEmptyBorder(14, 32, 14, 32));
        titleBlock.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleBlock.add(subtitle);
        titleBlock.add(Box.createVerticalStrut(10));
        titleBlock.add(buildDecoLine(260));
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(stoneTitle);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(buildDecoLine(260));
        titleBlock.add(Box.createVerticalStrut(10));
        titleBlock.add(tagline);

        JPanel newBtn  = buildMenuButton("Nueva Partida",  true,  () -> listener.onNewGame());
        JPanel loadBtn = buildMenuButton("Cargar Partida", SaveManager.saveExists(),
                                        () -> { if (SaveManager.saveExists()) listener.onLoadGame(); });

        JLabel version = shadowLabel("v1.0  —  Fase B  |  Paradigma Orientado a Objetos",
                                    Theme.bodyFont(10f),
                                    new Color(0xA8, 0x96, 0x6C));
        version.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(Box.createVerticalStrut(20));
        content.add(titleBlock);
        content.add(Box.createVerticalStrut(44));
        content.add(newBtn);
        content.add(Box.createVerticalStrut(12));
        content.add(loadBtn);
        content.add(Box.createVerticalStrut(28));
        content.add(version);

        add(content);
    }


    private void loadBackground() {
        try (InputStream is = getClass().getResourceAsStream("/sprites/backgrounds/menu-bg.png")) {
            if (is != null) background = ImageIO.read(is);
        } catch (Exception ignored) {}
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth(), h = getHeight();

        if (background != null) {
            double sx = (double) w / background.getWidth();
            double sy = (double) h / background.getHeight();
            double s  = Math.max(sx, sy);
            int bw = (int)(background.getWidth()  * s);
            int bh = (int)(background.getHeight() * s);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(background, (w - bw) / 2, (h - bh) / 2, bw, bh, null);
        }

        // Viñeta oscura en bordes
        for (int i = 0; i < 80; i++) {
            int alpha = (int)((float) i / 80 * 160);
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.drawRect(i, i, w - i * 2 - 1, h - i * 2 - 1);
        }

        // Gradiente oscuro en la mitad inferior para que los botones resalten
        GradientPaint grad = new GradientPaint(
            0, h * 0.5f, new Color(0, 0, 0, 0),
            0, h,        new Color(0, 0, 0, 180));
        g2.setPaint(grad);
        g2.fillRect(0, 0, w, h);
    }


    /** Dibuja "DUNGEON TALES" con efecto de piedra tallada. */
    private JPanel buildStoneTitle() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Font font = Theme.titleFont(64f);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics();
                String text = "DUNGEON TALES";
                int tx = (getWidth() - fm.stringWidth(text)) / 2;
                int ty = fm.getAscent() + 10;

                // Capa 1: sombra profunda (abajo-derecha)
                g2.setColor(new Color(0x00, 0x00, 0x00, 200));
                g2.drawString(text, tx + 5, ty + 6);

                // Capa 2: sombra media
                g2.setColor(new Color(0x10, 0x08, 0x04, 180));
                g2.drawString(text, tx + 3, ty + 4);

                // Capa 3: borde de roca oscuro
                g2.setColor(new Color(0x28, 0x1A, 0x0C));
                g2.drawString(text, tx + 2, ty + 2);

                // Capa 4: cuerpo principal de piedra (marrón-gris cálido)
                g2.setColor(new Color(0x7A, 0x62, 0x44));
                g2.drawString(text, tx + 1, ty + 1);

                // Capa 5: color base de roca
                g2.setColor(new Color(0x96, 0x7A, 0x52));
                g2.drawString(text, tx, ty);

                // Capa 6: brillo tallado (arista superior-izquierda, más claro)
                g2.setColor(new Color(0xD4, 0xB8, 0x80, 200));
                g2.drawString(text, tx - 1, ty - 1);

                // Capa 7: destello dorado sutil en el filo superior
                g2.setColor(new Color(0xFF, 0xE8, 0xA0, 90));
                g2.drawString(text, tx - 2, ty - 2);
            }
            @Override public Dimension getPreferredSize() {
                FontMetrics fm = getFontMetrics(Theme.titleFont(64f));
                return new Dimension(fm.stringWidth("DUNGEON TALES") + 20, fm.getHeight() + 30);
            }
            @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        };
    }


    private JPanel buildMenuButton(String label, boolean enabled, Runnable action) {
        Color borderColor = enabled
            ? new Color(0x90, 0x70, 0x38)
            : new Color(0x40, 0x34, 0x24);
        Color bgColor = enabled
            ? new Color(0x10, 0x0C, 0x06, 190)
            : new Color(0x0A, 0x08, 0x04, 160);
        Color textColor = enabled
            ? new Color(0xD4, 0xB8, 0x70)
            : new Color(0x50, 0x44, 0x30);

        JPanel btn = new JPanel(new GridBagLayout()) {
            boolean hover = false;

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                Color bg = hover && enabled
                    ? new Color(0x20, 0x18, 0x0A, 210)
                    : bgColor;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);

                g2.setColor(hover && enabled
                    ? new Color(0xC8, 0xA0, 0x50)
                    : borderColor);
                g2.setStroke(new BasicStroke(hover ? 1.8f : 1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                // Borde interior sutil
                g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                                    borderColor.getBlue(), 50));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(2, 2, w - 5, h - 5, 6, 6);

                // Decoraciones de esquina
                int cs = 5;
                g2.setColor(hover && enabled ? new Color(0xC8, 0xA0, 0x50) : borderColor);
                g2.fillRect(0,     0,     cs, cs);
                g2.fillRect(w - cs, 0,    cs, cs);
                g2.fillRect(0,     h - cs, cs, cs);
                g2.fillRect(w - cs, h - cs, cs, cs);
            }

            { // init hover
                setOpaque(false);
                if (enabled) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                        @Override public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                        @Override public void mouseClicked(MouseEvent e) { action.run(); }
                    });
                }
            }
        };
        btn.setPreferredSize(new Dimension(280, 52));
        btn.setMaximumSize(  new Dimension(280, 52));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(Theme.labelFont(16f));
        lbl.setForeground(textColor);
        btn.add(lbl);

        return btn;
    }


    /** JLabel con sombra paralela: dibuja el string en negro (+2,+2) y encima en el color dado. */
    private JLabel shadowLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(text)) / 2;
                int ty = fm.getAscent();
                g2.setColor(new Color(0, 0, 0, 210));
                g2.drawString(text, tx + 2, ty + 2);
                g2.setColor(color);
                g2.drawString(text, tx, ty);
            }
            @Override public Dimension getPreferredSize() {
                FontMetrics fm = getFontMetrics(font);
                return new Dimension(fm.stringWidth(text) + 6, fm.getHeight() + 4);
            }
        };
        lbl.setFont(font);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private JPanel buildDecoLine(int width) {
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.setColor(new Color(0xC0, 0x90, 0x40, 170));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(cx - width / 2, cy, cx - 14, cy);
                g2.drawLine(cx + 14, cy, cx + width / 2, cy);
                // Diamante central
                int[] dx = {cx, cx + 8, cx, cx - 8};
                int[] dy = {cy - 5, cy, cy + 5, cy};
                g2.fillPolygon(dx, dy, 4);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(width + 40, 12); }
            @Override public Dimension getMaximumSize()   { return new Dimension(width + 40, 12); }
        };
        line.setOpaque(false);
        line.setAlignmentX(Component.CENTER_ALIGNMENT);
        return line;
    }
}
