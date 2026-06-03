package com.dungeontales.core.model.enemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Fábrica que genera grupos de enemigos según el nivel del mapa. */
public class EnemyFactory {
    private static final Random RNG = new Random();

    public static List<Enemy> createNormalEncounter(int mapLevel) {
        List<Enemy> enemies = new ArrayList<>();
        int count = mapLevel < 2 ? 2 + RNG.nextInt(2) : 3 + RNG.nextInt(2);
        for (int i = 0; i < count; i++) {
            enemies.add(mapLevel < 2 ? new Goblin() : pickNormalEnemy(mapLevel));
        }
        return enemies;
    }

    public static List<Enemy> createEliteEncounter(int mapLevel) {
        List<Enemy> enemies = new ArrayList<>();
        enemies.add(new Troll());
        enemies.add(new Goblin());
        if (mapLevel > 1) enemies.add(new Skeleton());
        return enemies;
    }

    public static List<Enemy> createBossEncounter(int mapLevel) {
        List<Enemy> enemies = new ArrayList<>();
        String[] bossNames = {
            "La Sombra Ancestral",
            "El Rey Esqueleto",
            "El Troll Eterno",
            "El Señor de las Tinieblas"
        };
        String bossName = bossNames[Math.min(mapLevel - 1, bossNames.length - 1)];
        enemies.add(new FinalBoss(bossName, mapLevel));
        if (mapLevel > 1) enemies.add(new Skeleton());
        return enemies;
    }

    private static Enemy pickNormalEnemy(int level) {
        return RNG.nextBoolean() ? new Goblin() : new Skeleton();
    }

    // Permitir instanciar clases package-private desde fuera del paquete
    public static Enemy newGoblin()   { return new Goblin(); }
    public static Enemy newSkeleton() { return new Skeleton(); }
    public static Enemy newTroll()    { return new Troll(); }
    public static Enemy newBoss(String name, int level) { return new FinalBoss(name, level); }
}
