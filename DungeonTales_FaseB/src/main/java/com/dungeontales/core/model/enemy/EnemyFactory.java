package com.dungeontales.core.model.enemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Fábrica que genera grupos de enemigos según el piso del mapa. */
public class EnemyFactory {
    private static final Random RNG = new Random();

    // ── Encuentros normales ────────────────────────────────────────────────

    public static List<Enemy> createNormalEncounter(int mapLevel) {
        List<Enemy> enemies = new ArrayList<>();
        int count = 2 + RNG.nextInt(2); // 2 o 3 enemigos
        for (int i = 0; i < count; i++) {
            enemies.add(pickNormalEnemy(mapLevel));
        }
        return enemies;
    }

    private static Enemy pickNormalEnemy(int level) {
        return switch (level) {
            case 1  -> RNG.nextBoolean() ? new Goblin()        : new GoblinArquero();
            case 2  -> RNG.nextBoolean() ? new Skeleton()       : new ShadowHound();
            default -> switch (RNG.nextInt(4)) {
                case 0  -> new DemonioMenor();
                case 1  -> new CultistaOscuro();
                case 2  -> new Skeleton();
                default -> new Goblin();
            };
        };
    }

    // ── Encuentros élite (mini-boss) ───────────────────────────────────────

    public static List<Enemy> createEliteEncounter(int mapLevel) {
        List<Enemy> enemies = new ArrayList<>();
        switch (mapLevel) {
            case 1 -> {
                enemies.add(new Troll());
                enemies.add(new Ogro());
            }
            case 2 -> {
                enemies.add(new CaballeroOscuro());
                enemies.add(new EsqueletoGigante());
            }
            default -> {
                enemies.add(new VampiroAnciano());
                enemies.add(new GolemDePiedra());
            }
        }
        return enemies;
    }

    // ── Encuentros de boss ─────────────────────────────────────────────────

    public static List<Enemy> createBossEncounter(int mapLevel) {
        List<Enemy> enemies = new ArrayList<>();
        switch (mapLevel) {
            case 1 -> {
                enemies.add(new SombraAncestral());
                enemies.add(new GoblinArquero());
            }
            case 2 -> {
                enemies.add(new ReyEsqueleto());
                enemies.add(new ShadowHound());
            }
            default -> {
                enemies.add(new SenorDeLasTinieblas());
                enemies.add(new CaballeroOscuro());
            }
        }
        return enemies;
    }

    // ── Factory helpers (para instanciar desde fuera del paquete) ─────────

    public static Enemy newGoblin()          { return new Goblin(); }
    public static Enemy newSkeleton()        { return new Skeleton(); }
    public static Enemy newTroll()           { return new Troll(); }
    public static Enemy newGoblinArquero()   { return new GoblinArquero(); }
    public static Enemy newShadowHound()     { return new ShadowHound(); }
    public static Enemy newOgro()            { return new Ogro(); }
    public static Enemy newCaballeroOscuro() { return new CaballeroOscuro(); }
    public static Enemy newDemonio()         { return new DemonioMenor(); }
    public static Enemy newCultista()        { return new CultistaOscuro(); }
    public static Enemy newVampiro()         { return new VampiroAnciano(); }
    public static Enemy newGolem()           { return new GolemDePiedra(); }
    public static Enemy newBoss1()           { return new SombraAncestral(); }
    public static Enemy newBoss2()           { return new ReyEsqueleto(); }
    public static Enemy newBoss3()           { return new SenorDeLasTinieblas(); }
}
