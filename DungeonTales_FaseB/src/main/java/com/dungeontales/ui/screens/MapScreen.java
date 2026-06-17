package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.core.map.MapNode;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.util.SpriteLoader;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pantalla del mapa de nodos estilo Slay the Spire. */
public class MapScreen extends JPanel {

    public interface Listener {
        void onNodeSelected(MapNode node);
        void onSaveRequested();
        void onPartyRequested();
    }

    private final GameState state;
    private final Listener  listener;

    private final Map<MapNode, Point> nodeCenters = new HashMap<>();
    private MapNode hoveredNode = null;

    // Íconos PNG por tipo de nodo
    private final Map<MapNode.Type, BufferedImage> nodeIcons   = new EnumMap<>(MapNode.Type.class);
    private final Map<MapNode.Type, BufferedImage> legendIcons = new EnumMap<>(MapNode.Type.class);
    private BufferedImage startIcon;
    private BufferedImage startIconLegend;
    private BufferedImage mapBackground;

    private static final int NODE_R      = 34;   // radio nodo disponible / actual
    private static final int NODE_R_SM   = 24;   // radio nodo visitado / bloqueado
    private static final int ICON_SIZE   = 46;   // tamaño del ícono dentro del círculo
    private static final int LEGEND_ICON = 30;   // tamaño del ícono en la leyenda
    private static final int ROW_H       = 150;  // espacio vertical entre filas
    private static final int COL_GAP     = 155;  // espacio horizontal entre columnas
    private static final int PAD_TOP     = 70;
    private static final int PAD_BOT     = 80;

    public MapScreen(GameState state, Listener listener) {
        this.state    = state;
        this.listener = listener;
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        loadIcons();
        loadMapBackground();

        add(buildHud(),    BorderLayout.NORTH);
        add(buildLegend(), BorderLayout.EAST);

        JScrollPane scroll = buildScrollPane();
        add(scroll, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ── Carga de íconos ────────────────────────────────────────────────────

    private void loadIcons() {
        nodeIcons.put(MapNode.Type.COMBAT,   SpriteLoader.getMapIcon("combat-icon",   ICON_SIZE));
        nodeIcons.put(MapNode.Type.ELITE,    SpriteLoader.getMapIcon("miniBoss-icon",  ICON_SIZE));
        nodeIcons.put(MapNode.Type.BOSS,     SpriteLoader.getMapIcon("boss-icon",      ICON_SIZE));
        nodeIcons.put(MapNode.Type.REST,     SpriteLoader.getMapIcon("rest-icon",      ICON_SIZE));
        nodeIcons.put(MapNode.Type.SHOP,     SpriteLoader.getMapIcon("store-icon",     ICON_SIZE));
        nodeIcons.put(MapNode.Type.TREASURE, SpriteLoader.getMapIcon("treasure-icon",  ICON_SIZE));

        legendIcons.put(MapNode.Type.COMBAT,   SpriteLoader.getMapIcon("combat-icon",   LEGEND_ICON));
        legendIcons.put(MapNode.Type.ELITE,    SpriteLoader.getMapIcon("miniBoss-icon",  LEGEND_ICON));
        legendIcons.put(MapNode.Type.BOSS,     SpriteLoader.getMapIcon("boss-icon",      LEGEND_ICON));
        legendIcons.put(MapNode.Type.REST,     SpriteLoader.getMapIcon("rest-icon",      LEGEND_ICON));
        legendIcons.put(MapNode.Type.SHOP,     SpriteLoader.getMapIcon("store-icon",     LEGEND_ICON));
        legendIcons.put(MapNode.Type.TREASURE, SpriteLoader.getMapIcon("treasure-icon",  LEGEND_ICON));

        startIcon       = SpriteLoader.getMapIcon("start-icon", ICON_SIZE);
        startIconLegend = SpriteLoader.getMapIcon("start-icon", LEGEND_ICON);
    }

    private void loadMapBackground() {
        try (java.io.InputStream is = getClass().getResourceAsStream(
                "/sprites/backgrounds/map-bg.png")) {
            if (is != null) mapBackground = javax.imageio.ImageIO.read(is);
        } catch (Exception ignored) {}
    }

    // ── HUD (barra superior) ───────────────────────────────────────────────

    private JPanel buildHud() {
        JPanel hud = new JPanel(new BorderLayout(20, 0));
        hud.setBackground(new Color(0x0E, 0x0B, 0x08));
        hud.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.BORDER_ACTIVE),
            BorderFactory.createEmptyBorder(14, 28, 14, 28)
        ));

        // LEFT: piso + nombre
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        JLabel floorLabel = new JLabel("PISO  " + state.getMapLevel() + " / " + state.getTotalMaps());
        floorLabel.setFont(Theme.titleFont(22f));
        floorLabel.setForeground(Theme.TEXT_PRIMARY);
        left.add(floorLabel);
        JLabel dungName = new JLabel("· CRIPTA OLVIDADA");
        dungName.setFont(Theme.labelFont(15f));
        dungName.setForeground(Theme.TEXT_SECONDARY);
        left.add(dungName);
        hud.add(left, BorderLayout.WEST);

        // CENTER: HP de la party
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        center.setOpaque(false);
        for (Character c : state.getParty()) {
            center.add(buildHpWidget(c));
        }
        hud.add(center, BorderLayout.CENTER);

        // RIGHT: pociones + oro
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        right.setOpaque(false);

        int potCount = state.getInventory().getPotions().size();
        JLabel potLabel = new JLabel("♥  " + potCount);
        potLabel.setFont(Theme.labelFont(18f));
        potLabel.setForeground(new Color(0xE0, 0x60, 0x80));
        potLabel.setToolTipText("Pociones en inventario");
        right.add(potLabel);

        JLabel goldLabel = new JLabel("◆  " + state.getGold() + " GP");
        goldLabel.setFont(Theme.labelFont(18f));
        goldLabel.setForeground(Theme.GOLD_COLOR);
        right.add(goldLabel);

        hud.add(right, BorderLayout.EAST);
        return hud;
    }

    private JPanel buildHpWidget(Character c) {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();

                // Nombre
                g2.setFont(Theme.labelFont(14f));
                g2.setColor(c.isAlive() ? Theme.TEXT_PRIMARY : Theme.TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(c.getName(), (w - fm.stringWidth(c.getName())) / 2, 16);

                // Barra HP
                g2.setColor(Theme.BAR_BG);
                g2.fillRoundRect(0, 22, w, 9, 5, 5);
                float pct = c.isAlive() ? (float) c.getHp() / c.getHpMax() : 0f;
                Color col = pct > 0.5f ? Theme.HP_FULL : pct > 0.25f ? Theme.HP_MED : Theme.HP_LOW;
                g2.setColor(c.isAlive() ? col : Theme.TEXT_MUTED);
                g2.fillRoundRect(0, 22, Math.max(5, (int)(w * pct)), 9, 5, 5);

                // Texto HP
                String hp = c.getHp() + "/" + c.getHpMax();
                g2.setFont(Theme.bodyFont(12f));
                fm = g2.getFontMetrics();
                g2.setColor(Theme.TEXT_SECONDARY);
                g2.drawString(hp, (w - fm.stringWidth(hp)) / 2, 46);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(110, 50); }
        };
    }

    // ── Leyenda lateral ────────────────────────────────────────────────────

    private JPanel buildLegend() {
        JPanel legend = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawLegend(g2, getWidth(), getHeight());
            }
        };
        legend.setPreferredSize(new Dimension(170, 0));
        legend.setBackground(new Color(0x12, 0x0E, 0x0A));
        legend.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.SEPARATOR));
        return legend;
    }

    private void drawLegend(Graphics2D g, int w, int h) {
        int padX  = 18;
        int startY = 28;
        int rowH   = LEGEND_ICON + 14;

        // Título
        g.setFont(Theme.labelFont(13f));
        g.setColor(Theme.TEXT_PRIMARY);
        FontMetrics fmTitle = g.getFontMetrics();
        String title = "LEYENDA";
        g.drawString(title, (w - fmTitle.stringWidth(title)) / 2, startY);

        // Separador
        g.setColor(Theme.SEPARATOR);
        g.drawLine(padX, startY + 6, w - padX, startY + 6);

        int y = startY + 22;

        // Entrada especial: Inicio
        if (startIconLegend != null) g.drawImage(startIconLegend, padX, y, null);
        g.setFont(Theme.bodyFont(12f));
        g.setColor(Theme.TEXT_SECONDARY);
        g.drawString("Inicio", padX + LEGEND_ICON + 8, y + LEGEND_ICON / 2 + 5);
        y += rowH;

        MapNode.Type[] order = {
            MapNode.Type.COMBAT, MapNode.Type.ELITE, MapNode.Type.BOSS,
            MapNode.Type.REST, MapNode.Type.SHOP, MapNode.Type.TREASURE
        };
        String[] labels = { "Combate", "Élite", "Jefe", "Descanso", "Tienda", "Tesoro" };

        for (int i = 0; i < order.length; i++) {
            BufferedImage icon = legendIcons.get(order[i]);
            if (icon != null) g.drawImage(icon, padX, y, null);
            g.setFont(Theme.bodyFont(12f));
            g.setColor(Theme.TEXT_SECONDARY);
            g.drawString(labels[i], padX + LEGEND_ICON + 8, y + LEGEND_ICON / 2 + 5);
            y += rowH;
        }
    }

    // ── Canvas del mapa ────────────────────────────────────────────────────

    private JScrollPane buildScrollPane() {
        List<List<MapNode>> rows = state.getCurrentMap().getRows();
        int totalRows = rows.size();
        int maxCols   = rows.stream().mapToInt(List::size).max().orElse(1);

        int canvasW = (maxCols - 1) * COL_GAP + NODE_R * 2 + 120;
        int canvasH = PAD_TOP + totalRows * ROW_H + PAD_BOT;

        // Calcular centros de cada nodo
        nodeCenters.clear();
        for (int r = 0; r < rows.size(); r++) {
            List<MapNode> row = rows.get(r);
            int displayRow = totalRows - 1 - r;
            int cy         = PAD_TOP + displayRow * ROW_H + NODE_R;
            int totalRowW  = (row.size() - 1) * COL_GAP;
            int startX     = (canvasW - totalRowW) / 2;
            for (int c = 0; c < row.size(); c++) {
                nodeCenters.put(row.get(c), new Point(startX + c * COL_GAP, cy));
            }
        }

        JPanel canvas = new JPanel() {
            private boolean firstPaint = true;

            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawMap((Graphics2D) g, rows);
                if (firstPaint) {
                    firstPaint = false;
                    MapNode cur = state.getCurrentMap().getCurrentNode();
                    Point p = (cur != null) ? nodeCenters.get(cur) : null;
                    if (p != null) {
                        scrollRectToVisible(new Rectangle(p.x - 200, p.y - 100, 400, 300));
                    } else {
                        scrollRectToVisible(new Rectangle(0, canvasH - 300, canvasW, 300));
                    }
                }
            }
        };
        canvas.setPreferredSize(new Dimension(canvasW, canvasH));
        canvas.setOpaque(false);

        canvas.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                MapNode found = hitTest(e.getPoint());
                if (found != hoveredNode) {
                    hoveredNode = found;
                    canvas.setCursor(found != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                    canvas.repaint();
                }
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                MapNode n = hitTest(e.getPoint());
                if (n != null) listener.onNodeSelected(n);
            }
        });

        // Wrapper con fondo y GridBagLayout para centrar el canvas
        JPanel wrapper = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                int w = getWidth(), h = getHeight();
                if (mapBackground != null) {
                    // Cover: escalar manteniendo aspecto y cubriendo el panel completo
                    double sx = (double) w / mapBackground.getWidth();
                    double sy = (double) h / mapBackground.getHeight();
                    double s  = Math.max(sx, sy);
                    int bw = (int)(mapBackground.getWidth()  * s);
                    int bh = (int)(mapBackground.getHeight() * s);
                    int bx = (w - bw) / 2;
                    int by = (h - bh) / 2;
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(mapBackground, bx, by, bw, bh, null);
                    // Overlay oscuro leve para que los nodos resalten
                    g2.setColor(new Color(0, 0, 0, 60));
                    g2.fillRect(0, 0, w, h);
                } else {
                    g2.setColor(Theme.BG_DARK);
                    g2.fillRect(0, 0, w, h);
                }
            }
        };
        wrapper.setOpaque(true);
        wrapper.add(canvas);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBackground(Theme.BG_DARK);
        scroll.getViewport().setBackground(Theme.BG_DARK);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        return scroll;
    }

    private MapNode hitTest(Point p) {
        for (var entry : nodeCenters.entrySet()) {
            if (!isAvailable(entry.getKey())) continue;
            Point c = entry.getValue();
            if (Math.hypot(p.x - c.x, p.y - c.y) <= NODE_R + 4) return entry.getKey();
        }
        return null;
    }

    // ── Dibujo del mapa ────────────────────────────────────────────────────

    private void drawMap(Graphics2D g, List<List<MapNode>> rows) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        List<MapNode> available = state.getCurrentMap().getAvailableNodes();
        MapNode current         = state.getCurrentMap().getCurrentNode();

        // Conexiones primero (debajo de los nodos)
        for (List<MapNode> row : rows) {
            for (MapNode node : row) {
                Point src = nodeCenters.get(node);
                if (src == null) continue;
                for (MapNode next : node.getNext()) {
                    Point dst = nodeCenters.get(next);
                    if (dst == null) continue;
                    drawConnection(g, src, dst,
                        node.isVisited() && next.isVisited(),
                        available.contains(next));
                }
            }
        }

        // Nodos encima
        for (var entry : nodeCenters.entrySet()) {
            drawNode(g, entry.getKey(), entry.getValue(), available, current);
        }
    }

    private void drawConnection(Graphics2D g, Point src, Point dst,
                                 boolean visited, boolean active) {
        double dx = dst.x - src.x, dy = dst.y - src.y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) return;
        double ux = dx / dist, uy = dy / dist;

        int offset = NODE_R_SM + 2;
        int x1 = src.x + (int)(ux * offset);
        int y1 = src.y + (int)(uy * offset);
        int x2 = dst.x - (int)(ux * offset);
        int y2 = dst.y - (int)(uy * offset);

        Color  color;
        Stroke stroke;
        if (visited) {
            color  = new Color(0xE0, 0xC0, 0x80, 220);
            stroke = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        } else if (active) {
            color  = new Color(0xD0, 0xB8, 0x90, 210);
            stroke = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                         0, new float[]{6f, 5f}, 0);
        } else {
            color  = new Color(0xA0, 0x90, 0x78, 160);
            stroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                         0, new float[]{3f, 6f}, 0);
        }

        g.setColor(color);
        g.setStroke(stroke);
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawNode(Graphics2D g, MapNode node, Point c,
                           List<MapNode> available, MapNode current) {
        boolean isCurrent = node == current;
        boolean isAvail   = available.contains(node);
        boolean isHovered = node == hoveredNode;
        boolean isVisited = node.isVisited() && !isCurrent;

        int r = (isAvail || isCurrent) ? NODE_R : NODE_R_SM;

        Color fill, ring;
        float ringW;

        if (isCurrent) {
            fill  = (node.getRow() == 0) ? new Color(0x28, 0x0A, 0x40) : getNodeFill(node.getType());
            ring  = Theme.BORDER_ACTIVE;
            ringW = 3f;
        } else if (isAvail) {
            fill  = (node.getRow() == 0) ? new Color(0x28, 0x0A, 0x40) : getNodeFill(node.getType());
            ring  = isHovered ? Color.WHITE : ((node.getRow() == 0) ? new Color(0xB0, 0x50, 0xE8) : getNodeRing(node.getType()));
            ringW = isHovered ? 3f : 2f;
        } else if (isVisited) {
            fill  = new Color(0x14, 0x10, 0x0C);
            ring  = new Color(0x28, 0x20, 0x18);
            ringW = 1.5f;
        } else {
            fill  = new Color(0x0E, 0x0A, 0x08);
            ring  = new Color(0x1E, 0x18, 0x12);
            ringW = 1f;
        }

        // Halo de brillo
        if (isAvail) {
            Color nr = (node.getRow() == 0) ? new Color(0xB0, 0x50, 0xE8) : getNodeRing(node.getType());
            int alpha = isHovered ? 65 : 28;
            g.setColor(new Color(nr.getRed(), nr.getGreen(), nr.getBlue(), alpha));
            g.fillOval(c.x - r - 8, c.y - r - 8, (r + 8) * 2, (r + 8) * 2);
        }

        // Círculo relleno
        g.setColor(fill);
        g.fillOval(c.x - r, c.y - r, r * 2, r * 2);

        // Borde
        g.setColor(ring);
        g.setStroke(new BasicStroke(ringW));
        g.drawOval(c.x - r, c.y - r, r * 2, r * 2);
        g.setStroke(new BasicStroke(1f));

        // Ícono PNG (nodo de inicio usa sprite propio)
        BufferedImage icon = (node.getRow() == 0) ? startIcon : nodeIcons.get(node.getType());
        if (icon != null) {
            int iconSize = (isAvail || isCurrent) ? ICON_SIZE : (int)(ICON_SIZE * 0.75f);
            int ix = c.x - iconSize / 2;
            int iy = c.y - iconSize / 2;

            Composite original = g.getComposite();
            if (isVisited) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
            } else if (!isAvail && !isCurrent) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
            }
            g.drawImage(icon, ix, iy, iconSize, iconSize, null);
            g.setComposite(original);
        }

        // Etiqueta debajo (solo disponibles / actual con hover)
        if ((isAvail && isHovered) || isCurrent) {
            String label = (node.getRow() == 0) ? "PUERTA" : node.getLabel();
            g.setFont(Theme.labelFont(10f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(isCurrent ? Theme.BORDER_ACTIVE : Theme.TEXT_PRIMARY);
            g.drawString(label, c.x - fm.stringWidth(label) / 2, c.y + r + 15);
        }

        // Triángulo indicador de posición actual
        if (isCurrent) {
            g.setColor(Theme.BORDER_ACTIVE);
            int[] xs = { c.x - 5, c.x + 5, c.x };
            int[] ys = { c.y - r - 12, c.y - r - 12, c.y - r - 3 };
            g.fillPolygon(xs, ys, 3);
        }
    }

    private boolean isAvailable(MapNode node) {
        return state.getCurrentMap().getAvailableNodes().contains(node);
    }

    private Color getNodeFill(MapNode.Type type) {
        return switch (type) {
            case COMBAT   -> new Color(0x42, 0x14, 0x10);
            case ELITE    -> new Color(0x38, 0x2E, 0x08);
            case BOSS     -> new Color(0x58, 0x08, 0x08);
            case REST     -> new Color(0x0C, 0x22, 0x40);
            case SHOP     -> new Color(0x28, 0x0E, 0x42);
            case TREASURE -> new Color(0x0E, 0x2C, 0x0E);
        };
    }

    private Color getNodeRing(MapNode.Type type) {
        return switch (type) {
            case COMBAT   -> new Color(0xC0, 0x40, 0x30);
            case ELITE    -> new Color(0xD0, 0xB0, 0x20);
            case BOSS     -> new Color(0xFF, 0x20, 0x20);
            case REST     -> new Color(0x30, 0x80, 0xD0);
            case SHOP     -> new Color(0xA0, 0x50, 0xE0);
            case TREASURE -> new Color(0x40, 0xD0, 0x40);
        };
    }

    // ── Footer ─────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BG_PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SEPARATOR));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 8));
        btns.setOpaque(false);
        JButton partyBtn = new JButton("Ver Party");
        Theme.styleButton(partyBtn);
        partyBtn.addActionListener(e -> listener.onPartyRequested());
        btns.add(partyBtn);
        JButton saveBtn = new JButton("Guardar partida");
        Theme.styleButton(saveBtn);
        saveBtn.addActionListener(e -> listener.onSaveRequested());
        btns.add(saveBtn);
        bar.add(btns, BorderLayout.EAST);

        return bar;
    }
}
