package com.dungeontales.core.model.enemy;

public class GolemDePiedra extends Enemy {
    public GolemDePiedra() {
        super("Golem de Piedra", 280 + RNG.nextInt(40), 32 + RNG.nextInt(8),
              22, 3 + RNG.nextInt(3), 170, 65);
    }
    @Override public String getSpriteName() { return "miniboss/golem_piedra"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.40) return "smash";
        if (r < 0.65) return "attack";
        if (r < 0.80) return "heavy_strike";
        return "defend";
    }
}
