package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.util.SpriteLoader;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Pantalla de transición entre pisos.
 * Informa al jugador que avanzó de piso, muestra el estado de la party
 * tras la curación completa, y ofrece el botón para continuar.
 */
public class FloorTransitionScreen extends JPanel {

    public interface Listener { void onContinue(); }

    private static final Color BG        = new Color(0x06, 0x04, 0x0A);
    private static final Color GOLD      = new Color(0xC8, 0x92, 0x28);
    private static final Color GOLD_DIM  = new Color(0x60, 0x44, 0x10);
    private static final Color TEXT_MAIN = new Color(0xE8, 0xD8, 0xB0);
    private static final Color TEXT_DIM  = new Color(0x80, 0x70, 0x50);
    private static final Color GREEN     = new Color(0x48, 0xC8, 0x60);

    private BufferedImage background;

    public FloorTransitionScreen(GameState state, int fromLevel, Listener listener) {
        background = SpriteLoader.getBackground("rest"); // reutiliza fondo de descanso si existe

        setOpaque(true);
        setBackground(BG);
        setLayout(new GridBagLayout());

        JPanel card = buildCard(state, fromLevel, listener);
        add(card, new GridBagConstraints());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int w = getWidth(), h = getHeight();
        double s = Math.max((double) w / background.getWidth(),
                            (double) h / background.getHeight());
        int bw = (int)(background.getWidth() * s), bh = (int)(background.getHeight() * s);
        g2.drawImage(background, (w - bw) / 2, (h - bh) / 2, bw, bh, null);
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    // ── Card central ──────────────────────────────────────────────────────────

    private JPanel buildCard(GameState state, int fromLevel, Listener listener) {
        int nextLevel = fromLevel + 1;

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, new Color(0x16, 0x0C, 0x20),
                                               0, h, new Color(0x0A, 0x06, 0x10)));
                g2.fillRoundRect(0, 0, w, h, 16, 16);
                g2.setColor(GOLD_DIM);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        card.setPreferredSize(new Dimension(560, 0));

        // ── Título ────────────────────────────────────────────────────────
        JLabel floorDone = new JLabel("✦  PISO " + fromLevel + " COMPLETADO  ✦", SwingConstants.CENTER);
        floorDone.setFont(Theme.titleFont(28f));
        floorDone.setForeground(GOLD);
        floorDone.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel deco = new JLabel("◆  ◇  ◆", SwingConstants.CENTER);
        deco.setFont(Theme.labelFont(11f));
        deco.setForeground(GOLD_DIM);
        deco.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("La party avanza al PISO " + nextLevel, SwingConstants.CENTER);
        subtitle.setFont(Theme.bodyFont(14f));
        subtitle.setForeground(TEXT_DIM);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(floorDone);
        card.add(Box.createVerticalStrut(6));
        card.add(deco);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));
        card.add(goldLine());
        card.add(Box.createVerticalStrut(16));

        // ── Estado de la party post-curación ─────────────────────────────
        JLabel partyTitle = new JLabel("ESTADO DE LA PARTY", SwingConstants.CENTER);
        partyTitle.setFont(Theme.labelFont(12f).deriveFont(Font.BOLD));
        partyTitle.setForeground(GOLD_DIM);
        partyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(partyTitle);
        card.add(Box.createVerticalStrut(10));

        for (Character c : state.getParty()) {
            card.add(buildCharRow(c));
            card.add(Box.createVerticalStrut(6));
        }

        card.add(Box.createVerticalStrut(16));
        card.add(goldLine());
        card.add(Box.createVerticalStrut(20));

        // ── Botón continuar ───────────────────────────────────────────────
        JButton continueBtn = new JButton("Continuar al Piso " + nextLevel + "  ⟶");
        Theme.styleMenuButton(continueBtn);
        continueBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        continueBtn.setMaximumSize(new Dimension(340, 52));
        continueBtn.addActionListener(e -> listener.onContinue());
        card.add(continueBtn);

        return card;
    }

    private JPanel buildCharRow(Character c) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Nombre + clase
        JLabel name = new JLabel(c.getName() + "  ·  " + c.getClassName());
        name.setFont(Theme.labelFont(12f));
        name.setForeground(TEXT_MAIN);

        // Estado
        JLabel status = new JLabel(c.getHp() + " / " + c.getHpMax() + " HP");
        status.setFont(Theme.bodyFont(11f));
        status.setForeground(GREEN);
        status.setHorizontalAlignment(SwingConstants.RIGHT);

        // Tag de revivido (si el personaje estaba muerto antes de curar)
        // No podemos saber si estaba muerto (ya se curó), pero podemos inferirlo
        // mostrando el estado actual que siempre es full HP
        JLabel tag = new JLabel("  ✦ Curado al 100%");
        tag.setFont(Theme.bodyFont(10f));
        tag.setForeground(GREEN);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(status);
        right.add(tag);

        // Barra de HP (siempre llena)
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(0x06, 0x03, 0x01));
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.setColor(new Color(0x38, 0xB0, 0x48));
                g2.fillRoundRect(0, 0, w, h, h, h); // siempre llena
                g2.setColor(new Color(0x20, 0x60, 0x28, 100));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 6));

        JPanel inner = new JPanel(new BorderLayout(6, 2));
        inner.setOpaque(false);
        inner.add(name, BorderLayout.WEST);
        inner.add(right, BorderLayout.EAST);
        inner.add(bar, BorderLayout.SOUTH);

        row.add(inner, BorderLayout.CENTER);
        return row;
    }

    private JPanel goldLine() {
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(GOLD_DIM);
                g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            }
        };
        line.setOpaque(false);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }
}
