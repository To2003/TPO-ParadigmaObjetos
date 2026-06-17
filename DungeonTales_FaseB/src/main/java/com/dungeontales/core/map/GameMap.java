package com.dungeontales.core.map;

import java.io.Serializable;
import java.util.*;

/** Mapa de nodos generado proceduralmente, estilo Slay the Spire. */
public class GameMap implements Serializable {

    // ── Serialized (persisted in JSON) ───────────────────────────────────
    private final long   seed;
    private final int    mapLevel;
    private int          currentNodeRow = 0;
    private int          currentNodeCol = 0;
    // Coordenadas de cada nodo visitado vía advance() (no incluye el nodo inicio)
    private List<int[]>  visitedNodes   = new ArrayList<>();

    // ── Transient (reconstructed from seed after load) ────────────────────
    private transient List<List<MapNode>> rows;
    private transient MapNode             currentNode;

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
        this.seed     = seed;
        generate();
    }

    // ── Rebuild after Gson deserialization ────────────────────────────────

    /**
     * Regenera el grafo desde el seed y reaplica el estado de visitas guardado.
     * Se llama automáticamente la primera vez que se accede al grafo (lazy)
     * y también explícitamente desde SaveManager tras deserializar.
     */
    public void postLoad() {
        if (visitedNodes == null) visitedNodes = new ArrayList<>();
        generate();
        // Re-mark visited nodes (the start node is already marked inside generate())
        for (int[] coords : visitedNodes) {
            findNode(coords[0], coords[1]).markVisited();
        }
        currentNode = findNode(currentNodeRow, currentNodeCol);
    }

    private void ensureGraph() {
        if (rows == null) postLoad();
    }

    // ── Graph generation ──────────────────────────────────────────────────

    private void generate() {
        rows = new ArrayList<>();
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

    private MapNode findNode(int row, int col) {
        return rows.get(row).stream()
            .filter(n -> n.getCol() == col)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Nodo no encontrado: fila=" + row + " col=" + col));
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Nodos disponibles desde la posición actual. */
    public List<MapNode> getAvailableNodes() {
        ensureGraph();
        return currentNode.getNext().stream()
            .filter(n -> !n.isVisited())
            .toList();
    }

    public boolean advance(MapNode chosen) {
        ensureGraph();
        if (!currentNode.getNext().contains(chosen)) return false;
        currentNode = chosen;
        currentNode.markVisited();
        currentNodeRow = chosen.getRow();
        currentNodeCol = chosen.getCol();
        visitedNodes.add(new int[]{ chosen.getRow(), chosen.getCol() });
        return true;
    }

    public MapNode              getCurrentNode() { ensureGraph(); return currentNode; }
    public List<List<MapNode>>  getRows()        { ensureGraph(); return Collections.unmodifiableList(rows); }
    public int                  getMapLevel()    { return mapLevel; }
    public boolean isBossDefeated() {
        ensureGraph();
        return currentNode.getType() == MapNode.Type.BOSS && currentNode.isVisited();
    }
}
