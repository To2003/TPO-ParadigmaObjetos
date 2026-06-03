package com.dungeontales.core;

import com.dungeontales.core.map.GameMap;
import com.dungeontales.core.model.Inventory;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.core.model.character.Paladin;
import com.dungeontales.core.model.character.Rogue;
import com.dungeontales.core.model.character.Warrior;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState implements Serializable {

    public enum Phase { MENU, MAP, BATTLE, REST, SHOP, RESULT, GAME_OVER }

    private List<Character>  party;
    private Inventory        inventory;
    private GameMap          currentMap;
    private int              gold;
    private int              mapLevel;
    private int              totalMaps;
    private Phase            phase;
    private int              battlesWon;
    private int              lastExpGained;
    private int              lastGoldGained;
    private String           lastItemFound;
    private java.util.List<String> lastLevelUps;

    private GameState() {}

    public static GameState newGame(int totalMaps) {
        GameState gs  = new GameState();
        gs.party      = new ArrayList<>();
        gs.party.add(new Rogue("Kira"));
        gs.party.add(new Paladin("Aldric"));
        gs.party.add(new Warrior("Bronn"));
        gs.inventory  = new Inventory();
        gs.gold       = 50;
        gs.mapLevel   = 1;
        gs.totalMaps  = totalMaps;
        gs.phase      = Phase.MAP;
        gs.currentMap = new GameMap(1, System.currentTimeMillis());
        return gs;
    }

    public void advanceToNextLevel() {
        mapLevel++;
        currentMap = new GameMap(mapLevel, System.currentTimeMillis());
        party.stream().filter(Character::isAlive)
             .forEach(c -> c.healHp((int)(c.getHpMax() * 0.30)));
    }

    public boolean isLastLevel() { return mapLevel >= totalMaps; }
    public boolean isGameOver()  { return party.stream().noneMatch(Character::isAlive); }

    public List<Character> getParty()         { return Collections.unmodifiableList(party); }
    public List<Character> getAliveParty()    { return party.stream().filter(Character::isAlive).toList(); }
    public Inventory       getInventory()     { return inventory; }
    public GameMap         getCurrentMap()    { return currentMap; }
    public int             getGold()          { return gold; }
    public int             getMapLevel()      { return mapLevel; }
    public int             getTotalMaps()     { return totalMaps; }
    public Phase           getPhase()         { return phase; }
    public int             getBattlesWon()    { return battlesWon; }
    public int             getLastExpGained() { return lastExpGained; }
    public int             getLastGoldGained(){ return lastGoldGained; }
    public String          getLastItemFound() { return lastItemFound; }
    public java.util.List<String> getLastLevelUps() { return lastLevelUps; }

    public void setPhase(Phase p)    { this.phase = p; }
    public void addGold(int amount)  { this.gold += amount; }
    public boolean spendGold(int n)  { if (gold < n) return false; gold -= n; return true; }
    public void addBattleWon()       { battlesWon++; }
    public void setLastBattleResult(int exp, int gold, String item, java.util.List<String> levelUps) {
        this.lastExpGained  = exp;
        this.lastGoldGained = gold;
        this.lastItemFound  = item;
        this.lastLevelUps   = levelUps;
    }
}
