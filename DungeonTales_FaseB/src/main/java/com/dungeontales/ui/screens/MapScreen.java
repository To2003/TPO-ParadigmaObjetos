package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.core.map.MapNode;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    // Layout de nodos para dibujo
    private final Map<MapNode, Rectangle> nodeBounds = new HashMap<>();
    private MapNode hoveredNode = null;

    private static final int NODE_W    = 160;
    private static final int NODE_H    = 60;
    private static final int ROW_H     = 100;
    private static final int PAD_X     = 100;
    private static final int PAD_TOP   = 100;

    public MapScreen(GameState state, Listener listener) {
        this.state    = state;
        this.listener = listener;
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        // Header con estado de la party
        add(buildHeader(), BorderLayout.NORTH);

        // Canvas del mapa
        JPanel mapCanvas = buildMapCanvas();
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.BG_DARK);
        wrapper.add(mapCanvas);
        
        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBackground(Theme.BG_DARK);
        scroll.getViewport().setBackground(Theme.BG_DARK);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        // Footer con botón guardar
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_PANEL);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        JLabel levelLabel = new JLabel("NIVEL " + state.getMapLevel() + " / " + state.getTotalMaps()
            + "   —   CRIPTA OLVIDADA");
        levelLabel.setFont(Theme.labelFont(15f));
        levelLabel.setForeground(Theme.TEXT_PRIMARY);

        // HP resumido de la party
        JPanel partyHp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        partyHp.setBackground(Theme.BG_PANEL);
        for (var c : state.getParty()) {
            JLabel lbl = new JLabel(c.getName() + " " + c.getHp() + "/" + c.getHpMax());
            lbl.setFont(Theme.bodyFont(15f));
            Color col = (float)c.getHp()/c.getHpMax() > 0.5f ? Theme.HP_FULL :
                        (float)c.getHp()/c.getHpMax() > 0.25f ? Theme.HP_MED : Theme.HP_LOW;
            lbl.setForeground(c.isAlive() ? col : Theme.TEXT_MUTED);
            partyHp.add(lbl);
        }

        JLabel goldLabel = new JLabel("Oro: " + state.getGold() + " GP");
        goldLabel.setFont(Theme.bodyFont(15f));
        goldLabel.setForeground(Theme.GOLD_COLOR);
        partyHp.add(goldLabel);

        header.add(levelLabel, BorderLayout.WEST);
        header.add(partyHp, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMapCanvas() {
        List<List<MapNode>> rows = state.getCurrentMap().getRows();
        int totalRows = rows.size();
        int maxCols   = rows.stream().mapToInt(List::size).max().orElse(1);
        int canvasW   = Math.max(1200, PAD_X * 2 + maxCols * (NODE_W + 50));
        int canvasH   = PAD_TOP + totalRows * ROW_H + 100;

        // Precalcular posiciones
        nodeBounds.clear();
        for (int r = 0; r < rows.size(); r++) {
            List<MapNode> row = rows.get(r);
            int displayRow = totalRows - 1 - r; // invertido: inicio abajo, jefe arriba
            int rowY = PAD_TOP + displayRow * ROW_H;
            int totalW = row.size() * NODE_W + (row.size() - 1) * 50;
            int startX = (canvasW - totalW) / 2;
            for (int c = 0; c < row.size(); c++) {
                int nx = startX + c * (NODE_W + 50);
                nodeBounds.put(row.get(c), new Rectangle(nx, rowY, NODE_W, NODE_H));
            }
        }

        JPanel canvas = new JPanel() {
            private boolean firstPaint = true;
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawMap((Graphics2D) g, rows);
                if (firstPaint && state.getCurrentMap().getCurrentNode() != null) {
                    firstPaint = false;
                    Rectangle r = nodeBounds.get(state.getCurrentMap().getCurrentNode());
                    if (r != null) {
                        Rectangle viewR = new Rectangle(r.x - 200, r.y - 100, r.width + 400, r.height + 200);
                        scrollRectToVisible(viewR);
                    }
                }
            }
        };
        canvas.setPreferredSize(new Dimension(canvasW, canvasH));
        canvas.setBackground(Theme.BG_DARK);

        // Interacción del mouse
        canvas.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                MapNode found = null;
                for (var entry : nodeBounds.entrySet()) {
                    if (entry.getValue().contains(e.getPoint()) && isAvailable(entry.getKey())) {
                        found = entry.getKey();
                        break;
                    }
                }
                if (found != hoveredNode) { hoveredNode = found; canvas.repaint(); }
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                for (var entry : nodeBounds.entrySet()) {
                    if (entry.getValue().contains(e.getPoint()) && isAvailable(entry.getKey())) {
                        listener.onNodeSelected(entry.getKey());
                        return;
                    }
                }
            }
        });

        return canvas;
    }

    private void drawMap(Graphics2D g, List<List<MapNode>> rows) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<MapNode> available = state.getCurrentMap().getAvailableNodes();
        MapNode current = state.getCurrentMap().getCurrentNode();

        // Dibujar conexiones primero
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            0, new float[]{4, 4}, 0));
        for (var row : rows) {
            for (var node : row) {
                Rectangle nb = nodeBounds.get(node);
                if (nb == null) continue;
                for (var next : node.getNext()) {
                    Rectangle cb = nodeBounds.get(next);
                    if (cb == null) continue;
                    g.setColor(Theme.SEPARATOR);
                    g.drawLine(nb.x + nb.width/2, nb.y,
                               cb.x + cb.width/2, cb.y + cb.height);
                }
            }
        }
        g.setStroke(new BasicStroke(1.5f));

        // Dibujar nodos
        for (var entry : nodeBounds.entrySet()) {
            MapNode node = entry.getKey();
            Rectangle r  = entry.getValue();
            boolean isCurrent   = node == current;
            boolean isAvail     = available.contains(node);
            boolean isHovered   = node == hoveredNode;
            boolean isVisited   = node.isVisited() && node != current;

            Color fill   = getNodeFill(node.getType());
            Color border = isCurrent ? Theme.BORDER_ACTIVE :
                           isHovered ? Theme.TEXT_PRIMARY :
                           isAvail   ? fill.brighter() :
                           isVisited ? Theme.BG_PANEL :
                           Theme.BG_CARD;

            // Sombra si está disponible
            if (isAvail || isCurrent) {
                g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 30));
                g.fillRoundRect(r.x - 3, r.y - 3, r.width + 6, r.height + 6, 10, 10);
            }

            // Fondo del nodo
            g.setColor(isVisited ? Theme.BG_PANEL : fill.darker().darker());
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);

            // Borde
            g.setColor(border);
            g.setStroke(new BasicStroke(isCurrent || isHovered ? 2f : 1f));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setStroke(new BasicStroke(1.5f));

            // Texto
            String label = node.getLabel();
            g.setFont(Theme.labelFont(isAvail ? 14f : 12f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(isVisited ? Theme.TEXT_MUTED : isAvail ? Theme.TEXT_WHITE : Theme.TEXT_SECONDARY);
            g.drawString(label,
                r.x + (r.width - fm.stringWidth(label)) / 2,
                r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);

            // Indicador de posición actual
            if (isCurrent) {
                g.setColor(Theme.BORDER_ACTIVE);
                int[] xs = {r.x + r.width/2 - 4, r.x + r.width/2 + 4, r.x + r.width/2};
                int[] ys = {r.y - 10, r.y - 10, r.y - 3};
                g.fillPolygon(xs, ys, 3);
            }
        }
    }

    private boolean isAvailable(MapNode node) {
        return state.getCurrentMap().getAvailableNodes().contains(node);
    }

    private Color getNodeFill(MapNode.Type type) {
        return switch (type) {
            case COMBAT   -> new Color(0x5A, 0x20, 0x20);
            case ELITE    -> new Color(0x4A, 0x40, 0x10);
            case BOSS     -> new Color(0x7A, 0x10, 0x10);
            case REST     -> new Color(0x10, 0x30, 0x5A);
            case SHOP     -> new Color(0x35, 0x15, 0x55);
            case TREASURE -> new Color(0x15, 0x3A, 0x15);
        };
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        footer.setBackground(Theme.BG_PANEL);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SEPARATOR));

        JButton partyBtn = new JButton("Ver Party");
        Theme.styleButton(partyBtn);
        partyBtn.addActionListener(e -> listener.onPartyRequested());
        footer.add(partyBtn);

        JButton saveBtn = new JButton("Guardar partida");
        Theme.styleButton(saveBtn);
        saveBtn.addActionListener(e -> listener.onSaveRequested());
        footer.add(saveBtn);

        // Leyenda
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        legend.setBackground(Theme.BG_PANEL);
        addLegendItem(legend, "COMBATE",  new Color(0x5A,0x20,0x20));
        addLegendItem(legend, "ÉLITE",    new Color(0x4A,0x40,0x10));
        addLegendItem(legend, "DESCANSO", new Color(0x10,0x30,0x5A));
        addLegendItem(legend, "TIENDA",   new Color(0x35,0x15,0x55));
        addLegendItem(legend, "TESORO",   new Color(0x15,0x3A,0x15));
        addLegendItem(legend, "JEFE",     new Color(0x7A,0x10,0x10));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BG_PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SEPARATOR));
        bar.add(legend, BorderLayout.WEST);
        bar.add(footer, BorderLayout.EAST);
        return bar;
    }

    private void addLegendItem(JPanel p, String label, Color color) {
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(color);
                g.fillRoundRect(0, 2, 10, 10, 4, 4);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(10, 14); }
        };
        dot.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.bodyFont(10f));
        lbl.setForeground(Theme.TEXT_MUTED);
        p.add(dot);
        p.add(lbl);
    }
}
