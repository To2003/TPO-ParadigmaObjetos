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
    private final BattleLog             log;
    private final JPanel                actionPanel;
    private final JLabel                turnLabel;
    private final JPanel                carouselPanel;

    // Selección de enemigo
    private Enemy selectedEnemy = null;

    // Targeting de aliado (pociones)
    private enum TargetMode { NONE, WAITING_ALLY }
    private TargetMode targetMode   = TargetMode.NONE;
    private Potion     pendingPotion = null;

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

        // ── Header: turno actual y carrusel ───────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.BG_PANEL);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR));
        turnLabel = new JLabel("TURNO 1");
        turnLabel.setFont(Theme.labelFont(14f));
        turnLabel.setForeground(Theme.TEXT_PRIMARY);
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setBackground(Theme.BG_PANEL);
        titlePanel.add(turnLabel);
        
        header.add(titlePanel);

        // Panel para el carrusel
        carouselPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        carouselPanel.setBackground(Theme.BG_PANEL);
        header.add(carouselPanel);

        add(header, BorderLayout.NORTH);

        // ── Centro: party vs enemigos ─────────────────────────────────────
        JPanel centerPanel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                int w = getWidth(), h = getHeight();
                if (background != null) {
                    int iw = background.getWidth(), ih = background.getHeight();
                    double scale = Math.max((double) w / iw, (double) h / ih);
                    int dw = (int)(iw * scale), dh = (int)(ih * scale);
                    int dx = (w - dw) / 2, dy = (h - dh) / 2;
                    g2.drawImage(background, dx, dy, dw, dh, null);
                    // Dark overlay for readability
                    g2.setColor(new Color(0, 0, 0, 110));
                    g2.fillRect(0, 0, w, h);
                } else {
                    // Fondo atmosférico: gradiente radial simulado con capas
                    g2.setColor(new Color(0x06, 0x08, 0x0E));
                    g2.fillRect(0, 0, w, h);
                    // Capa central más clara (iluminación de antorcha)
                    for (int r = Math.max(w, h); r > 0; r -= 8) {
                        float t = 1f - (float)r / Math.max(w, h);
                        int alpha = (int)(t * t * 28);
                        g2.setColor(new Color(0x5A, 0x38, 0x18, Math.min(alpha, 25)));
                        g2.fillOval(w/2 - r, h/2 - r, r*2, r*2);
                    }
                }
                // Viñeta oscura en bordes
                for (int i = 0; i < 40; i++) {
                    int alpha = (int)((float)i / 40 * 80);
                    g2.setColor(new Color(0, 0, 0, alpha));
                    g2.drawRect(i, i, w - i*2 - 1, h - i*2 - 1);
                }
            }
        };
        centerPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;

        // Party
        JPanel partyPanel = new JPanel(new GridBagLayout());
        partyPanel.setOpaque(false);
        GridBagConstraints pGbc = new GridBagConstraints();
        pGbc.anchor = GridBagConstraints.SOUTH;
        pGbc.insets = new Insets(0, 5, 0, 5);
        for (Character c : party) {
            CharacterCard card = new CharacterCard(c);
            charCards.add(card);
            partyPanel.add(card, pGbc);
        }

        // Espaciador central (Reemplaza al emblema VS)
        JPanel vsLabel = new JPanel();
        vsLabel.setOpaque(false);
        vsLabel.setPreferredSize(new Dimension(100, 10));

        // Enemigos
        JPanel enemyPanel = new JPanel(new GridBagLayout());
        enemyPanel.setOpaque(false);
        GridBagConstraints eGbc = new GridBagConstraints();
        eGbc.anchor = GridBagConstraints.SOUTH;
        eGbc.insets = new Insets(0, 5, 0, 5);
        for (Enemy e : enemies) {
            EnemyCard card = new EnemyCard(e);
            // Click para seleccionar objetivo
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent ev) {
                    if (e.isAlive() && engine.isPlayerTurn()) selectEnemy(e);
                }
            });
            enemyCards.add(card);
            enemyPanel.add(card, eGbc);
        }

        gbc.gridx = 0; centerPanel.add(partyPanel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.1; centerPanel.add(vsLabel, gbc);
        gbc.gridx = 2; gbc.weightx = 1.0; centerPanel.add(enemyPanel, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // ── Log de combate ────────────────────────────────────────────────
        log = new BattleLog();
        add(log, BorderLayout.SOUTH);

        // ── Panel de acciones ─────────────────────────────────────────────
        actionPanel = new JPanel();
        actionPanel.setBackground(new Color(0x0C, 0x08, 0x06));
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, Theme.BORDER_ACTIVE),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        // Insertar entre log y borde sur
        JPanel southStack = new JPanel(new BorderLayout());
        southStack.setBackground(Theme.BG_DARK);
        southStack.add(actionPanel, BorderLayout.NORTH);
        southStack.add(log, BorderLayout.CENTER);
        remove(log);
        add(southStack, BorderLayout.SOUTH);

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
        log.addEvent(event);
        triggerAnimation(event);   // animaciones primero, antes del refresh
        refreshAllCards();

        switch (event.type) {
            case ENEMY_DIED -> {
                if (selectedEnemy != null && selectedEnemy.getName().equals(event.targetName)) {
                    selectedEnemy = null;
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

    // ── Panel de acciones del jugador ──────────────────────────────────────

    private void buildActionPanel() {
        actionPanel.removeAll();
        Object current = engine.getCurrentCombatant();
        if (!(current instanceof Character actor)) return;
        highlightActiveCharacter(actor.getName());

        // Info row
        JPanel infoRow = new JPanel(new BorderLayout());
        infoRow.setOpaque(false);
        infoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        JLabel info = new JLabel("  Turno de " + actor.getName()
            + "   PA: " + actor.getPa() + "/" + actor.getPaMax());
        info.setFont(Theme.labelFont(11f));
        info.setForeground(Theme.TEXT_PRIMARY);
        infoRow.add(info, BorderLayout.WEST);
        String tTxt = selectedEnemy != null
            ? "Objetivo: " + selectedEnemy.getName() + "   "
            : "↑  Seleccioná un enemigo   ";
        JLabel tLbl = new JLabel(tTxt);
        tLbl.setFont(Theme.bodyFont(11f));
        tLbl.setForeground(selectedEnemy != null ? Theme.GOLD_COLOR : Theme.TEXT_MUTED);
        infoRow.add(tLbl, BorderLayout.EAST);
        actionPanel.add(infoRow);
        actionPanel.add(Box.createVerticalStrut(4));

        // 3-zone button row
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;

        gbc.gridx = 0; gbc.weightx = 0.56; gbc.insets = new Insets(0, 0, 0, 6);
        row.add(buildSkillsZone(actor), gbc);
        gbc.gridx = 1; gbc.weightx = 0.30; gbc.insets = new Insets(0, 0, 0, 6);
        row.add(buildConsumablesZone(), gbc);
        gbc.gridx = 2; gbc.weightx = 0.14; gbc.insets = new Insets(0, 0, 0, 0);
        row.add(buildEndTurnZone(), gbc);

        actionPanel.add(row);
        actionPanel.revalidate();
        actionPanel.repaint();
    }

    /** Zona izquierda: ataque básico + habilidades en grilla de 3 columnas. */
    private JPanel buildSkillsZone(Character actor) {
        // Contar botones para calcular filas
        int btnCount = 1 + actor.getAbilities().size(); // ataque + habilidades
        int cols = 3;
        JPanel zone = new JPanel(new GridLayout(0, cols, 4, 3));
        zone.setOpaque(false);

        JButton basicBtn = new JButton("⚔  Ataque  [0 PA]");
        Theme.styleAttackButton(basicBtn);
        basicBtn.setEnabled(selectedEnemy != null);
        basicBtn.addActionListener(e -> {
            if (selectedEnemy == null) { promptSelectEnemy(); return; }
            executeAction(engine.playerBasicAttack(selectedEnemy));
        });
        zone.add(basicBtn);

        for (Ability ab : actor.getAbilities()) {
            JButton btn = new JButton(ab.getName() + "  [" + ab.getPaCost() + " PA]");
            btn.setToolTipText(ab.getDescription());
            Theme.stylePAActionButton(btn);
            boolean canUse = actor.getPa() >= ab.getPaCost();
            boolean needsEnemy = ab.getTargetType() == Ability.TargetType.SINGLE_ENEMY
                              || ab.getTargetType() == Ability.TargetType.ALL_ENEMIES;
            btn.setEnabled(canUse && (!needsEnemy || selectedEnemy != null));
            if (!canUse) btn.setForeground(Theme.TEXT_MUTED);
            btn.addActionListener(e -> {
                if (needsEnemy && selectedEnemy == null) { promptSelectEnemy(); return; }
                Character allyTarget = null;
                if (ab.getTargetType() == Ability.TargetType.SINGLE_ALLY) {
                    allyTarget = selectAllyDialog();
                    if (allyTarget == null) return;
                }
                executeAction(engine.playerUseAbility(ab, selectedEnemy, allyTarget));
            });
            zone.add(btn);
        }
        // Relleno de celdas vacías para GridLayout uniforme
        int remainder = btnCount % cols;
        if (remainder != 0) for (int i = remainder; i < cols; i++) {
            JPanel empty = new JPanel(); empty.setOpaque(false); zone.add(empty);
        }
        return zone;
    }

    /** Zona central: pociones agrupadas por tipo con indicador [xN]. */
    private JPanel buildConsumablesZone() {
        JPanel zone = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0x06, 0x04, 0x0C, 170));
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setColor(new Color(0x40, 0x28, 0x60, 100));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
            }
        };
        zone.setOpaque(false);
        zone.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        zone.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        // Agrupar por tipo
        Map<Potion.Effect, Integer> counts = new LinkedHashMap<>();
        Map<Potion.Effect, Potion>  reps   = new LinkedHashMap<>();
        for (Potion p : inventory.getPotions()) {
            counts.merge(p.getEffect(), 1, Integer::sum);
            reps.putIfAbsent(p.getEffect(), p);
        }

        if (counts.isEmpty()) {
            JLabel empty = new JLabel("Sin consumibles");
            empty.setFont(Theme.bodyFont(10f));
            empty.setForeground(Theme.TEXT_MUTED);
            zone.add(empty);
        } else {
            for (Potion.Effect fx : counts.keySet()) {
                Potion rep = reps.get(fx);
                int cnt = counts.get(fx);
                JButton btn = new JButton(rep.getName() + "  [x" + cnt + "]");
                Theme.styleItemButton(btn);
                btn.setToolTipText(rep.getDescription());
                btn.addActionListener(e -> enterAllyTargeting(rep));
                zone.add(btn);
            }
        }
        return zone;
    }

    /** Zona derecha: botón de terminar turno grande. */
    private JPanel buildEndTurnZone() {
        JPanel zone = new JPanel(new GridBagLayout());
        zone.setOpaque(false);
        JButton endBtn = new JButton("↷  Terminar turno");
        Theme.styleEndTurnButton(endBtn);
        endBtn.setPreferredSize(new Dimension(148, 64));
        endBtn.addActionListener(e -> executeAction(engine.playerEndTurn()));
        zone.add(endBtn);
        return zone;
    }

    // ── Targeting de aliados ───────────────────────────────────────────────

    private void enterAllyTargeting(Potion potion) {
        pendingPotion = potion;
        targetMode    = TargetMode.WAITING_ALLY;
        refreshTargetableCards();

        // Reemplazar el panel de acciones con el prompt de targeting
        actionPanel.removeAll();
        JPanel prompt = new JPanel(new BorderLayout(12, 0));
        prompt.setOpaque(false);
        prompt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel msg = new JLabel("  → Elegí un aliado para usar:  " + potion.getName());
        msg.setFont(Theme.labelFont(13f));
        msg.setForeground(new Color(0x50, 0xE0, 0x80));
        prompt.add(msg, BorderLayout.WEST);

        JButton cancel = new JButton("Cancelar");
        Theme.styleButton(cancel);
        cancel.addActionListener(e -> { exitTargeting(); buildActionPanel(); });
        prompt.add(cancel, BorderLayout.EAST);

        actionPanel.add(Box.createVerticalGlue());
        actionPanel.add(prompt);
        actionPanel.add(Box.createVerticalGlue());
        actionPanel.revalidate();
        actionPanel.repaint();
    }

    private void exitTargeting() {
        targetMode    = TargetMode.NONE;
        pendingPotion = null;
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
        if (pendingPotion == null || targetMode != TargetMode.WAITING_ALLY) return;
        boolean needsDead = pendingPotion.getEffect() == Potion.Effect.REVIVE;
        if (needsDead && target.isAlive()) {
            log.addMessage("El Elixir solo puede usarse en un aliado caído.", Theme.STUN_COLOR);
            return;
        }
        if (!needsDead && !target.isAlive()) {
            log.addMessage("Este ítem solo puede usarse en un aliado vivo.", Theme.STUN_COLOR);
            return;
        }
        Potion pot = pendingPotion;
        exitTargeting();
        inventory.removeItem(pot);
        executeAction(engine.playerUsePotion(pot, target));
    }

    private void executeAction(List<BattleEvent> events) {
        setActionsEnabled(false);
        pendingEvents.addAll(events);
        processNextEvent();
    }

    private void setActionsEnabled(boolean enabled) {
        for (Component c : actionPanel.getComponents()) {
            if (c instanceof JScrollPane sp) {
                Component view = sp.getViewport().getView();
                if (view instanceof JPanel p)
                    for (Component b : p.getComponents())
                        b.setEnabled(enabled);
            } else if (c instanceof JPanel p) {
                for (Component b : p.getComponents())
                    b.setEnabled(enabled);
            }
        }
    }

    // ── Selección de enemigo ───────────────────────────────────────────────

    private void selectEnemy(Enemy e) {
        selectedEnemy = e;
        enemyCards.forEach(c -> c.setTargeted(c.getEnemy() == e));
        buildActionPanel();
    }

    private void promptSelectEnemy() {
        log.addMessage("Seleccioná un objetivo haciendo click en uno de los enemigos.", Theme.STUN_COLOR);
    }

    private Character selectAllyDialog() {
        List<Character> alive = party.stream().filter(Character::isAlive).toList();
        if (alive.size() == 1) return alive.get(0);
        String[] names = alive.stream().map(c -> c.getName() + " (" + c.getHp() + "/" + c.getHpMax() + " HP)").toArray(String[]::new);
        String choice = (String) JOptionPane.showInputDialog(
            this, "Elegir objetivo:", "Curación", JOptionPane.PLAIN_MESSAGE,
            null, names, names[0]);
        if (choice == null) return null;
        int idx = 0;
        for (int i = 0; i < names.length; i++) if (names[i].equals(choice)) { idx = i; break; }
        return alive.get(idx);
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
    }

    private void highlightActiveCharacter(String name) {
        charCards.forEach(c -> c.setActive(c.getCharacterName().equals(name)));
    }
}
