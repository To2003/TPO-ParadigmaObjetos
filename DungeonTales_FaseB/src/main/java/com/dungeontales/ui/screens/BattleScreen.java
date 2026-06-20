package com.dungeontales.ui.screens;

import com.dungeontales.core.battle.BattleEngine;
import com.dungeontales.core.battle.BattleEvent;
import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.core.model.enemy.Enemy;
import com.dungeontales.core.model.items.Potion;
import com.dungeontales.core.model.Inventory;
import com.dungeontales.ui.components.*;
import com.dungeontales.util.Theme;
import com.dungeontales.util.SpriteLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla de batalla principal.
 * Layout: Party (izq) | VS | Enemigos (der)
 *         Turno / Log de combate
 *         Barra de acciones (cuando es turno del jugador)
 */
public class BattleScreen extends JPanel {

    public interface Listener {
        void onBattleWon(int expGained, int goldGained, String itemFound, List<String> levelUps);
        void onBattleGameOver();
    }

    private final BattleEngine          engine;
    private final List<Character>       party;
    private final List<Enemy>           enemies;
    private final Inventory             inventory;
    private final Listener              listener;

    // Componentes UI
    private final List<CharacterCard>   charCards   = new ArrayList<>();
    private final List<EnemyCard>       enemyCards  = new ArrayList<>();
    private       JPanel                actionPanel;
    private final JLabel                turnLabel;
    private final JPanel                carouselPanel;

    // Bottom-bar (construido una vez en buildBottomPanel)
    private JLabel              statusLabel;
    private JButton             endTurnBtn;
    private JWindow             tooltipWindow;
    private Character           currentActor;
    private boolean             actionsEnabled = true;

    // Party HUD
    private final List<JLabel>       hudNameLabels = new ArrayList<>();
    private final List<AnimatedBar>  hudHpBars     = new ArrayList<>();
    private JLabel                   hudPaLabel;

    // Selección de enemigo
    private Enemy        selectedEnemy  = null;
    private JPanel       targetOverlay;
    private JLabel       enemyNameLabel;
    private JLabel       enemyHpLabel;
    private AnimatedBar  enemyHpBar;

    // Targeting de aliado (pociones y habilidades)
    private enum TargetMode { NONE, WAITING_ALLY }
    private TargetMode targetMode    = TargetMode.NONE;
    private Potion     pendingPotion  = null;
    private com.dungeontales.core.model.Ability pendingAbility = null;

    // Timer para procesar eventos con delay visual
    private final List<BattleEvent> pendingEvents = new ArrayList<>();
    private Timer eventTimer;
    private final java.awt.image.BufferedImage background;

    public BattleScreen(BattleEngine engine, List<Character> party,
                        List<Enemy> enemies, Inventory inventory, String bgName, Listener listener) {
        this.engine   = engine;
        this.party    = party;
        this.enemies  = enemies;
        this.inventory = inventory;
        this.listener = listener;
        this.background = com.dungeontales.util.SpriteLoader.getBackground(bgName);

        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(0, 0));

        // ── Header: turno actual y carrusel ─ overlay transparente ──────────
        JPanel header = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 210),
                        0, getHeight(), new Color(0, 0, 0, 0)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 40, 16, 130)));
        turnLabel = new JLabel("TURNO 1");
        turnLabel.setFont(Theme.labelFont(14f));
        turnLabel.setForeground(Theme.TEXT_PRIMARY);
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setOpaque(false);
        titlePanel.add(turnLabel);

        header.add(titlePanel);

        // Panel para el carrusel
        carouselPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        carouselPanel.setOpaque(false);
        header.add(carouselPanel);

        add(header, BorderLayout.NORTH);

        // ── Centro: party vs enemigos ─ transparente, bg lo pinta BattleScreen ──
        // GridBagLayout sin weightx → el grupo completo (party+gap+enemigos) queda centrado
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        // bottomH=148, sin overlap → cards no tapan el panel inferior ni bloquean clicks
        centerPanel.setBorder(BorderFactory.createEmptyBorder(8, 30, 155, 30));

        // Constraint externo: ancla cada grupo al fondo, ocupa toda la altura disponible
        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.weighty = 1.0;
        cGbc.anchor  = GridBagConstraints.SOUTH;
        cGbc.fill    = GridBagConstraints.NONE;
        cGbc.weightx = 0;

        // Party — null-layout con solapamiento horizontal
        JPanel partyPanel = buildOverlapRow(45);
        for (Character c : party) {
            CharacterCard card = new CharacterCard(c);
            charCards.add(card);
            partyPanel.add(card);
        }

        // Escalar enemigos si el ancho total superaría la pantalla
        int screenW    = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
        // Ancho real del partyPanel con overlap de 45px
        int partyPxW   = CharacterCard.CARD_W + Math.max(0, party.size() - 1) * (CharacterCard.CARD_W - 45);
        int vsGapPx    = 100;
        int enemyAvail = Math.max(300, (int)(screenW * 0.97) - 60 - partyPxW - vsGapPx);
        int maxEnemyW  = Math.max(160, enemyAvail / enemies.size());
        // Enemies — null-layout con solapamiento horizontal
        JPanel enemyPanel = buildOverlapRow(35);
        for (Enemy e : enemies) {
            EnemyCard card = new EnemyCard(e, maxEnemyW);
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent ev) {
                    if (e.isAlive() && engine.isPlayerTurn()) selectEnemy(e);
                }
            });
            enemyCards.add(card);
            enemyPanel.add(card);
        }

        JPanel vsGap = new JPanel(); vsGap.setOpaque(false);
        vsGap.setPreferredSize(new Dimension(vsGapPx, 10));

        cGbc.gridx = 0; centerPanel.add(partyPanel, cGbc);
        cGbc.gridx = 1; centerPanel.add(vsGap,      cGbc);
        cGbc.insets = new java.awt.Insets(0, 0, 35, 0);
        cGbc.gridx = 2; centerPanel.add(enemyPanel,  cGbc);
        cGbc.insets = new java.awt.Insets(0, 0, 0, 0);

        // JLayeredPane: centerPanel (sprites) en capa alta → pinta SOBRE el bottom panel
        buildTargetOverlay();
        JPanel bottomPanel = buildBottomPanel();
        JLayeredPane battleLayer = new JLayeredPane() {
            @Override
            public void doLayout() {
                int w = getWidth(), h = getHeight();
                centerPanel.setBounds(0, 0, w, h);
                bottomPanel.setBounds(0, h - 148, w, 148);
                // Overlay de target: derecha, a la altura de los enemigos
                int ow = 220, oh = 115;
                int ox = w - ow - 14;
                int oy = (int)(h * 0.55) - oh / 2;
                targetOverlay.setBounds(ox, oy, ow, oh);
            }
        };
        battleLayer.setOpaque(false);
        battleLayer.add(bottomPanel,   JLayeredPane.DEFAULT_LAYER);
        battleLayer.add(centerPanel,   JLayeredPane.PALETTE_LAYER);
        battleLayer.add(targetOverlay, JLayeredPane.PALETTE_LAYER);
        add(battleLayer, BorderLayout.CENTER);

        // Listeners de click en charCards para targeting de aliados
        for (CharacterCard card : charCards) {
            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (targetMode == TargetMode.WAITING_ALLY)
                        onAllySelected(card.getCharacter());
                }
            });
        }

        // Timer de eventos: un evento cada 400ms para que se vean las animaciones
        eventTimer = new Timer(400, e -> processNextEvent());
        eventTimer.setRepeats(false);

        // Iniciar
        initBattle();
    }

    // ── Fondo a pantalla completa ───────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int w = getWidth(), h = getHeight();
        if (background != null) {
            int iw = background.getWidth(), ih = background.getHeight();
            double scale = Math.max((double) w / iw, (double) h / ih);
            int dw = (int)(iw * scale), dh = (int)(ih * scale);
            int dx = (w - dw) / 2, dy = (h - dh) / 2;
            g2.drawImage(background, dx, dy, dw, dh, null);
            // Overlay leve — sólo lo suficiente para que el texto sea legible
            g2.setColor(new Color(0, 0, 0, 55));
            g2.fillRect(0, 0, w, h);
        } else {
            g2.setColor(new Color(0x06, 0x08, 0x0E));
            g2.fillRect(0, 0, w, h);
            for (int r = Math.max(w, h); r > 0; r -= 8) {
                float t = 1f - (float) r / Math.max(w, h);
                int alpha = (int) (t * t * 28);
                g2.setColor(new Color(0x5A, 0x38, 0x18, Math.min(alpha, 25)));
                g2.fillOval(w / 2 - r, h / 2 - r, r * 2, r * 2);
            }
        }
        // Viñeta de bordes
        for (int i = 0; i < 40; i++) {
            int alpha = (int) ((float) i / 40 * 80);
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.drawRect(i, i, w - i * 2 - 1, h - i * 2 - 1);
        }
    }

    // ── Inicialización ──────────────────────────────────────────────────────

    private void initBattle() {
        updateCarousel();
        List<BattleEvent> startEvents = engine.getStartEvents();
        pendingEvents.addAll(startEvents);
        processNextEvent();
    }

    // ── Procesamiento de eventos ───────────────────────────────────────────

    private void processNextEvent() {
        if (pendingEvents.isEmpty()) {
            // Termina la cola — ver si es turno del jugador o del enemigo
            if (!engine.isBattleOver()) {
                if (engine.isPlayerTurn()) {
                    buildActionPanel();
                } else {
                    // Procesar turno de enemigos
                    List<BattleEvent> events = engine.processUntilPlayerTurn();
                    pendingEvents.addAll(events);
                    eventTimer.start();
                }
            }
            return;
        }

        BattleEvent event = pendingEvents.remove(0);
        handleEvent(event);

        if (!pendingEvents.isEmpty()) {
            int delay = switch (event.type) {
                case DAMAGE_DEALT, HEAL_APPLIED, POISON_TICK -> 350;
                case BATTLE_WON, BATTLE_LOST                 -> 800;
                default                                       -> 200;
            };
            eventTimer.setInitialDelay(delay);
            eventTimer.restart();
        } else {
            // Cola vacía, volver a revisar estado
            SwingUtilities.invokeLater(this::processNextEvent);
        }
    }

    private void handleEvent(BattleEvent event) {
        triggerAnimation(event);   // animaciones primero, antes del refresh
        refreshAllCards();

        switch (event.type) {
            case ENEMY_DIED -> {
                if (selectedEnemy != null && selectedEnemy.getName().equals(event.targetName)) {
                    selectedEnemy = null;
                    updateEnemyInfo();
                    if (engine.isPlayerTurn()) buildActionPanel();
                }
            }
            case TURN_START -> {
                updateCarousel();
                highlightActiveCharacter(event.actorName);
            }
            case BATTLE_WON -> SwingUtilities.invokeLater(() -> {
                int totalExp  = enemies.stream().mapToInt(Enemy::getExpReward).sum();
                int totalGold = enemies.stream().mapToInt(Enemy::getGoldReward).sum();
                List<String> levelUps = new ArrayList<>();
                party.stream().filter(Character::isAlive)
                     .forEach(c -> {
                         if (c.gainExp(totalExp / Math.max(1, (int) party.stream().filter(Character::isAlive).count()))) {
                             levelUps.add(c.getName() + " -> Nivel " + c.getLevel());
                         }
                     });
                listener.onBattleWon(totalExp, totalGold, null, levelUps);
            });
            case LEVEL_UP -> {
                // Ya manejado arriba
            }
            case BATTLE_LOST -> SwingUtilities.invokeLater(listener::onBattleGameOver);
            default -> {}
        }
    }

    private void updateCarousel() {
        turnLabel.setText("TURNO " + engine.getTurnNumber());

        carouselPanel.removeAll();
        List<Object> order = engine.getTurnOrder();
        int idx = engine.getTurnIndex();
        
        for (int i = 0; i < order.size(); i++) {
            Object combatant = order.get(i);
            boolean isDead = (combatant instanceof Character c && !c.isAlive()) 
                          || (combatant instanceof Enemy e && !e.isAlive());
            if (isDead) continue;
            
            String name = combatant instanceof Character c ? c.getName() : ((Enemy) combatant).getName();
            String spriteName = combatant instanceof Character c ? c.getSpriteName() : ((Enemy) combatant).getSpriteName();
            
            JLabel avatar = new JLabel();
            java.awt.image.BufferedImage img = combatant instanceof Character ?
                SpriteLoader.getCharacterSprite(spriteName, 40, 40) :
                SpriteLoader.getEnemySprite(spriteName, 40, 40);
            
            if (img != null) avatar.setIcon(new javax.swing.ImageIcon(img));
            
            if (i == idx) {
                avatar.setBorder(BorderFactory.createLineBorder(Theme.BORDER_ACTIVE, 2));
            } else {
                avatar.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
            }
            
            avatar.setToolTipText(name);
            carouselPanel.add(avatar);
            
            if (i < order.size() - 1) {
                JLabel arrow = new JLabel(" \u2794 ");
                arrow.setForeground(Theme.TEXT_MUTED);
                carouselPanel.add(arrow);
            }
        }
        carouselPanel.revalidate();
        carouselPanel.repaint();
    }

    // ── Overlay flotante de enemigo seleccionado ───────────────────────────

    private void buildTargetOverlay() {
        enemyNameLabel = new JLabel(" ");
        enemyNameLabel.setFont(Theme.labelFont(17f));
        enemyNameLabel.setForeground(Theme.GOLD_COLOR);
        enemyNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        enemyNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        enemyHpBar = new AnimatedBar(AnimatedBar.BarType.HP, 1, 0);
        enemyHpBar.setPreferredSize(new Dimension(190, 13));
        enemyHpBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 13));
        enemyHpBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        enemyHpLabel = new JLabel(" ");
        enemyHpLabel.setFont(Theme.labelFont(13f));
        enemyHpLabel.setForeground(new Color(0xE8, 0x80, 0x50));
        enemyHpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        enemyHpLabel.setHorizontalAlignment(SwingConstants.CENTER);

        targetOverlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(new Color(0x80, 0x40, 0x18, 220));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override
            public boolean contains(int x, int y) { return false; } // no bloquea clicks
        };
        targetOverlay.setOpaque(false);
        targetOverlay.setLayout(new BoxLayout(targetOverlay, BoxLayout.Y_AXIS));
        targetOverlay.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        targetOverlay.add(enemyNameLabel);
        targetOverlay.add(Box.createVerticalStrut(8));
        targetOverlay.add(enemyHpBar);
        targetOverlay.add(Box.createVerticalStrut(5));
        targetOverlay.add(enemyHpLabel);
        targetOverlay.setVisible(false);
    }

    // ── Bottom bar ─────────────────────────────────────────────────────────

    private JPanel buildBottomPanel() {
        // ACTION PANEL (CENTER – reconstruido cada turno)
        actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));

        // LEFT: Party HUD — panel con fondo semitransparente
        JPanel left = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 110));
                g2.fillRoundRect(6, 4, getWidth() - 10, getHeight() - 8, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(295, 0));
        left.setBorder(BorderFactory.createEmptyBorder(8, 16, 6, 12));

        left.add(Box.createVerticalGlue());
        for (Character c : party) {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

            JLabel nameLabel = new JLabel(c.getName());
            nameLabel.setFont(Theme.labelFont(14f));
            nameLabel.setForeground(Theme.TEXT_PRIMARY);
            nameLabel.setPreferredSize(new Dimension(80, 20));
            hudNameLabels.add(nameLabel);

            AnimatedBar bar = new AnimatedBar(AnimatedBar.BarType.HP, c.getHpMax(), c.getHp());
            bar.setPreferredSize(new Dimension(150, 15));
            hudHpBars.add(bar);

            row.add(nameLabel, BorderLayout.WEST);
            row.add(bar,       BorderLayout.CENTER);
            left.add(row);
            left.add(Box.createVerticalStrut(6));
        }

        hudPaLabel = new JLabel(" ");
        hudPaLabel.setFont(Theme.bodyFont(11f));
        hudPaLabel.setForeground(new Color(0x50, 0xC0, 0xE8));
        hudPaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(Box.createVerticalStrut(2));
        left.add(hudPaLabel);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.bodyFont(11f));
        statusLabel.setForeground(Theme.STUN_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(statusLabel);
        left.add(Box.createVerticalGlue());

        // RIGHT: solo botón terminar turno
        endTurnBtn = new JButton("↷  Terminar turno");
        Theme.styleEndTurnButton(endTurnBtn);
        endTurnBtn.setPreferredSize(new Dimension(160, 50));
        endTurnBtn.addActionListener(e -> { if (actionsEnabled) executeAction(engine.playerEndTurn()); });

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(200, 0));
        right.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 16));
        right.add(endTurnBtn);

        JPanel bottom = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Fondo oscuro sólido (~60% opacidad) + gradiente hacia abajo
                g2.setColor(new Color(0, 0, 0, 155));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 0),
                        0, getHeight(), new Color(3, 1, 0, 70)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Línea divisoria dorada en el borde superior
                g2.setColor(Theme.BORDER_ACTIVE);
                g2.fillRect(0, 0, getWidth(), 2);
                g2.dispose();
            }
        };
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        bottom.setPreferredSize(new Dimension(0, 148));
        bottom.add(left,        BorderLayout.WEST);
        bottom.add(actionPanel, BorderLayout.CENTER);
        bottom.add(right,       BorderLayout.EAST);
        return bottom;
    }

    // ── Panel de acciones del jugador ──────────────────────────────────────

    private void buildActionPanel() {
        hideTooltip();
        actionsEnabled = true;
        actionPanel.removeAll();
        Object current = engine.getCurrentCombatant();
        if (!(current instanceof Character actor)) return;
        currentActor = actor;
        highlightActiveCharacter(actor.getName());
        endTurnBtn.setEnabled(true);
        refreshHUD();
        statusLabel.setText(" ");

        // Pestañas
        JButton skillsTab = new JButton("⚔  Habilidades");
        JButton itemsTab  = new JButton("o  Consumibles");
        styleTabBtn(skillsTab, true);
        styleTabBtn(itemsTab,  false);

        String hint = selectedEnemy != null
            ? "  → " + selectedEnemy.getName()
            : "  ↑ Selecciona un enemigo";
        JLabel hintLabel = new JLabel(hint);
        hintLabel.setFont(Theme.bodyFont(10f));
        hintLabel.setForeground(selectedEnemy != null ? Theme.GOLD_COLOR : Theme.TEXT_MUTED);

        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabRow.setOpaque(false);
        tabRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        tabRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabRow.add(skillsTab);
        tabRow.add(itemsTab);
        tabRow.add(hintLabel);

        CardLayout cl = new CardLayout();
        JPanel barContainer = new JPanel(cl);
        barContainer.setOpaque(false);
        barContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        barContainer.add(buildSkillBar(actor), "skills");
        barContainer.add(buildConsumableBar(),  "items");

        skillsTab.addActionListener(e -> { styleTabBtn(skillsTab, true);  styleTabBtn(itemsTab,  false); cl.show(barContainer, "skills"); });
        itemsTab.addActionListener(e  -> { styleTabBtn(skillsTab, false); styleTabBtn(itemsTab,  true);  cl.show(barContainer, "items"); });

        actionPanel.add(tabRow);
        actionPanel.add(Box.createVerticalStrut(4));
        actionPanel.add(barContainer);
        actionPanel.revalidate();
        actionPanel.repaint();
    }

    private JPanel buildSkillBar(Character actor) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);

        BufferedImage atkImg = SpriteLoader.getAttackSprite(78);
        bar.add(buildAbilityIcon(atkImg, "AT", "Ataque", 0, "Ataque basico. No consume PA.",
            new Color(0x50, 0x28, 0x10), true,
            () -> {
                if (selectedEnemy == null) { showStatus("Selecciona un enemigo.", Theme.STUN_COLOR); return; }
                executeAction(engine.playerBasicAttack(selectedEnemy));
            }
        ));

        for (Ability ab : actor.getAbilities()) {
            boolean canUse = actor.getPa() >= ab.getPaCost();
            boolean needsEnemy = ab.getTargetType() == Ability.TargetType.SINGLE_ENEMY;
            boolean iconEnabled = canUse && (!needsEnemy || selectedEnemy != null);
            BufferedImage skillImg = SpriteLoader.getSkillSprite(actor.getSpriteName(), ab.getName(), 78);
            bar.add(buildAbilityIcon(
                skillImg, abilitySymbol(ab), ab.getName(), ab.getPaCost(),
                ab.getDescription() != null ? ab.getDescription() : ab.getName(),
                abilityColor(ab, canUse), iconEnabled,
                () -> {
                    if (!canUse) { showStatus("PA insuficientes.", Theme.STUN_COLOR); return; }
                    if (needsEnemy && selectedEnemy == null) { showStatus("Selecciona un enemigo.", Theme.STUN_COLOR); return; }
                    if (ab.getTargetType() == Ability.TargetType.SINGLE_ALLY) { enterAllyTargetingForAbility(ab); return; }
                    executeAction(engine.playerUseAbility(ab, selectedEnemy, null));
                }
            ));
        }
        return bar;
    }

    private JPanel buildConsumableBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);

        Map<Potion.Effect, Integer> counts = new LinkedHashMap<>();
        Map<Potion.Effect, Potion>  reps   = new LinkedHashMap<>();
        for (Potion p : inventory.getPotions()) {
            counts.merge(p.getEffect(), 1, Integer::sum);
            reps.putIfAbsent(p.getEffect(), p);
        }

        if (counts.isEmpty()) {
            JLabel lbl = new JLabel("Sin consumibles en la mochila.");
            lbl.setFont(Theme.bodyFont(11f));
            lbl.setForeground(Theme.TEXT_MUTED);
            bar.add(lbl);
        } else {
            for (Potion.Effect fx : counts.keySet())
                bar.add(buildPotionIcon(reps.get(fx), counts.get(fx)));
        }
        return bar;
    }

    // ── Icono de habilidad ──────────────────────────────────────────────────

    private JPanel buildAbilityIcon(BufferedImage img, String symbol, String name, int paCost, String desc,
                                    Color baseColor, boolean enabled, Runnable action) {
        boolean[] hovered = {false};
        JPanel icon = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Fondo
                Color bg = !enabled
                    ? new Color(baseColor.getRed()/3, baseColor.getGreen()/3, baseColor.getBlue()/3)
                    : hovered[0] ? baseColor.brighter() : baseColor;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w-1, h-1, 10, 10);
                // Borde
                g2.setColor(hovered[0] && enabled ? new Color(0xC8, 0x92, 0x28) : new Color(0x40, 0x30, 0x20));
                g2.setStroke(new BasicStroke(hovered[0] && enabled ? 2f : 1.5f));
                g2.drawRoundRect(0, 0, w-2, h-2, 10, 10);
                if (img != null) {
                    int pad = 5;
                    int ih = h - pad * 2 - (paCost > 0 ? 10 : 0);
                    Composite prev = g2.getComposite();
                    if (!enabled)
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                    g2.drawImage(img, pad, pad, w - pad * 2, ih, null);
                    g2.setComposite(prev);
                    if (hovered[0] && enabled) {
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
                        g2.setColor(Color.WHITE);
                        g2.fillRoundRect(1, 1, w-3, h-3, 9, 9);
                        g2.setComposite(prev);
                    }
                } else {
                    // Fallback: símbolo de texto
                    g2.setFont(Theme.labelFont(18f).deriveFont(Font.BOLD));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.setColor(enabled ? Color.WHITE : new Color(255, 255, 255, 80));
                    String s = symbol.length() > 3 ? symbol.substring(0, 3) : symbol;
                    g2.drawString(s, (w - fm.stringWidth(s)) / 2, h / 2 + fm.getAscent() / 2 - 4);
                }
                // Badge de PA (abajo derecha)
                if (paCost > 0) {
                    g2.setColor(new Color(0x08, 0x06, 0x04, 210));
                    g2.fillRoundRect(w-22, h-17, 20, 13, 4, 4);
                    g2.setColor(enabled ? new Color(0x50, 0xC0, 0xE8) : new Color(0x28, 0x60, 0x70));
                    g2.setFont(Theme.bodyFont(9f));
                    String pa = String.valueOf(paCost);
                    g2.drawString(pa, w - 20 + (2 - pa.length()) * 3, h - 7);
                }
                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(88, 88));
        icon.setOpaque(false);
        if (enabled) icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        icon.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered[0] = true;  icon.repaint(); showTooltip(icon, name, paCost, desc); }
            @Override public void mouseExited(MouseEvent e)  { hovered[0] = false; icon.repaint(); hideTooltip(); }
            @Override public void mouseClicked(MouseEvent e) { if (enabled && actionsEnabled) { hideTooltip(); action.run(); } }
        });
        return icon;
    }

    private JPanel buildPotionIcon(Potion potion, int count) {
        String sym = switch (potion.getEffect()) {
            case HEAL_SMALL, HEAL_LARGE -> "HP";
            case PA_RESTORE -> "PA";
            case ANTIDOTE   -> "ANT";
            case STRENGTH   -> "STR";
            case REVIVE     -> "REV";
        };
        Color col = switch (potion.getEffect()) {
            case HEAL_SMALL, HEAL_LARGE -> new Color(0x12, 0x4A, 0x20);
            case PA_RESTORE             -> new Color(0x10, 0x28, 0x5A);
            case ANTIDOTE               -> new Color(0x18, 0x42, 0x18);
            case STRENGTH               -> new Color(0x5A, 0x28, 0x08);
            case REVIVE                 -> new Color(0x3A, 0x14, 0x5A);
        };
        boolean[] hovered = {false};
        String desc = potion.getDescription() != null ? potion.getDescription() : "";
        JPanel icon = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(hovered[0] ? col.brighter() : col);
                g2.fillRoundRect(0, 0, w-1, h-1, 10, 10);
                g2.setColor(hovered[0] ? new Color(0xC8, 0x92, 0x28) : new Color(0x40, 0x30, 0x20));
                g2.setStroke(new BasicStroke(hovered[0] ? 2f : 1.5f));
                g2.drawRoundRect(0, 0, w-2, h-2, 10, 10);
                g2.setFont(Theme.labelFont(18f).deriveFont(Font.BOLD));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Color.WHITE);
                g2.drawString(sym, (w - fm.stringWidth(sym)) / 2, h / 2 + fm.getAscent() / 2 - 4);
                // Badge de cantidad (arriba derecha)
                g2.setColor(new Color(0x08, 0x06, 0x04, 210));
                g2.fillRoundRect(w-24, 4, 20, 14, 4, 4);
                g2.setColor(new Color(0xC8, 0x92, 0x28));
                g2.setFont(Theme.bodyFont(9f));
                g2.drawString("x" + count, w - 22, 14);
                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(88, 88));
        icon.setOpaque(false);
        icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        icon.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered[0] = true;  icon.repaint(); showTooltip(icon, potion.getName() + "  x" + count, 0, desc); }
            @Override public void mouseExited(MouseEvent e)  { hovered[0] = false; icon.repaint(); hideTooltip(); }
            @Override public void mouseClicked(MouseEvent e) { if (actionsEnabled) { hideTooltip(); enterAllyTargeting(potion); } }
        });
        return icon;
    }

    // ── Helpers de estilo e iconos ──────────────────────────────────────────

    private void styleTabBtn(JButton btn, boolean active) {
        btn.setFont(Theme.bodyFont(10f));
        btn.setForeground(active ? new Color(0xC8, 0x92, 0x28) : Theme.TEXT_MUTED);
        btn.setBackground(active ? new Color(0x28, 0x1A, 0x08) : new Color(0x10, 0x0C, 0x08));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, new Color(0xC8, 0x92, 0x28)),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
    }

    private String abilitySymbol(Ability ab) {
        if (ab.isHeal()) return "HP";
        String[] words = ab.getName().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) { sb.append(java.lang.Character.toUpperCase(w.charAt(0))); if (sb.length() == 2) break; }
        }
        return sb.length() > 0 ? sb.toString()
            : ab.getName().substring(0, Math.min(2, ab.getName().length())).toUpperCase();
    }

    private Color abilityColor(Ability ab, boolean canUse) {
        if (!canUse) return new Color(0x18, 0x14, 0x10);
        if (ab.isHeal()) return new Color(0x10, 0x46, 0x1A);
        return switch (ab.getTargetType()) {
            case ALL_ENEMIES -> new Color(0x5A, 0x18, 0x08);
            case SELF        -> new Color(0x10, 0x24, 0x56);
            case ALL_ALLIES  -> new Color(0x30, 0x20, 0x56);
            default -> ab.isIgnoreDefense() ? new Color(0x48, 0x14, 0x2C) : new Color(0x40, 0x20, 0x08);
        };
    }

    // ── Tooltip personalizado ───────────────────────────────────────────────

    private void showTooltip(Component anchor, String name, int paCost, String desc) {
        hideTooltip();
        Window parent = SwingUtilities.getWindowAncestor(this);
        if (parent == null) return;
        JWindow w = new JWindow(parent);
        JPanel content = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1A, 0x10, 0x08), 0, getHeight(), new Color(0x0C, 0x06, 0x04)));
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setColor(new Color(0x60, 0x40, 0x18));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-2, getHeight()-2, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(Theme.labelFont(13f).deriveFont(Font.BOLD));
        nameLabel.setForeground(Color.WHITE);
        JLabel paLabel = new JLabel(paCost == 0 ? "Gratis" : "PA: " + paCost);
        paLabel.setFont(Theme.bodyFont(10f));
        paLabel.setForeground(new Color(0x50, 0xC0, 0xE8));
        content.add(nameLabel);
        content.add(Box.createVerticalStrut(3));
        content.add(paLabel);
        if (desc != null && !desc.isBlank()) {
            JLabel descLabel = new JLabel("<html><body style='width:200px'>" + desc + "</body></html>");
            descLabel.setFont(Theme.bodyFont(10f));
            descLabel.setForeground(new Color(0xA0, 0x90, 0x70));
            content.add(Box.createVerticalStrut(5));
            content.add(descLabel);
        }
        w.add(content);
        w.pack();
        try {
            Point loc = anchor.getLocationOnScreen();
            int x = loc.x + anchor.getWidth() / 2 - w.getWidth() / 2;
            int y = loc.y - w.getHeight() - 6;
            w.setLocation(Math.max(0, x), Math.max(0, y));
        } catch (Exception ignored) { return; }
        w.setVisible(true);
        tooltipWindow = w;
    }

    private void hideTooltip() {
        if (tooltipWindow != null) { tooltipWindow.dispose(); tooltipWindow = null; }
    }

    private void showStatus(String msg, Color color) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        Timer t = new Timer(2500, e -> { if (statusLabel != null) statusLabel.setText(" "); });
        t.setRepeats(false);
        t.start();
    }

    // ── Targeting de aliados ───────────────────────────────────────────────

    private void enterAllyTargeting(Potion potion) {
        pendingPotion = potion;
        targetMode    = TargetMode.WAITING_ALLY;
        refreshTargetableCards();

        actionPanel.removeAll();
        JPanel prompt = new JPanel();
        prompt.setOpaque(false);
        prompt.setLayout(new BoxLayout(prompt, BoxLayout.Y_AXIS));

        JLabel msg = new JLabel("→  " + potion.getName() + ": elegí un aliado");
        msg.setFont(Theme.labelFont(12f));
        msg.setForeground(new Color(0x50, 0xE0, 0x80));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton cancel = new JButton("Cancelar");
        Theme.styleButton(cancel);
        cancel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancel.addActionListener(e -> { exitTargeting(); buildActionPanel(); });

        prompt.add(Box.createVerticalGlue());
        prompt.add(msg);
        prompt.add(Box.createVerticalStrut(6));
        prompt.add(cancel);
        prompt.add(Box.createVerticalGlue());
        actionPanel.add(prompt);
        actionPanel.revalidate();
        actionPanel.repaint();
    }

    private void enterAllyTargetingForAbility(Ability ab) {
        pendingAbility = ab;
        targetMode     = TargetMode.WAITING_ALLY;
        charCards.forEach(c -> c.setTargetable(c.getCharacter().isAlive()));

        actionPanel.removeAll();
        JPanel prompt = new JPanel();
        prompt.setOpaque(false);
        prompt.setLayout(new BoxLayout(prompt, BoxLayout.Y_AXIS));

        JLabel msg = new JLabel("→  " + ab.getName() + ": elegí un aliado");
        msg.setFont(Theme.labelFont(12f));
        msg.setForeground(new Color(0x50, 0xD0, 0xA0));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton cancel = new JButton("Cancelar");
        Theme.styleButton(cancel);
        cancel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancel.addActionListener(e -> { exitTargeting(); buildActionPanel(); });

        prompt.add(Box.createVerticalGlue());
        prompt.add(msg);
        prompt.add(Box.createVerticalStrut(6));
        prompt.add(cancel);
        prompt.add(Box.createVerticalGlue());
        actionPanel.add(prompt);
        actionPanel.revalidate();
        actionPanel.repaint();
    }

    private void exitTargeting() {
        targetMode     = TargetMode.NONE;
        pendingPotion  = null;
        pendingAbility = null;
        charCards.forEach(c -> c.setTargetable(false));
    }

    private void refreshTargetableCards() {
        boolean needsDead = pendingPotion != null
            && pendingPotion.getEffect() == Potion.Effect.REVIVE;
        charCards.forEach(c -> {
            boolean valid = needsDead ? !c.getCharacter().isAlive()
                                      :  c.getCharacter().isAlive();
            c.setTargetable(valid);
        });
    }

    private void onAllySelected(Character target) {
        if (targetMode != TargetMode.WAITING_ALLY) return;

        if (pendingPotion != null) {
            boolean needsDead = pendingPotion.getEffect() == Potion.Effect.REVIVE;
            if (needsDead && target.isAlive()) {
                showStatus("El Elixir solo puede usarse en un aliado caído.", Theme.STUN_COLOR);
                return;
            }
            if (!needsDead && !target.isAlive()) {
                showStatus("Este ítem solo puede usarse en un aliado vivo.", Theme.STUN_COLOR);
                return;
            }
            Potion pot = pendingPotion;
            exitTargeting();
            inventory.removeItem(pot);
            executeAction(engine.playerUsePotion(pot, target));

        } else if (pendingAbility != null) {
            if (!target.isAlive()) {
                showStatus("El objetivo debe estar vivo.", Theme.STUN_COLOR);
                return;
            }
            Ability ab = pendingAbility;
            exitTargeting();
            executeAction(engine.playerUseAbility(ab, selectedEnemy, target));
        }
    }

    private void executeAction(List<BattleEvent> events) {
        setActionsEnabled(false);
        pendingEvents.addAll(events);
        processNextEvent();
    }

    private void setActionsEnabled(boolean enabled) {
        actionsEnabled = enabled;
        if (endTurnBtn != null) endTurnBtn.setEnabled(enabled);
    }

    // ── Selección de enemigo ───────────────────────────────────────────────

    private void selectEnemy(Enemy e) {
        selectedEnemy = e;
        enemyCards.forEach(c -> c.setTargeted(c.getEnemy() == e));
        updateEnemyInfo();
        buildActionPanel();
    }

    private void updateEnemyInfo() {
        if (targetOverlay == null) return;
        if (selectedEnemy == null) {
            targetOverlay.setVisible(false);
        } else {
            enemyNameLabel.setText(selectedEnemy.getName());
            enemyHpLabel.setText(selectedEnemy.getHp() + " / " + selectedEnemy.getHpMax() + " HP");
            enemyHpBar.setValue(selectedEnemy.getHp(), selectedEnemy.getHpMax());
            targetOverlay.setVisible(true);
        }
    }

    private void promptSelectEnemy() {
        showStatus("Seleccioná un objetivo haciendo click en un enemigo.", Theme.STUN_COLOR);
    }

    // ── Animaciones de impacto ─────────────────────────────────────────────

    private void triggerAnimation(BattleEvent event) {
        switch (event.type) {
            case DAMAGE_DEALT -> {
                // Actor attacks
                CharacterCard actor = findCharCard(event.actorName);
                if (actor != null) actor.playAttack();
                // Target receives hit
                CharacterCard target = findCharCard(event.targetName);
                if (target != null) { target.playHit(); target.flashDamage(event.value); }
                else {
                    EnemyCard ec = findEnemyCard(event.targetName);
                    if (ec != null) ec.flashDamage(event.value);
                }
            }
            case HEAL_APPLIED, REGEN_TICK -> {
                CharacterCard actor = findCharCard(event.actorName);
                if (actor != null) actor.playSkill();
                CharacterCard target = findCharCard(event.targetName);
                if (target != null) target.flashHeal(event.value);
                else {
                    EnemyCard ec = findEnemyCard(event.targetName);
                    if (ec != null) ec.flashHeal(event.value);
                }
            }
            case POISON_TICK -> {
                CharacterCard cc = findCharCard(event.targetName);
                if (cc != null) { cc.playHit(); cc.flashDamage(event.value); }
            }
            case DAMAGE_DODGED -> {
                CharacterCard cc = findCharCard(event.targetName);
                if (cc != null) cc.flashDodge();
                else {
                    EnemyCard ec = findEnemyCard(event.targetName);
                    if (ec != null) ec.flashDodge();
                }
            }
            case STATUS_APPLIED -> {
                CharacterCard actor = findCharCard(event.actorName);
                if (actor != null) actor.playSkill();
                CharacterCard target = findCharCard(event.targetName);
                if (target != null) target.flashStatus("✦");
            }
            case CHARACTER_DIED -> {
                CharacterCard cc = findCharCard(event.targetName);
                if (cc != null) cc.playDeath();
            }
            default -> {}
        }
    }

    private CharacterCard findCharCard(String name) {
        if (name == null) return null;
        return charCards.stream()
            .filter(c -> c.getCharacterName().equals(name))
            .findFirst().orElse(null);
    }

    private EnemyCard findEnemyCard(String name) {
        if (name == null) return null;
        return enemyCards.stream()
            .filter(c -> c.getEnemy().getName().equals(name))
            .findFirst().orElse(null);
    }

    // ── Refresh visual ─────────────────────────────────────────────────────

    private void refreshAllCards() {
        charCards.forEach(CharacterCard::refresh);
        enemyCards.forEach(EnemyCard::refresh);
        updateEnemyInfo();
        refreshHUD();
    }

    private void refreshHUD() {
        for (int i = 0; i < party.size() && i < hudHpBars.size(); i++) {
            Character c = party.get(i);
            hudHpBars.get(i).setValue(c.getHp(), c.getHpMax());
            JLabel lbl = hudNameLabels.get(i);
            boolean isActive = currentActor != null && currentActor == c;
            boolean isDead   = !c.isAlive();
            lbl.setForeground(isDead   ? Theme.TEXT_MUTED
                            : isActive ? Theme.GOLD_COLOR
                                       : Theme.TEXT_SECONDARY);
        }
        if (hudPaLabel != null && currentActor != null)
            hudPaLabel.setText("PA: " + currentActor.getPa() + "/" + currentActor.getPaMax());
    }

    private void highlightActiveCharacter(String name) {
        charCards.forEach(c -> c.setActive(c.getCharacterName().equals(name)));
    }

    // ── Solapamiento horizontal (null-layout con bottom-anchor) ────────────
    // getPreferredSize() correcto → el GridBagLayout externo reserva el espacio justo.
    // doLayout() ancla cada carta al fondo del panel independientemente de su alto.
    // Z-order: index 0 = más alta (pintada encima), index n-1 = más baja (detrás).
    // Al agregar de izquierda a derecha: carta izquierda (frente de batalla) queda encima.

    private static JPanel buildOverlapRow(int overlap) {
        return new JPanel(null) {
            { setOpaque(false); }

            @Override
            public Dimension getPreferredSize() {
                Component[] cs = getComponents();
                if (cs.length == 0) return new Dimension(0, 0);
                int maxH = 0, totalW = 0;
                for (int i = 0; i < cs.length; i++) {
                    Dimension ps = cs[i].getPreferredSize();
                    maxH   = Math.max(maxH, ps.height);
                    totalW += (i < cs.length - 1) ? (ps.width - overlap) : ps.width;
                }
                return new Dimension(totalW, maxH);
            }

            @Override
            public void doLayout() {
                int panelH = getHeight();
                int x = 0;
                // Iterar en orden inverso (index n-1 primero) para posicionar izq→der
                // sin que el orden de iteración afecte las posiciones X
                Component[] cs = getComponents();
                for (int i = cs.length - 1; i >= 0; i--) {
                    Dimension ps = cs[i].getPreferredSize();
                    // Anclaje estricto al fondo: y = panelH - cardH
                    cs[i].setBounds(x, panelH - ps.height, ps.width, ps.height);
                    x += ps.width - overlap;
                }
            }
        };
    }
}
