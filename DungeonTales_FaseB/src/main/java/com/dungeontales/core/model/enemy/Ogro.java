package com.dungeontales.core.model.enemy;

public class Ogro extends Enemy {
    public Ogro() {
        super("Ogro Salvaje", 200 + RNG.nextInt(40), 28 + RNG.nextInt(7),
              10, 5 + RNG.nextInt(4), 110, 38);
    }
    @Override public String getSpriteName() { return "miniboss/ogro"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.45) return "attack";
        if (r < 0.70) return "smash";
        if (r < 0.85) return "roar";
        return "heavy_strike";
    }
}
