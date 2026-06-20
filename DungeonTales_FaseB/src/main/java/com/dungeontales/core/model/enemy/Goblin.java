package com.dungeontales.core.model.enemy;

public class Goblin extends Enemy {
    public Goblin() {
        super("Goblin Acechador", 100 + RNG.nextInt(30), 16 + RNG.nextInt(6),
              3, 16 + RNG.nextInt(5), 30, 10);
    }
    @Override public String getSpriteName() { return "normales/goblin"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.60) return "attack";
        if (r < 0.85) return "double_attack";
        return "flee_attempt";
    }
}
