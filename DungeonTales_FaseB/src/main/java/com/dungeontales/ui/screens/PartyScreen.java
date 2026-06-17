package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.Inventory;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.core.model.items.Potion;
import com.dungeontales.util.SpriteLoader;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Modal overlay that shows the full party status:
 * per-character stats + abilities + equipment slots,
 * plus the shared inventory.
 */
public class PartyScreen extends JDialog {

    private static final Color BG        = new Color(0x0C, 0x08, 0x04);
    private static final Color CARD_TOP  = new Color(0x22, 0x14, 0x08);
    private static final Color CARD_BOT  = new Color(0x10, 0x08, 0x04);
    private static final Color GOLD      = new Color(0xC8, 0x92, 0x28);
    private static final Color GOLD_DIM  = new Color(0x70, 0x50, 0x14);
    private static final Color TEXT_MAIN = new Color(0xE8, 0xD8, 0xB0);
    private static final Color TEXT_DIM  = new Color(0x88, 0x78, 0x58);
    private static final Color HP_COL    = new Color(0xC8, 0x28, 0x28);
    private static final Color EXP_COL   = new Color(0x38, 0x78, 0xD0);

    public PartyScreen(Frame parent, GameState state) {
        super(parent, true);
        setUndecorated(true);
        setSize(parent.getSize());
        setLocationRelativeTo(parent);

        // Semi-transparent dimming layer — click outside container to close
        JPanel dim = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 175));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        dim.setOpaque(false);
        dim.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dispose(); }
        });

        // Main container
        JPanel box = new JPanel(new BorderLayout(0, 12));
        box.setBackground(BG);
        box.setOpaque(true);
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD, 2),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        box.setPreferredSize(new Dimension(940, 670));
        box.addMouseListener(new MouseAdapter() {}); // absorb clicks inside

        box.add(buildHeader(), BorderLayout.NORTH);
        box.add(buildCharRow(state), BorderLayout.CENTER);
        box.add(buildInventoryPanel(state.getInventory()), BorderLayout.SOUTH);

        dim.add(box);
        setContentPane(dim);
    }

    // ── Header ────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        JLabel title = new JLabel("ESTADO DE LA PARTY");
        title.setFont(Theme.titleFont(22f));
        title.setForeground(GOLD);

        JButton close = new JButton("✕  Cerrar");
        Theme.styleButton(close);
        close.addActionListener(e -> dispose());

        p.add(title, BorderLayout.CENTER);
        p.add(close, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.add(p, BorderLayout.NORTH);
        wrapper.add(goldLine(), BorderLayout.SOUTH);
        return wrapper;
    }

    // ── Character row ─────────────────────────────────────────────────────

    private JPanel buildCharRow(GameState state) {
        JPanel row = new JPanel(new GridLayout(1, state.getParty().size(), 10, 0));
        row.setOpaque(false);
        for (Character c : state.getParty()) row.add(buildCharCard(c));
        return row;
    }

    private JPanel buildCharCard(Character c) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, CARD_TOP, 0, h, CARD_BOT));
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                g2.setColor(GOLD_DIM);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // ── Sprite ───────────────────────────────────────────────────────
        BufferedImage sprite = SpriteLoader.getCharacterSprite(c.getSpriteName(), 140, 160);
        JPanel spritePanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (sprite != null) {
                    int sx = (getWidth() - sprite.getWidth()) / 2;
                    g.drawImage(sprite, sx, 0, this);
                }
                if (!c.isAlive()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0, 0, 0, 150));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setFont(Theme.labelFont(16f));
                    g2.setColor(new Color(0xD0, 0x30, 0x30));
                    FontMetrics fm = g2.getFontMetrics();
                    String txt = "☠  CAÍDO";
                    g2.drawString(txt, (getWidth() - fm.stringWidth(txt)) / 2, getHeight() / 2);
                    g2.dispose();
                }
            }
        };
        spritePanel.setOpaque(false);
        spritePanel.setPreferredSize(new Dimension(140, 160));
        spritePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        spritePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(spritePanel);
        card.add(Box.createVerticalStrut(6));

        // ── Name / Class ─────────────────────────────────────────────────
        addCenteredLabel(card, c.getName(), Theme.labelFont(14f).deriveFont(Font.BOLD), TEXT_MAIN);
        addCenteredLabel(card, c.getClassName() + "  ·  Nv. " + c.getLevel(), Theme.bodyFont(10f), GOLD);
        card.add(Box.createVerticalStrut(6));
        card.add(goldLine());
        card.add(Box.createVerticalStrut(5));

        // ── HP / EXP bars ────────────────────────────────────────────────
        card.add(buildBar("HP",  c.getHp(),  c.getHpMax(),  HP_COL));
        card.add(Box.createVerticalStrut(3));
        card.add(buildBar("EXP", c.getExp(), c.getExpToNext(), EXP_COL));
        card.add(Box.createVerticalStrut(6));
        card.add(goldLine());
        card.add(Box.createVerticalStrut(5));

        // ── Stats 2×2 ────────────────────────────────────────────────────
        JPanel stats = new JPanel(new GridLayout(2, 2, 4, 3));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        stats.setAlignmentX(Component.CENTER_ALIGNMENT);
        stats.add(statLabel("⚔ ATK", String.valueOf(c.getEffectiveAtk())));
        stats.add(statLabel("🛡 DEF", String.valueOf(c.getEffectiveDef())));
        stats.add(statLabel("💨 SPD", String.valueOf(c.getSpd())));
        stats.add(statLabel("✦  PA",  c.getPa() + "/" + c.getPaMax()));
        card.add(stats);
        card.add(Box.createVerticalStrut(6));
        card.add(goldLine());
        card.add(Box.createVerticalStrut(5));

        // ── Abilities ────────────────────────────────────────────────────
        addSectionTitle(card, "HABILIDADES");
        for (Ability ab : c.getAbilities()) {
            JLabel l = new JLabel("• " + ab.getName() + "  (" + ab.getPaCost() + " PA)");
            l.setFont(Theme.bodyFont(10f));
            l.setForeground(TEXT_DIM);
            l.setToolTipText(ab.getDescription());
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(l);
        }
        card.add(Box.createVerticalStrut(6));
        card.add(goldLine());
        card.add(Box.createVerticalStrut(5));

        // ── Equipment ────────────────────────────────────────────────────
        addSectionTitle(card, "EQUIPO");

        String wName = c.getEquippedWeapon() != null ? c.getEquippedWeapon().getName() : "Sin arma";
        String aName = c.getEquippedArmor()  != null ? c.getEquippedArmor().getName()  : "Sin armadura";
        Color  wCol  = c.getEquippedWeapon() != null ? TEXT_MAIN : TEXT_DIM;
        Color  aCol  = c.getEquippedArmor()  != null ? TEXT_MAIN : TEXT_DIM;

        JLabel wLabel = new JLabel("⚔  " + wName);
        wLabel.setFont(Theme.bodyFont(10f)); wLabel.setForeground(wCol);
        wLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel aLabel = new JLabel("🛡  " + aName);
        aLabel.setFont(Theme.bodyFont(10f)); aLabel.setForeground(aCol);
        aLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(wLabel);
        card.add(aLabel);
        card.add(Box.createVerticalGlue());
        return card;
    }

    // ── Inventory panel ───────────────────────────────────────────────────

    private JPanel buildInventoryPanel(Inventory inventory) {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, CARD_TOP, 0, getHeight(), CARD_BOT));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(GOLD_DIM);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel title = new JLabel("INVENTARIO COMPARTIDO");
        title.setFont(Theme.labelFont(12f).deriveFont(Font.BOLD));
        title.setForeground(GOLD);
        panel.add(title, BorderLayout.NORTH);

        // Group potions by effect
        Map<Potion.Effect, Integer> counts = new LinkedHashMap<>();
        Map<Potion.Effect, Potion>  reps   = new LinkedHashMap<>();
        for (Potion p : inventory.getPotions()) {
            counts.merge(p.getEffect(), 1, Integer::sum);
            reps.putIfAbsent(p.getEffect(), p);
        }

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);

        if (counts.isEmpty()) {
            JLabel empty = new JLabel("El inventario está vacío.");
            empty.setFont(Theme.bodyFont(11f));
            empty.setForeground(TEXT_DIM);
            chips.add(empty);
        } else {
            for (Potion.Effect effect : counts.keySet()) {
                chips.add(buildPotionChip(reps.get(effect), counts.get(effect), inventory.getMaxPerPotion()));
            }
        }
        panel.add(chips, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPotionChip(Potion potion, int count, int max) {
        Color accent = switch (potion.getEffect()) {
            case HEAL_SMALL, HEAL_LARGE -> new Color(0xC8, 0x30, 0x30);
            case PA_RESTORE             -> new Color(0x30, 0x90, 0xD0);
            case REVIVE                 -> new Color(0x90, 0x30, 0xB0);
            default                     -> new Color(0x60, 0x60, 0x60);
        };
        BufferedImage icon = SpriteLoader.getShopIcon(iconKeyFor(potion.getEffect()), 36);

        JPanel chip = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setLayout(new BoxLayout(chip, BoxLayout.Y_AXIS));
        chip.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        if (icon != null) {
            JPanel iconPanel = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g); g.drawImage(icon, 0, 0, 36, 36, this);
                }
            };
            iconPanel.setOpaque(false);
            iconPanel.setPreferredSize(new Dimension(36, 36));
            iconPanel.setMaximumSize(new Dimension(36, 36));
            iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            chip.add(iconPanel);
            chip.add(Box.createVerticalStrut(4));
        }

        JLabel nameLbl = new JLabel(potion.getName(), SwingConstants.CENTER);
        nameLbl.setFont(Theme.bodyFont(10f));
        nameLbl.setForeground(TEXT_MAIN);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel cntLbl = new JLabel("x" + count + " / " + max, SwingConstants.CENTER);
        cntLbl.setFont(Theme.bodyFont(9f));
        cntLbl.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210));
        cntLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        chip.add(nameLbl);
        chip.add(cntLbl);
        return chip;
    }

    private String iconKeyFor(Potion.Effect effect) {
        return switch (effect) {
            case HEAL_SMALL, HEAL_LARGE -> "potion-heal";
            case PA_RESTORE             -> "potion-pa";
            case REVIVE                 -> "potion-revive";
            default                     -> "potion-heal";
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JPanel buildBar(String label, int current, int max, Color fill) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.bodyFont(9f));
        lbl.setForeground(TEXT_DIM);
        lbl.setPreferredSize(new Dimension(26, 12));

        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int filled = max > 0 ? (int)((double) current / max * w) : 0;
                g2.setColor(new Color(0x08, 0x04, 0x02));
                g2.fillRoundRect(0, 0, w, h, h, h);
                if (filled > 0) { g2.setColor(fill); g2.fillRoundRect(0, 0, filled, h, h, h); }
                g2.setColor(new Color(0x50, 0x38, 0x10, 100));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.dispose();
            }
        };
        bar.setOpaque(false);

        JLabel val = new JLabel(current + "/" + max);
        val.setFont(Theme.bodyFont(9f));
        val.setForeground(TEXT_DIM);
        val.setPreferredSize(new Dimension(52, 12));
        val.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(lbl, BorderLayout.WEST);
        row.add(bar, BorderLayout.CENTER);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private JLabel statLabel(String key, String value) {
        JLabel l = new JLabel(key + ":  " + value);
        l.setFont(Theme.bodyFont(10f));
        l.setForeground(TEXT_MAIN);
        return l;
    }

    private void addCenteredLabel(JPanel p, String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font); l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(l);
    }

    private void addSectionTitle(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bodyFont(9f).deriveFont(Font.BOLD));
        l.setForeground(GOLD_DIM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(2));
    }

    private JPanel goldLine() {
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(GOLD_DIM); g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            }
        };
        line.setOpaque(false);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }
}
