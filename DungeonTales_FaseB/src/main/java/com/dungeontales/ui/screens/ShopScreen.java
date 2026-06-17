package com.dungeontales.ui.screens;

import com.dungeontales.core.GameState;
import com.dungeontales.core.model.items.Item;
import com.dungeontales.core.model.items.Potion;
import com.dungeontales.util.SpriteLoader;
import com.dungeontales.util.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends JPanel {

    public interface Listener { void onDone(); }

    private static final Object[][] SHOP_ITEMS = {
        { Potion.smallHeal(),    25,  "potion-heal"   },
        { Potion.paPotion(),     40,  "potion-pa"     },
        { Potion.revivePotion(), 110, "potion-revive" },
    };

    private static final int COLS     = 3;
    private static final int CARD_W   = 180;
    private static final int CARD_H   = 230;
    private static final int CARD_PAD = 10;   // espacio extra para el efecto de escala
    private static final int CARD_GAP = 16;
    private static final int ICON_SZ  = 72;

    private BufferedImage background;
    private JPanel        goldPanel;
    private GameState     state;

    public ShopScreen(GameState state, Listener listener) {
        this.state = state;
        loadBackground();
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(0x06, 0x04, 0x08));

        add(buildHeader(state),    BorderLayout.NORTH);
        add(buildGrid(state),      BorderLayout.CENTER);
        add(buildFooter(listener), BorderLayout.SOUTH);
    }

    // ── Fondo ──────────────────────────────────────────────────────────────

    private void loadBackground() {
        try (InputStream is = getClass().getResourceAsStream("/sprites/backgrounds/shop-bg.png")) {
            if (is != null) background = ImageIO.read(is);
        } catch (Exception ignored) {}
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background == null) return;
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth(), h = getHeight();
        double s = Math.max((double) w / background.getWidth(),
                            (double) h / background.getHeight());
        int bw = (int)(background.getWidth()  * s);
        int bh = (int)(background.getHeight() * s);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(background, (w - bw) / 2, (h - bh) / 2, bw, bh, null);
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRect(0, 0, w, h);
    }

    // ── Header: placa tallada + monedero ───────────────────────────────────

    private JPanel buildHeader(GameState state) {
        JPanel header = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                // Banda oscura de fondo
                g2.setColor(new Color(0x00, 0x00, 0x00, 155));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Línea dorada inferior
                g2.setColor(new Color(0xA0, 0x70, 0x20, 130));
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        // ── Placa de piedra tallada (título) ──────────────────────────────
        JPanel plaque = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Fondo de la placa: gradiente de piedra
                GradientPaint stone = new GradientPaint(0, 0, new Color(0x30, 0x26, 0x1A),
                                                         0, h, new Color(0x1A, 0x12, 0x0A));
                g2.setPaint(stone);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Borde exterior dorado grueso
                g2.setColor(new Color(0xB8, 0x88, 0x30));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 9, 9);

                // Borde interior fino
                g2.setColor(new Color(0x70, 0x50, 0x18, 180));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(4, 4, w - 9, h - 9, 6, 6);

                // Esquinas ornamentales (pequeños cuadrados dorados)
                int cs = 6;
                g2.setColor(new Color(0xC8, 0x98, 0x38));
                g2.fillRect(0,     0,     cs, cs);
                g2.fillRect(w - cs, 0,    cs, cs);
                g2.fillRect(0,     h - cs, cs, cs);
                g2.fillRect(w - cs, h - cs, cs, cs);

                // Ornamentos en los extremos del título (líneas y diamante)
                int midY = h / 2;
                g2.setColor(new Color(0xA0, 0x78, 0x28, 160));
                g2.drawLine(10, midY, 24, midY);
                g2.drawLine(w - 24, midY, w - 10, midY);
                int[] dx = {17, 21, 17, 13};
                int[] dy = {midY - 4, midY, midY + 4, midY};
                g2.fillPolygon(dx, dy, 4);
                int[] dx2 = {w-17, w-13, w-17, w-21};
                int[] dy2 = {midY - 4, midY, midY + 4, midY};
                g2.fillPolygon(dx2, dy2, 4);

                // Texto del título con efecto de piedra tallada
                Font titleFont = Theme.titleFont(22f);
                g2.setFont(titleFont);
                FontMetrics fm = g2.getFontMetrics();
                String text = "TIENDA DEL MERCADER";
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;

                // Sombra profunda
                g2.setColor(new Color(0x00, 0x00, 0x00, 200));
                g2.drawString(text, tx + 2, ty + 2);
                // Cuerpo de piedra
                g2.setColor(new Color(0x60, 0x48, 0x28));
                g2.drawString(text, tx + 1, ty + 1);
                // Texto dorado principal
                g2.setColor(new Color(0xD8, 0xAA, 0x48));
                g2.drawString(text, tx, ty);
                // Brillo
                g2.setColor(new Color(0xFF, 0xE8, 0xA0, 100));
                g2.drawString(text, tx - 1, ty - 1);

                g2.setStroke(new BasicStroke(1f));
            }
            @Override public Dimension getPreferredSize() {
                FontMetrics fm = getFontMetrics(Theme.titleFont(22f));
                int textW = fm.stringWidth("TIENDA DEL MERCADER");
                return new Dimension(textW + 80, 54);
            }
        };

        // ── Monedero de monedas ───────────────────────────────────────────
        goldPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Fondo del monedero
                g2.setColor(new Color(0x10, 0x0C, 0x04, 200));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.setColor(new Color(0x90, 0x68, 0x20, 160));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.setStroke(new BasicStroke(1f));

                // Pila de monedas (3 círculos escalonados)
                int cx = 20, cy = h / 2;
                int[] offsets = {4, 2, 0};
                for (int i = 0; i < 3; i++) {
                    int ox = offsets[i];
                    // Sombra de moneda
                    g2.setColor(new Color(0x40, 0x28, 0x00, 120));
                    g2.fillOval(cx - 8 + ox + 1, cy - 9 + 1, 16, 16);
                    // Cara de moneda
                    GradientPaint coin = new GradientPaint(
                        cx - 8 + ox, cy - 9, new Color(0xF0, 0xC0, 0x30),
                        cx + 8 + ox, cy + 9, new Color(0xA0, 0x70, 0x18));
                    g2.setPaint(coin);
                    g2.fillOval(cx - 8 + ox, cy - 9, 16, 16);
                    // Borde
                    g2.setColor(new Color(0xC8, 0x90, 0x28));
                    g2.drawOval(cx - 8 + ox, cy - 9, 16, 16);
                    // Símbolo
                    g2.setColor(new Color(0x80, 0x50, 0x10));
                    g2.setFont(Theme.bodyFont(8f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("G", cx + ox - fm.stringWidth("G")/2, cy + fm.getAscent()/2 - 1);
                }

                // Cantidad de oro
                g2.setFont(Theme.labelFont(18f));
                FontMetrics fm = g2.getFontMetrics();
                String goldText = state.getGold() + " GP";
                int tx = 44;
                // Sombra
                g2.setColor(new Color(0x00, 0x00, 0x00, 180));
                g2.drawString(goldText, tx + 1, h/2 + fm.getAscent()/2 - 1);
                // Texto dorado
                g2.setColor(new Color(0xF0, 0xC8, 0x50));
                g2.drawString(goldText, tx, h/2 + fm.getAscent()/2 - 2);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(160, 54); }
        };

        header.add(plaque,    BorderLayout.WEST);
        header.add(goldPanel, BorderLayout.EAST);
        return header;
    }

    // ── Grilla de tarjetas ─────────────────────────────────────────────────

    private JPanel buildGrid(GameState state) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel backdrop = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x00, 0x00, 0x00, 120));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(new Color(0x80, 0x60, 0x28, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            }
        };
        backdrop.setOpaque(false);
        backdrop.setLayout(new GridBagLayout());
        backdrop.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        List<ItemCard> cards = new ArrayList<>();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(CARD_GAP / 2, CARD_GAP / 2, CARD_GAP / 2, CARD_GAP / 2);
        gbc.anchor = GridBagConstraints.CENTER;

        for (int i = 0; i < SHOP_ITEMS.length; i++) {
            Potion p    = (Potion) SHOP_ITEMS[i][0];
            int    cost = (int)    SHOP_ITEMS[i][1];
            String icon = (String) SHOP_ITEMS[i][2];

            BufferedImage img = SpriteLoader.getShopIcon(icon, ICON_SZ);
            ItemCard card = new ItemCard(p, cost, img, state, () -> {
                goldPanel.repaint();
                cards.forEach(ItemCard::refreshEnabled);
            });
            cards.add(card);
            gbc.gridx = i % COLS;
            gbc.gridy = i / COLS;
            backdrop.add(card, gbc);
        }

        // Relleno de celdas vacías
        int total = SHOP_ITEMS.length;
        int remainder = total % COLS;
        if (remainder != 0) {
            for (int i = remainder; i < COLS; i++) {
                JPanel empty = new JPanel();
                empty.setOpaque(false);
                empty.setPreferredSize(new Dimension(CARD_W + CARD_PAD * 2, CARD_H + CARD_PAD * 2));
                gbc.gridx = i;
                gbc.gridy = total / COLS;
                backdrop.add(empty, gbc);
            }
        }

        wrapper.add(backdrop);
        return wrapper;
    }

    // ── Footer ─────────────────────────────────────────────────────────────

    private JPanel buildFooter(Listener listener) {
        JPanel footer = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0x00, 0x00, 0x00, 140));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0xA0, 0x70, 0x20, 100));
                g2.fillRect(0, 0, getWidth(), 2);
            }
        };
        footer.setOpaque(false);
        footer.setLayout(new FlowLayout(FlowLayout.RIGHT, 24, 12));

        JPanel exitBtn = new JPanel(new GridBagLayout()) {
            boolean hover = false;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(hover ? new Color(0x28, 0x1C, 0x0A, 220) : new Color(0x12, 0x0C, 0x06, 200));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.setColor(hover ? new Color(0xC8, 0xA0, 0x50) : new Color(0x80, 0x60, 0x30));
                g2.setStroke(new BasicStroke(hover ? 1.8f : 1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                int cs = 4;
                g2.fillRect(0,     0,     cs, cs);   g2.fillRect(w-cs, 0,     cs, cs);
                g2.fillRect(0, h - cs, cs, cs);      g2.fillRect(w-cs, h-cs, cs, cs);
                g2.setStroke(new BasicStroke(1f));
            }
            { setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
              addMouseListener(new MouseAdapter() {
                  @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                  @Override public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                  @Override public void mouseClicked(MouseEvent e) { listener.onDone(); }
              }); }
        };
        exitBtn.setPreferredSize(new Dimension(200, 44));
        JLabel lbl = new JLabel("Salir de la tienda");
        lbl.setFont(Theme.labelFont(14f));
        lbl.setForeground(new Color(0xD4, 0xB8, 0x70));
        exitBtn.add(lbl);
        footer.add(exitBtn);
        return footer;
    }

    // ── Tarjeta de ítem ────────────────────────────────────────────────────

    private class ItemCard extends JPanel {

        private final Potion        potion;
        private final int           cost;
        private final GameState     state;
        private final Runnable      onBuy;
        private final BufferedImage icon;
        private boolean             hover = false;
        private boolean             canAfford;

        ItemCard(Potion potion, int cost, BufferedImage icon, GameState state, Runnable onBuy) {
            this.potion    = potion;
            this.cost      = cost;
            this.icon      = icon;
            this.state     = state;
            this.onBuy     = onBuy;
            this.canAfford = canBuy();

            setOpaque(false);
            // Tamaño total = tarjeta + padding para el efecto de escala
            setPreferredSize(new Dimension(CARD_W + CARD_PAD * 2, CARD_H + CARD_PAD * 2));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (canAfford) { hover = true; repaint(); }
                }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    if (!canAfford) return;
                    if (!state.spendGold(cost)) return;
                    state.getInventory().addItem(newInstance());
                    canAfford = canBuy();
                    hover = false;
                    onBuy.run();
                    repaint();
                    JOptionPane.showMessageDialog(ShopScreen.this,
                        potion.getName() + " añadida al inventario.",
                        "Compra exitosa", JOptionPane.PLAIN_MESSAGE);
                }
            });
        }

        void refreshEnabled() { canAfford = canBuy(); repaint(); }

        private boolean canBuy()  { return state.getGold() >= cost && !atMax(); }
        private int stockCount()  { return state.getInventory().getPotionCount(potion.getEffect()); }
        private boolean atMax()   { return stockCount() >= state.getInventory().getMaxPerPotion(); }

        private Potion newInstance() {
            return switch (potion.getEffect()) {
                case HEAL_SMALL -> Potion.smallHeal();
                case REVIVE     -> Potion.revivePotion();
                case PA_RESTORE -> Potion.paPotion();
                default -> throw new IllegalArgumentException("Unknown potion effect: " + potion.getEffect());
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            boolean maxed = atMax();
            boolean poor  = !canAfford && !maxed;
            Color rarity  = rarityColor(potion.getRarity());

            // ── Coordenadas de la tarjeta según estado hover (efecto escala) ──
            int cx, cy, cw, ch;
            if (hover && canAfford) {
                // Escala ~105%: expande hacia los bordes del padding
                cx = CARD_PAD / 2;   cy = CARD_PAD / 2;
                cw = CARD_W + CARD_PAD; ch = CARD_H + CARD_PAD;
            } else {
                cx = CARD_PAD;   cy = CARD_PAD;
                cw = CARD_W;     ch = CARD_H;
            }

            Composite original = g2.getComposite();

            // ── Outer glow en hover ────────────────────────────────────────
            if (hover && canAfford) {
                Color glow = new Color(0xD0, 0xA0, 0x40);
                for (int i = 5; i >= 1; i--) {
                    g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), i * 12));
                    g2.setStroke(new BasicStroke(i * 2f));
                    g2.drawRoundRect(cx - i * 2, cy - i * 2,
                                     cw + i * 4, ch + i * 4, 16, 16);
                }
                g2.setStroke(new BasicStroke(1f));
            }

            // ── Colores del fondo y borde ──────────────────────────────────
            Color bgColor, borderColor;
            if (maxed) {
                bgColor     = new Color(0x08, 0x06, 0x10, 185);
                borderColor = new Color(0x60, 0x48, 0x80);
            } else if (poor) {
                bgColor     = new Color(0x0C, 0x0A, 0x08, 175);
                borderColor = new Color(0x35, 0x2C, 0x1E);
            } else if (hover) {
                bgColor     = new Color(0x28, 0x1C, 0x0A, 225);
                borderColor = new Color(0xD8, 0xA8, 0x48);
            } else {
                bgColor     = new Color(0x14, 0x0E, 0x06, 205);
                borderColor = rarity;
            }

            // ── Fondo ─────────────────────────────────────────────────────
            g2.setColor(bgColor);
            g2.fillRoundRect(cx, cy, cw, ch, 12, 12);

            // ── Franja de rareza superior ─────────────────────────────────
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                                  borderColor.getBlue(), hover ? 90 : 55));
            g2.fillRoundRect(cx, cy, cw, 38, 12, 12);
            g2.fillRect(cx, cy + 20, cw, 18);

            // ── Borde principal ───────────────────────────────────────────
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(hover ? 2.2f : 1.5f));
            g2.drawRoundRect(cx, cy, cw, ch, 12, 12);
            g2.setStroke(new BasicStroke(1f));

            // ── Aura radial detrás del ícono ──────────────────────────────
            int ix = cx + (cw - ICON_SZ) / 2;
            int iy = cy + 10;
            float auraR = ICON_SZ * 0.85f;
            RadialGradientPaint aura = new RadialGradientPaint(
                new Point2D.Float(ix + ICON_SZ / 2f, iy + ICON_SZ / 2f),
                auraR,
                new float[]{0f, 1f},
                new Color[]{
                    new Color(rarity.getRed(), rarity.getGreen(), rarity.getBlue(),
                              poor ? 25 : hover ? 110 : 70),
                    new Color(rarity.getRed(), rarity.getGreen(), rarity.getBlue(), 0)
                }
            );
            g2.setPaint(aura);
            g2.fillOval((int)(ix + ICON_SZ/2f - auraR), (int)(iy + ICON_SZ/2f - auraR),
                        (int)(auraR * 2), (int)(auraR * 2));

            // ── Ícono ─────────────────────────────────────────────────────
            if (icon != null) {
                float iconAlpha = poor ? 0.30f : 1f;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, iconAlpha));
                g2.drawImage(icon, ix, iy, ICON_SZ, ICON_SZ, null);
                g2.setComposite(original);
            }

            // ── Overlay gris (sin oro) ────────────────────────────────────
            if (poor) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                g2.setColor(new Color(0x18, 0x14, 0x10));
                g2.fillRoundRect(cx, cy, cw, ch, 12, 12);
                g2.setComposite(original);
            }

            // ── Línea separadora ──────────────────────────────────────────
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                                  borderColor.getBlue(), 70));
            g2.drawLine(cx + 10, cy + 92, cx + cw - 10, cy + 92);

            // ── Nombre ────────────────────────────────────────────────────
            g2.setFont(Theme.labelFont(13f));
            FontMetrics fm = g2.getFontMetrics();
            String name = potion.getName();
            int nx = cx + (cw - fm.stringWidth(name)) / 2;
            int ny = cy + 110;
            g2.setColor(new Color(0, 0, 0, 170));
            g2.drawString(name, nx + 1, ny + 1);
            g2.setColor(poor ? new Color(0x60, 0x54, 0x3A) : new Color(0xE8, 0xD0, 0x90));
            g2.drawString(name, nx, ny);

            // ── Descripción ───────────────────────────────────────────────
            g2.setFont(Theme.bodyFont(10f));
            fm = g2.getFontMetrics();
            g2.setColor(poor ? new Color(0x48, 0x40, 0x30) : new Color(0x98, 0x88, 0x68));
            drawWrapped(g2, potion.getDescription(), cx + 8, cy + 124, cw - 16, fm);

            // ── Stock "En inventario: X/5" ────────────────────────────────
            int maxStack = state.getInventory().getMaxPerPotion();
            int cnt      = stockCount();
            g2.setFont(Theme.bodyFont(10f));
            fm = g2.getFontMetrics();
            String stock = "En inventario: " + cnt + "/" + maxStack;
            int sx = cx + (cw - fm.stringWidth(stock)) / 2;
            int sy = cy + ch - 36;
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(stock, sx + 1, sy + 1);
            Color stockCol = maxed    ? new Color(0xB0, 0x78, 0xFF)
                           : cnt > 0  ? new Color(0x70, 0xCC, 0x80)
                           :             new Color(0x60, 0x58, 0x48);
            g2.setColor(stockCol);
            g2.drawString(stock, sx, sy);

            // ── Precio / estado ───────────────────────────────────────────
            Font priceFont = Theme.labelFont(maxed ? 11f : poor ? 12f : 15f);
            g2.setFont(priceFont);
            fm = g2.getFontMetrics();
            String bottomText;
            Color  bottomColor;
            if (maxed) {
                bottomText  = "✕  Máximo alcanzado";
                bottomColor = new Color(0x90, 0x70, 0xB0);
            } else if (poor) {
                bottomText  = "◆  " + cost + " GP  (sin oro)";
                bottomColor = new Color(0xD0, 0x40, 0x30);
            } else {
                bottomText  = "◆  " + cost + " GP";
                bottomColor = Theme.GOLD_COLOR;
            }
            int px = cx + (cw - fm.stringWidth(bottomText)) / 2;
            int py = cy + ch - 14;
            g2.setColor(new Color(0, 0, 0, 190));
            g2.drawString(bottomText, px + 1, py + 1);
            g2.setColor(bottomColor);
            g2.drawString(bottomText, px, py);
        }

        private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxW, FontMetrics fm) {
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int lineY = y;
            for (String word : words) {
                String test = line.isEmpty() ? word : line + " " + word;
                if (fm.stringWidth(test) > maxW) {
                    String l = line.toString();
                    g2.drawString(l, x + (maxW - fm.stringWidth(l)) / 2, lineY);
                    line = new StringBuilder(word);
                    lineY += fm.getHeight();
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (!line.isEmpty()) {
                String l = line.toString();
                g2.drawString(l, x + (maxW - fm.stringWidth(l)) / 2, lineY);
            }
        }

        private Color rarityColor(Item.Rarity rarity) {
            return switch (rarity) {
                case COMMON   -> new Color(0x80, 0x80, 0x80);
                case UNCOMMON -> new Color(0x40, 0xB0, 0x50);
                case RARE     -> new Color(0x40, 0x80, 0xD0);
                case EPIC     -> new Color(0xA0, 0x40, 0xD0);
            };
        }
    }
}
