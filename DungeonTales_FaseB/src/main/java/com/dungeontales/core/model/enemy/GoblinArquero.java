package com.dungeontales.core.model.enemy;

public class GoblinArquero extends Enemy {
    public GoblinArquero() {
        super("Goblin Arquero", 80 + RNG.nextInt(20), 20 + RNG.nextInt(5),
              2, 20 + RNG.nextInt(6), 35, 12);
    }
    @Override public String getSpriteName() { return "normales/goblin_arquero"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.55) return "attack";
        if (r < 0.80) return "double_attack";
        return "flee_attempt";
    }
}
