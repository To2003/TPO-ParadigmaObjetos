package com.dungeontales.core.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Un nodo del mapa roguelite. */
public class MapNode implements Serializable {
    public enum Type { COMBAT, ELITE, BOSS, REST, SHOP, TREASURE }

    private final Type type;
    private final int col, row;
    private final List<MapNode> next = new ArrayList<>();
    private boolean visited = false;

    public MapNode(Type type, int row, int col) {
        this.type = type; this.row = row; this.col = col;
    }

    public void addNext(MapNode n) { next.add(n); }
    public void markVisited()      { visited = true; }
    public Type getType()          { return type; }
    public int getRow()            { return row; }
    public int getCol()            { return col; }
    public boolean isVisited()     { return visited; }
    public List<MapNode> getNext() { return List.copyOf(next); }

    public String getLabel() {
        return switch (type) {
            case COMBAT   -> "COMBATE";
            case ELITE    -> "ÉLITE";
            case BOSS     -> "JEFE";
            case REST     -> "DESCANSO";
            case SHOP     -> "TIENDA";
            case TREASURE -> "TESORO";
        };
    }
}
