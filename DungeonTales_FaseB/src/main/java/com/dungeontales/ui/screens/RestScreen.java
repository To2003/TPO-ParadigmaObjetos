package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.util.SpriteLoader;
import com.dungeontales.util.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class RestScreen extends JPanel {

    public interface Listener { void onDone(); }

    private BufferedImage background;
    private BufferedImage iconHeal;
    private BufferedImage iconMeditate;

    public RestScreen(GameState state, Listener listener) {
        loadBackground();
        setLayout(new GridBagLayout());
        setOpaque(true);
        setBackground(new Color(0x06, 0x08, 0x04));

        // Panel central con contenido
        JPanel content = buildContent(state, listener);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(content, gbc);
    }

    // ── Fondo ──────────────────────────────────────────────────────────────

    private void loadBackground() {
        try (InputStream is = getClass().getResourceAsStream("/sprites/backgrounds/rest-bg.png")) {
            if (is != null) background = ImageIO.read(is);
        } catch (Exception ignored) {}
        // Íconos con fondo negro eliminado (mismo tratamiento que los íconos del mapa)
        iconHeal     = SpriteLoader.getRestIcon("heal",     52);
        iconMeditate = SpriteLoader.getRestIcon("meditate", 52);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth(), h = getHeight();

        if (background != null) {
            // Escalar manteniendo relación de aspecto y cubriendo todo el panel (cover)
            double scaleX = (double) w / background.getWidth();
            double scaleY = (double) h / background.getHeight();
            double scale  = Math.max(scaleX, scaleY);
            int bw = (int)(background.getWidth()  * scale);
            int bh = (int)(background.getHeight() * scale);
            int bx = (w - bw) / 2;
            int by = (h - bh) / 2;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(background, bx, by, bw, bh, null);
        }

        // Oscurecer ligeramente para que el texto sea legible
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, 0, w, h);
    }

    // ── Contenido central ──────────────────────────────────────────────────

    private JPanel buildContent(GameState state, Listener listener) {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                // Sin fondo propio — el background del padre se ve a través
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(500, 480));

        // ── Ícono hoguera ──
        JLabel fireIcon = new JLabel("🔥") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Dibujar ícono de llama con Graphics2D
                int cx = getWidth() / 2, cy = getHeight() / 2;
                // Llama exterior (naranja)
                g2.setColor(new Color(0xE8, 0x70, 0x10, 200));
                int[] fx1 = {cx, cx - 18, cx - 8, cx + 8, cx + 18};
                int[] fy1 = {cy - 26, cy + 8, cy - 4, cy - 4, cy + 8};
                g2.fillPolygon(fx1, fy1, 5);
                // Llama interior (amarilla)
                g2.setColor(new Color(0xFF, 0xD0, 0x40, 220));
                int[] fx2 = {cx, cx - 10, cx - 3, cx + 3, cx + 10};
                int[] fy2 = {cy - 18, cy + 6, cy - 2, cy - 2, cy + 6};
                g2.fillPolygon(fx2, fy2, 5);
                // Núcleo blanco
                g2.setColor(new Color(0xFF, 0xFF, 0xE0, 180));
                g2.fillOval(cx - 4, cy - 4, 8, 8);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(60, 60); }
        };
        fireIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Línea decorativa superior
        JPanel topDeco = buildDecoLine();

        // Título
        JLabel title = new JLabel("CAMPAMENTO DE DESCANSO");
        title.setFont(Theme.titleFont(28f));
        title.setForeground(new Color(0xD4, 0xAA, 0x50));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Línea decorativa inferior del título
        JPanel midDeco = buildDecoLine();

        // Subtítulo
        JLabel sub1 = centeredLabel("Un lugar tranquilo para recuperar fuerzas.", Theme.bodyFont(14f), new Color(0xB0, 0x98, 0x78));
        JLabel sub2 = centeredLabel("El descanso te hará más fuerte para lo que viene.", Theme.bodyFont(14f), new Color(0xB0, 0x98, 0x78));

        // Separador de diamantes
        JLabel deco = centeredLabel("◆  ◇  ◆", Theme.labelFont(11f), new Color(0x80, 0x68, 0x40));

        // Botones
        JPanel healBtn   = buildRestButton(
            iconHeal, "Curar toda la party", "(+30% HP)",
            new Color(0x14, 0x3A, 0x20), new Color(0x28, 0x70, 0x3A),
            e -> {
                state.getAliveParty().forEach(c -> c.healHp((int)(c.getHpMax() * 0.30)));
                listener.onDone();
            }
        );

        JPanel meditateBtn = buildRestButton(
            iconMeditate, "Meditar (un personaje)", "(+50% HP)",
            new Color(0x12, 0x22, 0x3A), new Color(0x24, 0x44, 0x68),
            e -> {
                var alive = state.getAliveParty();
                if (alive.size() == 1) {
                    alive.get(0).healHp((int)(alive.get(0).getHpMax() * 0.50));
                } else {
                    String[] names = alive.stream().map(c ->
                        c.getName() + " (" + c.getHp() + "/" + c.getHpMax() + " HP)")
                        .toArray(String[]::new);
                    String choice = (String) JOptionPane.showInputDialog(
                        this, "Elegir personaje:", "Meditar",
                        JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
                    if (choice == null) return;
                    for (int i = 0; i < names.length; i++) {
                        if (names[i].equals(choice)) {
                            alive.get(i).healHp((int)(alive.get(i).getHpMax() * 0.50));
                            break;
                        }
                    }
                }
                listener.onDone();
            }
        );

        // ── Pergamino de texto ─────────────────────────────────────────────
        JPanel parchment = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Fondo semitransparente cálido (pergamino oscuro)
                g2.setColor(new Color(0x10, 0x0A, 0x04, 175));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
                // Borde dorado fino
                g2.setColor(new Color(0xC8, 0x96, 0x40, 140));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
                // Línea interior sutil
                g2.setColor(new Color(0xC8, 0x96, 0x40, 50));
                g2.drawRoundRect(3, 3, w - 7, h - 7, 12, 12);
                g2.setStroke(new BasicStroke(1f));
            }
        };
        parchment.setOpaque(false);
        parchment.setLayout(new BoxLayout(parchment, BoxLayout.Y_AXIS));
        parchment.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        parchment.setMaximumSize(new Dimension(480, 220));
        parchment.setAlignmentX(Component.CENTER_ALIGNMENT);

        parchment.add(fireIcon);
        parchment.add(Box.createVerticalStrut(6));
        parchment.add(topDeco);
        parchment.add(Box.createVerticalStrut(10));
        parchment.add(title);
        parchment.add(Box.createVerticalStrut(4));
        parchment.add(midDeco);
        parchment.add(Box.createVerticalStrut(14));
        parchment.add(sub1);
        parchment.add(Box.createVerticalStrut(4));
        parchment.add(sub2);
        parchment.add(Box.createVerticalStrut(10));
        parchment.add(deco);

        panel.add(Box.createVerticalGlue());
        panel.add(parchment);
        panel.add(Box.createVerticalStrut(20));
        panel.add(healBtn);
        panel.add(Box.createVerticalStrut(12));
        panel.add(meditateBtn);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private JPanel buildRestButton(BufferedImage iconImg, String label, String sublabel,
                                    Color bgColor, Color borderColor,
                                    java.awt.event.ActionListener action) {
        JPanel btn = new JPanel(new BorderLayout(0, 0)) {
            boolean hover = false;

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Fondo semitransparente
                Color bg = hover
                    ? new Color(bgColor.getRed() + 18, bgColor.getGreen() + 18,
                                bgColor.getBlue() + 18, 210)
                    : new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 190);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);

                // Borde
                g2.setColor(hover
                    ? new Color(Math.min(255, borderColor.getRed() + 30),
                                Math.min(255, borderColor.getGreen() + 30),
                                Math.min(255, borderColor.getBlue() + 30))
                    : borderColor);
                g2.setStroke(new BasicStroke(hover ? 2f : 1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g2.setStroke(new BasicStroke(1f));
            }

            { // inicialización del hover
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true;  repaint(); }
                    @Override public void mouseExited (java.awt.event.MouseEvent e) { hover = false; repaint(); }
                    @Override public void mouseClicked(java.awt.event.MouseEvent e) { action.actionPerformed(null); }
                });
            }
        };
        btn.setOpaque(false);
        btn.setMaximumSize(new Dimension(440, 80));
        btn.setPreferredSize(new Dimension(440, 80));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Ícono circular con PNG
        JPanel iconPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int cx = getWidth() / 2, cy = getHeight() / 2, r = 26;

                // Fondo circular
                g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                                      borderColor.getBlue(), 170));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                g2.setColor(new Color(255, 255, 255, 190));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                g2.setStroke(new BasicStroke(1f));

                // Ícono PNG centrado dentro del círculo
                if (iconImg != null) {
                    int iconSize = r * 2 - 10;
                    // Clip circular para que el ícono no salga del borde
                    java.awt.Shape clip = new java.awt.geom.Ellipse2D.Float(
                        cx - r + 1, cy - r + 1, (r - 1) * 2, (r - 1) * 2);
                    g2.setClip(clip);
                    g2.drawImage(iconImg, cx - iconSize / 2, cy - iconSize / 2,
                                 iconSize, iconSize, null);
                    g2.setClip(null);
                }
            }
            @Override public Dimension getPreferredSize() { return new Dimension(70, 80); }
        };
        iconPanel.setOpaque(false);

        // Textos
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory.createEmptyBorder(18, 10, 18, 16));

        JLabel lblMain = new JLabel(label);
        lblMain.setFont(Theme.labelFont(16f));
        lblMain.setForeground(new Color(0xE8, 0xD8, 0xA0));

        JLabel lblSub = new JLabel(sublabel);
        lblSub.setFont(Theme.bodyFont(13f));
        lblSub.setForeground(new Color(0x90, 0xB8, 0x80));

        textPanel.add(lblMain);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(lblSub);

        btn.add(iconPanel, BorderLayout.WEST);
        btn.add(textPanel, BorderLayout.CENTER);
        return btn;
    }

    private JPanel buildDecoLine() {
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.setColor(new Color(0xC8, 0x96, 0x40, 160));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(cx - 120, cy, cx - 18, cy);
                g2.drawLine(cx + 18,  cy, cx + 120, cy);
                // diamante central
                int[] dx = {cx, cx + 8, cx, cx - 8};
                int[] dy = {cy - 6, cy, cy + 6, cy};
                g2.fillPolygon(dx, dy, 4);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(300, 14); }
            @Override public Dimension getMaximumSize()   { return new Dimension(500, 14); }
        };
        line.setOpaque(false);
        line.setAlignmentX(Component.CENTER_ALIGNMENT);
        return line;
    }

    private JLabel centeredLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(font);
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }
}
