package com.dungeontales.core.model.enemy;

import com.dungeontales.core.model.StatusEffect;

public class Goblin extends Enemy {
    public Goblin() {
        super("Goblin Acechador", 100 + RNG.nextInt(30), 16 + RNG.nextInt(6),
            3, 16 + RNG.nextInt(5), 30, 10);
    }
    @Override public String getSpriteName() { return "goblin"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.60) return "attack";
        if (r < 0.85) return "double_attack";
        return "flee_attempt";
    }
    /** Texto de la próxima acción para mostrar en UI */
    public String getNextActionHint() {
        return "Ataque básico";
    }
}

class Skeleton extends Enemy {
    public Skeleton() {
        super("Esqueleto Guerrero", 140 + RNG.nextInt(30), 22 + RNG.nextInt(6),
            7, 10 + RNG.nextInt(5), 55, 18);
    }
    @Override public String getSpriteName() { return "skeleton"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.50) return "attack";
        if (r < 0.75) return "heavy_strike";
        if (r < 0.90) return "defend";
        return "bone_throw";
    }
}

class Troll extends Enemy {
    private final int regeneration = 5;
    public Troll() {
        super("Troll de Piedra", 250 + RNG.nextInt(50), 30 + RNG.nextInt(10),
            12, 5 + RNG.nextInt(4), 120, 40);
    }
    @Override public String getSpriteName() { return "troll"; }
    @Override public void startTurn() {
        super.startTurn();
        int regen = Math.min(regeneration, hpMax - hp);
        hp += regen; // regeneración pasiva
    }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.40) return "attack";
        if (r < 0.65) return "smash";
        if (r < 0.80) return "roar";
        return "attack";
    }
    public int getRegenerationAmount() { return Math.min(regeneration, hpMax - hp); }
}

class FinalBoss extends Enemy {
    private int phase = 1;
    public FinalBoss(String name, int level) {
        super(name, 200 + level * 30, 25 + level * 3, 14 + level * 2,
            12, 400, 150);
    }
    @Override public String getSpriteName() { return "boss"; }
    @Override public void startTurn() {
        super.startTurn();
        // Fase 2: cuando baja del 50% HP
        if (phase == 1 && hp < hpMax / 2) {
            phase = 2;
            atk += 8; def += 4;
        }
    }
    @Override public String decideAction() {
        if (phase == 2) {
            double r = Math.random();
            if (r < 0.35) return "heavy_strike";
            if (r < 0.60) return "attack";
            if (r < 0.80) return "roar";
            return "smash";
        }
        double r = Math.random();
        if (r < 0.50) return "attack";
        if (r < 0.70) return "heavy_strike";
        if (r < 0.85) return "roar";
        return "smash";
    }
    public int getPhase() { return phase; }
}
