package com.dungeontales.ui;

import com.dungeontales.core.GameState;
import com.dungeontales.core.battle.BattleEngine;
import com.dungeontales.core.map.MapNode;
import com.dungeontales.core.model.enemy.EnemyFactory;
import com.dungeontales.core.model.enemy.Enemy;
import com.dungeontales.save.SaveManager;
import com.dungeontales.ui.screens.*;
import com.dungeontales.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Ventana principal del juego.
 * Gestiona el CardLayout y las transiciones entre pantallas.
 */
public class GameWindow extends JFrame {

    private static final String SCREEN_MENU    = "MENU";
    private static final String SCREEN_MAP     = "MAP";
    private static final String SCREEN_BATTLE  = "BATTLE";
    private static final String SCREEN_RESULT  = "RESULT";
    private static final String SCREEN_REST    = "REST";
    private static final String SCREEN_SHOP    = "SHOP";
    private static final String SCREEN_GAMEOVER  = "GAMEOVER";
    private static final String SCREEN_TREASURE  = "TREASURE";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     cardPanel  = new JPanel(cardLayout);
    private GameState        state;

    public GameWindow() {
        super("Dungeon Tales");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 640));
        setPreferredSize(new Dimension(1024, 720));
        getContentPane().setBackground(Theme.BG_DARK);

        cardPanel.setBackground(Theme.BG_DARK);
        add(cardPanel);

        showMainMenu();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── Navegación de pantallas ────────────────────────────────────────────

    private void showMainMenu() {
        cardPanel.removeAll();
        MainMenuScreen menu = new MainMenuScreen(new MainMenuScreen.Listener() {
            @Override public void onNewGame() {
                SaveManager.deleteSave();
                state = GameState.newGame(3); // 3 niveles
                showMap();
            }
            @Override public void onLoadGame() {
                GameState loaded = SaveManager.load();
                if (loaded != null) { state = loaded; showMap(); }
                else JOptionPane.showMessageDialog(GameWindow.this,
                    "No se pudo cargar la partida.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cardPanel.add(menu, SCREEN_MENU);
        cardLayout.show(cardPanel, SCREEN_MENU);
        revalidate();
    }

    private void showMap() {
        cardPanel.removeAll();
        MapScreen map = new MapScreen(state, new MapScreen.Listener() {
            @Override public void onNodeSelected(MapNode node) {
                state.getCurrentMap().advance(node);
                handleNode(node);
            }
            @Override public void onSaveRequested() {
                boolean ok = SaveManager.save(state);
                JOptionPane.showMessageDialog(GameWindow.this,
                    ok ? "Partida guardada correctamente." : "Error al guardar.",
                    "Guardar", JOptionPane.INFORMATION_MESSAGE);
            }
            @Override public void onPartyRequested() {
                new PartyScreen(GameWindow.this, state).setVisible(true);
            }
        });
        cardPanel.add(map, SCREEN_MAP);
        cardLayout.show(cardPanel, SCREEN_MAP);
        revalidate();
    }

    private void handleNode(MapNode node) {
        switch (node.getType()) {
            case COMBAT   -> startBattle(EnemyFactory.createNormalEncounter(state.getMapLevel()), "lvl" + state.getMapLevel() + "-combat");
            case ELITE    -> startBattle(EnemyFactory.createEliteEncounter(state.getMapLevel()), "lvl" + state.getMapLevel() + "-miniBoss");
            case BOSS     -> startBattle(EnemyFactory.createBossEncounter(state.getMapLevel()), "lvl" + state.getMapLevel() + "-boss");
            case REST     -> showRest();
            case SHOP     -> showShop();
            case TREASURE -> showTreasure();
        }
    }

    private void startBattle(List<Enemy> enemies, String bgName) {
        cardPanel.removeAll();
        BattleEngine engine = new BattleEngine(state.getAliveParty(), enemies);

        BattleScreen battle = new BattleScreen(engine, state.getAliveParty(), enemies, state.getInventory(), bgName,
            new BattleScreen.Listener() {
                @Override public void onBattleWon(int exp, int gold, String item, java.util.List<String> levelUps) {
                    state.addBattleWon();
                    state.setLastBattleResult(exp, gold, item, levelUps);
                    state.addGold(gold);
                    showResult();
                }
                @Override public void onBattleGameOver() {
                    showGameOver();
                }
            });

        cardPanel.add(battle, SCREEN_BATTLE);
        cardLayout.show(cardPanel, SCREEN_BATTLE);
        revalidate();
    }

    private void showResult() {
        cardPanel.removeAll();
        ResultScreen result = new ResultScreen(state, () -> {
            // ¿Era el jefe y hay más niveles?
            if (state.getCurrentMap().isBossDefeated()) {
                if (!state.isLastLevel()) {
                    state.advanceToNextLevel();
                    JOptionPane.showMessageDialog(this,
                        "¡Nivel completado!\nAvanzás al Nivel " + state.getMapLevel() + ".\nLa party se cura un 30% de HP.",
                        "Nivel Completado", JOptionPane.PLAIN_MESSAGE);
                } else {
                    showFinalVictory();
                    return;
                }
            }
            showMap();
        });
        cardPanel.add(result, SCREEN_RESULT);
        cardLayout.show(cardPanel, SCREEN_RESULT);
        revalidate();
    }

    private void showRest() {
        cardPanel.removeAll();
        RestScreen rest = new RestScreen(state, this::showMap);
        cardPanel.add(rest, SCREEN_REST);
        cardLayout.show(cardPanel, SCREEN_REST);
        revalidate();
    }

    private void showShop() {
        cardPanel.removeAll();
        ShopScreen shop = new ShopScreen(state, this::showMap);
        cardPanel.add(shop, SCREEN_SHOP);
        cardLayout.show(cardPanel, SCREEN_SHOP);
        revalidate();
    }

    private void showTreasure() {
        cardPanel.removeAll();
        TreasureScreen treasure = new TreasureScreen(state, this::showMap);
        cardPanel.add(treasure, SCREEN_TREASURE);
        cardLayout.show(cardPanel, SCREEN_TREASURE);
        revalidate();
    }

    private void showGameOver() {
        cardPanel.removeAll();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BG_DARK);

        JPanel content = new JPanel();
        content.setBackground(Theme.BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("GAME  OVER", SwingConstants.CENTER);
        title.setFont(Theme.titleFont(48f));
        title.setForeground(new Color(0xE0, 0x30, 0x20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Tu party ha sido derrotada en el calabozo...", SwingConstants.CENTER);
        sub.setFont(Theme.bodyFont(14f));
        sub.setForeground(Theme.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton menu = new JButton("Volver al menú");
        Theme.styleMenuButton(menu);
        menu.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.addActionListener(e -> { SaveManager.deleteSave(); showMainMenu(); });

        content.add(Box.createVerticalStrut(60));
        content.add(title);
        content.add(Box.createVerticalStrut(20));
        content.add(sub);
        content.add(Box.createVerticalStrut(40));
        content.add(menu);

        panel.add(content);
        cardPanel.add(panel, SCREEN_GAMEOVER);
        cardLayout.show(cardPanel, SCREEN_GAMEOVER);
        revalidate();
    }

    private void showFinalVictory() {
        cardPanel.removeAll();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BG_DARK);

        JPanel content = new JPanel();
        content.setBackground(Theme.BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("¡¡ VICTORIA FINAL !!", SwingConstants.CENTER);
        title.setFont(Theme.titleFont(42f));
        title.setForeground(Theme.GOLD_COLOR);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Derrotaste a todos los jefes del Dungeon!", SwingConstants.CENTER);
        sub.setFont(Theme.bodyFont(16f));
        sub.setForeground(Theme.TEXT_PRIMARY);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel stats = new JLabel("Batallas ganadas: " + state.getBattlesWon() +
            "  |  Oro final: " + state.getGold() + " GP");
        stats.setFont(Theme.bodyFont(13f));
        stats.setForeground(Theme.TEXT_SECONDARY);
        stats.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton menu = new JButton("Volver al menú");
        Theme.styleMenuButton(menu);
        menu.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.addActionListener(e -> { SaveManager.deleteSave(); showMainMenu(); });

        content.add(Box.createVerticalStrut(60));
        content.add(title);
        content.add(Box.createVerticalStrut(16));
        content.add(sub);
        content.add(Box.createVerticalStrut(12));
        content.add(stats);
        content.add(Box.createVerticalStrut(40));
        content.add(menu);

        panel.add(content);
        cardPanel.add(panel, "VICTORY");
        cardLayout.show(cardPanel, "VICTORY");
        revalidate();
    }
}
