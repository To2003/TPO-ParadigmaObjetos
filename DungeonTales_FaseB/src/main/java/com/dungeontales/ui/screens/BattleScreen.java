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
import java.util.ArrayList;
import java.util.List;

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

    // Selección de objetivo
    private Enemy selectedEnemy = null;

    // Timer para procesar eventos con delay visual
    private final List<BattleEvent> pendingEvents = new ArrayList<>();
    private Timer eventTimer;

    public BattleScreen(BattleEngine engine, List<Character> party,
                        List<Enemy> enemies, Inventory inventory, Listener listener) {
        this.engine   = engine;
        this.party    = party;
        this.enemies  = enemies;
        this.inventory = inventory;
        this.listener = listener;

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
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Theme.BG_DARK);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;

        // Party
        JPanel partyPanel = new JPanel(new GridBagLayout());
        partyPanel.setBackground(Theme.BG_DARK);
        GridBagConstraints pGbc = new GridBagConstraints();
        pGbc.anchor = GridBagConstraints.SOUTH;
        pGbc.insets = new Insets(0, 5, 0, 5);
        for (Character c : party) {
            CharacterCard card = new CharacterCard(c);
            charCards.add(card);
            partyPanel.add(card, pGbc);
        }

        // VS label
        JLabel vsLabel = new JLabel("VS");
        vsLabel.setFont(Theme.titleFont(22f));
        vsLabel.setForeground(Theme.TEXT_MUTED);
        vsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        vsLabel.setPreferredSize(new Dimension(80, 320));

        // Enemigos
        JPanel enemyPanel = new JPanel(new GridBagLayout());
        enemyPanel.setBackground(Theme.BG_DARK);
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
        actionPanel.setBackground(Theme.BG_ACTION_BAR);
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SEPARATOR),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Insertar entre log y borde sur
        JPanel southStack = new JPanel(new BorderLayout());
        southStack.setBackground(Theme.BG_DARK);
        southStack.add(actionPanel, BorderLayout.NORTH);
        southStack.add(log, BorderLayout.CENTER);
        remove(log);
        add(southStack, BorderLayout.SOUTH);

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

        // Highlight
        highlightActiveCharacter(actor.getName());

        // Fila 1: info del turno
        JPanel infoRow = new JPanel(new BorderLayout());
        infoRow.setBackground(Theme.BG_ACTION_BAR);
        infoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel info = new JLabel("  Turno de " + actor.getName() +
            "   PA: " + actor.getPa() + "/" + actor.getPaMax());
        info.setFont(Theme.labelFont(11f));
        info.setForeground(Theme.TEXT_PRIMARY);
        infoRow.add(info, BorderLayout.WEST);

        String targetTxt = selectedEnemy != null
            ? "Objetivo: " + selectedEnemy.getName() + "   "
            : "Seleccioná un enemigo   ";
        JLabel targetLbl = new JLabel(targetTxt);
        targetLbl.setFont(Theme.bodyFont(11f));
        targetLbl.setForeground(selectedEnemy != null ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED);
        infoRow.add(targetLbl, BorderLayout.EAST);

        actionPanel.add(infoRow);
        actionPanel.add(Box.createVerticalStrut(5));

        // Fila 2: botones de acción en grid visual
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        btns.setBackground(Theme.BG_ACTION_BAR);
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Ataque básico
        JButton basicBtn = new JButton("Ataque basico  [0 PA]");
        Theme.styleAttackButton(basicBtn);
        basicBtn.setEnabled(selectedEnemy != null);
        basicBtn.addActionListener(e -> {
            if (selectedEnemy == null) { promptSelectEnemy(); return; }
            executeAction(engine.playerBasicAttack(selectedEnemy));
        });
        btns.add(basicBtn);

        // Habilidades
        for (Ability ab : actor.getAbilities()) {
            JButton btn = new JButton(ab.getName() + "  [" + ab.getPaCost() + " PA]");
            btn.setToolTipText(ab.getDescription());
            Theme.stylePAActionButton(btn);
            boolean canUse = actor.getPa() >= ab.getPaCost();
            boolean needsTarget = ab.getTargetType() == Ability.TargetType.SINGLE_ENEMY
                               || ab.getTargetType() == Ability.TargetType.ALL_ENEMIES;
            btn.setEnabled(canUse && (!needsTarget || selectedEnemy != null));
            if (!canUse) btn.setForeground(Theme.TEXT_MUTED);

            btn.addActionListener(e -> {
                if (needsTarget && selectedEnemy == null) { promptSelectEnemy(); return; }
                Character allyTarget = null;
                if (ab.getTargetType() == Ability.TargetType.SINGLE_ALLY) {
                    allyTarget = selectAllyDialog();
                    if (allyTarget == null) return;
                }
                executeAction(engine.playerUseAbility(ab, selectedEnemy, allyTarget));
            });
            btns.add(btn);
        }

        // Potions
        List<Potion> potions = inventory.getPotions();
        for (Potion p : potions) {
            JButton btn = new JButton(p.getName() + "  [Item]");
            Theme.styleItemButton(btn);
            btn.setToolTipText(p.getDescription());
            btn.addActionListener(e -> {
                Character target = null;
                if (p.getName().contains("Vida") || p.getName().contains("Revivir") || p.getName().contains("Antidoto")) {
                    target = selectAllyDialog();
                    if (target == null) return;
                } else {
                    target = actor;
                }
                inventory.removeItem(p);
                executeAction(engine.playerUsePotion(p, target));
            });
            btns.add(btn);
        }

        // Terminar turno
        JButton endBtn = new JButton("Terminar turno");
        Theme.styleEndTurnButton(endBtn);
        endBtn.addActionListener(e -> executeAction(engine.playerEndTurn()));
        btns.add(endBtn);

        actionPanel.add(btns);
        actionPanel.revalidate();
        actionPanel.repaint();
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
