package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.core.model.items.Potion;
import com.dungeontales.util.SpriteLoader;
import com.dungeontales.util.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public class PartyScreen extends JDialog {

    // ── Colores ───────────────────────────────────────────────────────────────
    private static final Color BG        = new Color(0x14, 0x0C, 0x06);
    private static final Color LEFT_BG   = new Color(0x1C, 0x12, 0x08);
    private static final Color RIGHT_BG  = new Color(0x10, 0x0A, 0x06);
    private static final Color CELL_BG   = new Color(0x18, 0x10, 0x08);
    private static final Color CELL_EMPTY= new Color(0x0C, 0x08, 0x04);
    private static final Color GOLD      = new Color(0xC8, 0x92, 0x28);
    private static final Color GOLD_DIM  = new Color(0x60, 0x44, 0x10);
    private static final Color TEXT_MAIN = new Color(0xE8, 0xD8, 0xB0);
    private static final Color TEXT_DIM  = new Color(0x80, 0x70, 0x50);
    private static final Color HP_COL    = new Color(0xC0, 0x28, 0x28);
    private static final Color EXP_COL   = new Color(0x38, 0x78, 0xD0);
    private static final Color PA_COL    = new Color(0x28, 0x90, 0xD0);

    private static final int DIALOG_W = 980;
    private static final int DIALOG_H = 650;
    private static final int LEFT_W   = 310;
    private static final int CELL_SZ  = 72;
    private static final int GRID_COLS= 5;

    // ── Estado ────────────────────────────────────────────────────────────────
    private final GameState       state;
    private final List<Character> party;
    private int                   selectedIdx = 0;

    // Paneles que se reemplazan al cambiar personaje
    private JPanel bodyPanel;
    private JLabel tooltipLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PartyScreen(Frame parent, GameState state) {
        super(parent, true);
        this.state = state;
        this.party = state.getParty();
        setUndecorated(true);
        setSize(DIALOG_W, DIALOG_H);
        setLocationRelativeTo(parent);

        java.awt.image.BufferedImage bg = SpriteLoader.getBackground("party");

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (bg != null) {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    int w = getWidth(), h = getHeight();
                    double s = Math.max((double) w / bg.getWidth(), (double) h / bg.getHeight());
                    int bw = (int)(bg.getWidth() * s), bh = (int)(bg.getHeight() * s);
                    g2.drawImage(bg, (w - bw) / 2, (h - bh) / 2, bw, bh, null);
                    g2.setColor(new Color(0, 0, 0, 160));
                    g2.fillRect(0, 0, w, h);
                } else {
                    g2.setColor(BG);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
            }
        };
        root.setOpaque(true);
        root.setBorder(BorderFactory.createLineBorder(GOLD, 2));

        root.add(buildHeader(), BorderLayout.NORTH);

        bodyPanel = buildBody();
        root.add(bodyPanel, BorderLayout.CENTER);

        setContentPane(root);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x0C, 0x06, 0x02));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(GOLD_DIM);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                g2.dispose();
            }
        };
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 16));

        JLabel title = new JLabel("MENÚ DE EQUIPO");
        title.setFont(Theme.titleFont(20f));
        title.setForeground(GOLD);

        // ── Derecha: selector + cerrar ────────────────────────────────────
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JLabel switchLbl = new JLabel("CAMBIAR PERSONAJE  ▼");
        switchLbl.setFont(Theme.bodyFont(10f).deriveFont(Font.BOLD));
        switchLbl.setForeground(TEXT_DIM);
        right.add(switchLbl);

        JComboBox<String> combo = new JComboBox<>();
        combo.setBackground(new Color(0x1C, 0x12, 0x08));
        combo.setForeground(TEXT_MAIN);
        combo.setFont(Theme.labelFont(12f));
        combo.setFocusable(false);
        for (Character c : party)
            combo.addItem(c.getName() + "  (" + c.getClassName() + ")");
        combo.setSelectedIndex(selectedIdx);
        combo.addActionListener(e -> {
            selectedIdx = combo.getSelectedIndex();
            refreshBody();
        });
        right.add(combo);
        right.add(Box.createHorizontalStrut(8));

        JButton close = new JButton("✕");
        Theme.styleButton(close);
        close.setPreferredSize(new Dimension(36, 30));
        close.addActionListener(e -> dispose());
        right.add(close);

        h.add(title, BorderLayout.WEST);
        h.add(right,  BorderLayout.EAST);
        return h;
    }

    // ── Body ──────────────────────────────────────────────────────────────────

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(2, 0));
        body.setOpaque(false);
        body.add(buildLeftPanel(currentChar()), BorderLayout.WEST);
        body.add(buildRightPanel(),             BorderLayout.CENTER);
        return body;
    }

    private void refreshBody() {
        Container root = getContentPane();
        root.remove(bodyPanel);
        bodyPanel = buildBody();
        root.add(bodyPanel, BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }

    // ── Panel izquierdo: personaje ────────────────────────────────────────────

    private JPanel buildLeftPanel(Character c) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(LEFT_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(GOLD_DIM);
                g2.fillRect(getWidth() - 2, 0, 2, getHeight());
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(LEFT_W, 0));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // Nombre + clase + nivel
        addCentered(p, nameLabel(c.getName().toUpperCase() + "  (" + c.getClassName() + ")",
            Theme.labelFont(14f).deriveFont(Font.BOLD), TEXT_MAIN));
        addCentered(p, nameLabel("Nivel: " + c.getLevel(), Theme.bodyFont(10f), GOLD));
        p.add(vgap(8));
        p.add(hLine());
        p.add(vgap(8));

        // Retrato
        JPanel portrait = buildPortrait(c);
        portrait.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(portrait);
        p.add(vgap(10));
        p.add(hLine());
        p.add(vgap(8));

        // Barras HP / EXP / PA
        p.add(buildBar("HP",  c.getHp(),  c.getHpMax(),  HP_COL));
        p.add(vgap(3));
        p.add(buildBar("EXP", c.getExp(), c.getExpToNext(), EXP_COL));
        p.add(vgap(3));
        p.add(buildBar("PA",  c.getPa(),  c.getPaMax(),  PA_COL));
        p.add(vgap(8));
        p.add(hLine());
        p.add(vgap(6));

        // Stats
        JPanel stats = new JPanel(new GridLayout(2, 2, 6, 4));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.add(statRow("⚔ ATK", String.valueOf(c.getEffectiveAtk())));
        stats.add(statRow("🛡 DEF", String.valueOf(c.getEffectiveDef())));
        stats.add(statRow("💨 SPD", String.valueOf(c.getSpd())));
        stats.add(statRow("✦  PA",  c.getPaMax() + "  (+" + c.getPaRegen() + "/turno)"));
        p.add(stats);
        p.add(vgap(8));
        p.add(hLine());
        p.add(vgap(6));

        // Habilidades
        sectionTitle(p, "HABILIDADES");
        for (Ability ab : c.getAbilities()) {
            JLabel l = new JLabel("• " + ab.getName() + "  (" + ab.getPaCost() + " PA)");
            l.setFont(Theme.bodyFont(10f));
            l.setForeground(TEXT_DIM);
            l.setToolTipText(ab.getDescription());
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(l);
            p.add(vgap(1));
        }
        p.add(vgap(8));
        p.add(hLine());
        p.add(vgap(6));

        // Slots de equipo
        sectionTitle(p, "EQUIPO");
        JPanel slots = new JPanel(new GridLayout(1, 2, 8, 0));
        slots.setOpaque(false);
        slots.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        slots.setAlignmentX(Component.LEFT_ALIGNMENT);
        slots.add(equipSlot("⚔ ARMA",
            c.getEquippedWeapon() != null ? c.getEquippedWeapon().getName() : null));
        slots.add(equipSlot("🛡 ARMADURA",
            c.getEquippedArmor()  != null ? c.getEquippedArmor().getName()  : null));
        p.add(slots);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Retrato (mitad superior zoomeada) ─────────────────────────────────────

    private JPanel buildPortrait(Character c) {
        BufferedImage stand = loadStand(c.getSpriteName());
        int pW = LEFT_W - 28;
        int pH = 230;

        JPanel portrait = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

                // Fondo del retrato
                g2.setColor(new Color(0x08, 0x04, 0x02));
                g2.fillRect(0, 0, getWidth(), getHeight());

                if (stand != null) {
                    int sw = stand.getWidth(), sh = stand.getHeight();
                    // Mostrar solo la mitad superior de la imagen, escalada para llenar el ancho
                    int srcH = (int)(sh * 0.55);
                    g2.drawImage(stand,
                        0, 0, getWidth(), getHeight(),
                        0, 0, sw, srcH,
                        null);
                }

                // Degradado inferior para fundir con el panel
                g2.setPaint(new GradientPaint(0, getHeight() * 0.65f, new Color(0, 0, 0, 0),
                    0, getHeight(), new Color(0x1C, 0x12, 0x08, 240)));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Borde dorado
                g2.setColor(GOLD_DIM);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        portrait.setOpaque(false);
        portrait.setPreferredSize(new Dimension(pW, pH));
        portrait.setMaximumSize(new Dimension(pW, pH));
        return portrait;
    }

    // ── Panel derecho: inventario ─────────────────────────────────────────────

    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(RIGHT_BG);
        p.setOpaque(true);
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 16));

        JLabel title = new JLabel("INVENTARIO COMPARTIDO");
        title.setFont(Theme.labelFont(13f).deriveFont(Font.BOLD));
        title.setForeground(GOLD);

        // Tooltip del ítem seleccionado
        tooltipLabel = new JLabel(" ");
        tooltipLabel.setFont(Theme.bodyFont(11f));
        tooltipLabel.setForeground(new Color(0xD0, 0xC0, 0x90));

        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);
        topRow.add(title,        BorderLayout.WEST);
        topRow.add(tooltipLabel, BorderLayout.CENTER);

        // Grilla de ítems
        JPanel grid = buildItemGrid();
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(GOLD_DIM, 1));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Tabs filtro (preparado para el futuro)
        JPanel tabs = buildFilterTabs();

        p.add(topRow, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        p.add(tabs,   BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildItemGrid() {
        // Agrupar pociones por tipo
        Map<Potion.Effect, Integer> counts = new LinkedHashMap<>();
        Map<Potion.Effect, Potion>  reps   = new LinkedHashMap<>();
        for (Potion po : state.getInventory().getPotions()) {
            counts.merge(po.getEffect(), 1, Integer::sum);
            reps.putIfAbsent(po.getEffect(), po);
        }

        // Convertir a lista ordenada de (potion, count)
        List<Potion> itemList = new ArrayList<>(reps.values());
        List<Integer> countList = new ArrayList<>();
        for (Potion po : itemList) countList.add(counts.get(po.getEffect()));

        int total = itemList.size();
        int rows  = Math.max(4, (int) Math.ceil((double) total / GRID_COLS) + 2);
        int cells = rows * GRID_COLS;

        JPanel grid = new JPanel(new GridLayout(rows, GRID_COLS, 3, 3));
        grid.setBackground(new Color(0x08, 0x05, 0x02));
        grid.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        for (int i = 0; i < cells; i++) {
            if (i < total)
                grid.add(buildItemCell(itemList.get(i), countList.get(i)));
            else
                grid.add(buildEmptyCell());
        }
        return grid;
    }

    private JPanel buildItemCell(Potion potion, int count) {
        String iconKey = switch (potion.getEffect()) {
            case HEAL_SMALL, HEAL_LARGE -> "potion-heal";
            case PA_RESTORE             -> "potion-pa";
            case REVIVE                 -> "potion-revive";
            default                     -> "potion-heal";
        };
        Color accent = switch (potion.getEffect()) {
            case HEAL_SMALL, HEAL_LARGE -> new Color(0xB0, 0x28, 0x28);
            case PA_RESTORE             -> new Color(0x28, 0x80, 0xC0);
            case REVIVE                 -> new Color(0x80, 0x28, 0xA0);
            default                     -> new Color(0x60, 0x60, 0x60);
        };
        BufferedImage icon = SpriteLoader.getShopIcon(iconKey, 40);

        JPanel cell = new JPanel() {
            boolean hover = false;
            {
                setOpaque(false);
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        hover = true; repaint();
                        if (tooltipLabel != null)
                            tooltipLabel.setText(potion.getName() + "  ×" + count
                                + " / " + state.getInventory().getMaxPerPotion());
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        hover = false; repaint();
                        if (tooltipLabel != null) tooltipLabel.setText(" ");
                    }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                g2.setColor(hover ? new Color(0x22, 0x16, 0x0A) : CELL_BG);
                g2.fillRect(0, 0, w, h);

                g2.setColor(hover ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200)
                    : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 100));
                g2.setStroke(new BasicStroke(hover ? 1.8f : 1.2f));
                g2.drawRect(0, 0, w - 1, h - 1);

                // Ícono centrado
                if (icon != null) {
                    int ix = (w - 40) / 2;
                    int iy = (h - 40) / 2 - 6;
                    g2.drawImage(icon, ix, iy, 40, 40, null);
                }

                // Badge de cantidad (esquina inferior derecha)
                String badge = "×" + count;
                g2.setFont(Theme.bodyFont(9f).deriveFont(Font.BOLD));
                FontMetrics fm = g2.getFontMetrics();
                int bw = fm.stringWidth(badge) + 4;
                g2.setColor(new Color(0x08, 0x04, 0x02, 200));
                g2.fillRect(w - bw - 2, h - fm.getHeight() - 2, bw + 2, fm.getHeight() + 2);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220));
                g2.drawString(badge, w - bw, h - fm.getDescent() - 2);

                g2.dispose();
            }
        };
        cell.setPreferredSize(new Dimension(CELL_SZ, CELL_SZ));
        return cell;
    }

    private JPanel buildEmptyCell() {
        JPanel cell = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(CELL_EMPTY);
                g.fillRect(0, 0, getWidth(), getHeight());
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0x22, 0x16, 0x0A));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        cell.setOpaque(false);
        cell.setPreferredSize(new Dimension(CELL_SZ, CELL_SZ));
        return cell;
    }

    private JPanel buildFilterTabs() {
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 4));
        tabs.setOpaque(false);
        for (String f : new String[]{"TODOS", "ARMAS", "ARMADURAS", "CONSUMIBLES"}) {
            JButton btn = new JButton("[" + f + "]");
            btn.setFont(Theme.bodyFont(10f));
            btn.setBackground(new Color(0x14, 0x0C, 0x06));
            btn.setForeground(TEXT_DIM);
            btn.setBorder(BorderFactory.createLineBorder(GOLD_DIM, 1));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tabs.add(btn);
        }
        return tabs;
    }

    // ── Helpers de layout ────────────────────────────────────────────────────

    private JPanel buildBar(String label, int current, int max, Color fill) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.bodyFont(9f)); lbl.setForeground(TEXT_DIM);
        lbl.setPreferredSize(new Dimension(24, 12));

        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int filled = max > 0 ? (int)((double) current / max * w) : 0;
                g2.setColor(new Color(0x06, 0x03, 0x01));
                g2.fillRoundRect(0, 0, w, h, h, h);
                if (filled > 0) { g2.setColor(fill); g2.fillRoundRect(0, 0, filled, h, h, h); }
                g2.setColor(new Color(0x40, 0x28, 0x0C, 100));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.dispose();
            }
        };
        bar.setOpaque(false);

        JLabel val = new JLabel(current + "/" + max);
        val.setFont(Theme.bodyFont(9f)); val.setForeground(TEXT_DIM);
        val.setPreferredSize(new Dimension(54, 12));
        val.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(lbl, BorderLayout.WEST);
        row.add(bar, BorderLayout.CENTER);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private JPanel statRow(String key, String val) {
        JPanel r = new JPanel(new BorderLayout(4, 0));
        r.setOpaque(false);
        JLabel k = new JLabel(key); k.setFont(Theme.bodyFont(10f)); k.setForeground(TEXT_DIM);
        JLabel v = new JLabel(val); v.setFont(Theme.bodyFont(10f).deriveFont(Font.BOLD));
        v.setForeground(TEXT_MAIN);
        r.add(k, BorderLayout.WEST); r.add(v, BorderLayout.EAST);
        return r;
    }

    private JPanel equipSlot(String slotName, String itemName) {
        boolean filled = itemName != null;
        JPanel slot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(filled ? CELL_BG : CELL_EMPTY);
                g2.fillRoundRect(0, 0, w, h, 6, 6);
                g2.setColor(filled ? GOLD_DIM : new Color(0x32, 0x22, 0x0C));
                g2.setStroke(filled
                    ? new BasicStroke(1.5f)
                    : new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        1f, new float[]{5, 3}, 0));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        slot.setOpaque(false);
        slot.setLayout(new BoxLayout(slot, BoxLayout.Y_AXIS));
        slot.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));

        JLabel slotLbl = new JLabel(slotName, SwingConstants.CENTER);
        slotLbl.setFont(Theme.bodyFont(8f).deriveFont(Font.BOLD));
        slotLbl.setForeground(GOLD_DIM);
        slotLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel itemLbl = new JLabel(filled ? itemName : "—", SwingConstants.CENTER);
        itemLbl.setFont(Theme.bodyFont(10f));
        itemLbl.setForeground(filled ? TEXT_MAIN : TEXT_DIM);
        itemLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        slot.add(slotLbl);
        slot.add(Box.createVerticalStrut(3));
        slot.add(itemLbl);
        return slot;
    }

    private void sectionTitle(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bodyFont(9f).deriveFont(Font.BOLD));
        l.setForeground(GOLD_DIM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(vgap(2));
    }

    private void addCentered(JPanel p, JLabel l) {
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(l);
    }

    private JLabel nameLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font); l.setForeground(color);
        return l;
    }

    private Component vgap(int h)      { return Box.createVerticalStrut(h); }

    private JPanel hLine() {
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

    // ── Carga del sprite Stand ────────────────────────────────────────────────

    private BufferedImage loadStand(String spriteName) {
        String base = java.lang.Character.toUpperCase(spriteName.charAt(0))
            + spriteName.substring(1).toLowerCase();
        String path = "/sprites/characters/" + base + "/" + base + "-Stand.png";
        try {
            InputStream is = getClass().getResourceAsStream(path);
            return is != null ? ImageIO.read(is) : null;
        } catch (Exception ignored) { return null; }
    }

    private Character currentChar() { return party.get(selectedIdx); }
}
