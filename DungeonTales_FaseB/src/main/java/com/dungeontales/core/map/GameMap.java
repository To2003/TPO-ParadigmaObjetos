package com.dungeontales.core.map;

import java.io.Serializable;
import java.util.*;

/** Mapa de nodos generado proceduralmente, estilo Slay the Spire. */
public class GameMap implements Serializable {

    private final List<List<MapNode>> rows = new ArrayList<>();
    private MapNode currentNode;
    private final int mapLevel;

    // Distribución de tipos por fila (excluyendo fila de inicio y jefe)
    private static final MapNode.Type[][] ROW_POOLS = {
        { MapNode.Type.COMBAT, MapNode.Type.COMBAT, MapNode.Type.COMBAT, MapNode.Type.ELITE },
        { MapNode.Type.COMBAT, MapNode.Type.COMBAT, MapNode.Type.TREASURE, MapNode.Type.REST },
        { MapNode.Type.COMBAT, MapNode.Type.ELITE,  MapNode.Type.SHOP,    MapNode.Type.COMBAT },
        { MapNode.Type.COMBAT, MapNode.Type.REST,   MapNode.Type.COMBAT,  MapNode.Type.ELITE },
        { MapNode.Type.COMBAT, MapNode.Type.TREASURE,MapNode.Type.SHOP,   MapNode.Type.COMBAT },
    };

    public GameMap(int mapLevel, long seed) {
        this.mapLevel = mapLevel;
        generate(seed);
    }

    private void generate(long seed) {
        Random rng = new Random(seed);

        // Fila 0: nodo de inicio
        MapNode start = new MapNode(MapNode.Type.COMBAT, 0, 0);
        start.markVisited();
        rows.add(List.of(start));
        currentNode = start;

        // Filas intermedias: 12 filas con 2-3 nodos cada una
        int[] rowSizes = { 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3 };
        for (int r = 0; r < rowSizes.length; r++) {
            List<MapNode> row = new ArrayList<>();
            MapNode.Type[] pool = ROW_POOLS[r % ROW_POOLS.length];
            Set<MapNode.Type> used = new HashSet<>();

            for (int c = 0; c < rowSizes[r]; c++) {
                MapNode.Type type;
                int tries = 0;
                do {
                    type = pool[rng.nextInt(pool.length)];
                    tries++;
                } while (used.contains(type) && tries < 10);
                used.add(type);
                row.add(new MapNode(type, r + 1, c));
            }
            rows.add(row);
        }

        // Última fila: el Jefe
        MapNode boss = new MapNode(MapNode.Type.BOSS, rows.size(), 0);
        rows.add(List.of(boss));

        // Conectar filas con bifurcaciones
        for (int r = 0; r < rows.size() - 1; r++) {
            List<MapNode> cur  = rows.get(r);
            List<MapNode> next = rows.get(r + 1);
            for (MapNode node : cur) {
                // Cada nodo conecta a 1-2 nodos de la fila siguiente
                MapNode primary = next.get(rng.nextInt(next.size()));
                node.addNext(primary);
                if (next.size() > 1 && rng.nextBoolean()) {
                    MapNode secondary;
                    do { secondary = next.get(rng.nextInt(next.size())); }
                    while (secondary == primary);
                    node.addNext(secondary);
                }
            }
            // Garantizar que cada nodo en next tiene al menos un padre
            for (MapNode nextNode : next) {
                boolean hasParent = cur.stream().anyMatch(n -> n.getNext().contains(nextNode));
                if (!hasParent) {
                    cur.get(rng.nextInt(cur.size())).addNext(nextNode);
                }
            }
        }
    }

    /** Nodos disponibles desde la posición actual. */
    public List<MapNode> getAvailableNodes() {
        return currentNode.getNext().stream()
            .filter(n -> !n.isVisited())
            .toList();
    }

    public boolean advance(MapNode chosen) {
        if (!currentNode.getNext().contains(chosen)) return false;
        currentNode = chosen;
        currentNode.markVisited();
        return true;
    }

    public MapNode getCurrentNode() { return currentNode; }
    public List<List<MapNode>> getRows() { return Collections.unmodifiableList(rows); }
    public int getMapLevel()         { return mapLevel; }
    public boolean isBossDefeated()  { return currentNode.getType() == MapNode.Type.BOSS
                                           && currentNode.isVisited(); }
}
