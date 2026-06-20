package com.dungeontales.core.model.enemy;

public class EsqueletoGigante extends Enemy {
    public EsqueletoGigante() {
        super("Esqueleto Gigante", 200 + RNG.nextInt(30), 28 + RNG.nextInt(6),
              12, 6 + RNG.nextInt(4), 130, 45);
    }
    @Override public String getSpriteName() { return "miniboss/esqueleto_gigante"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.45) return "attack";
        if (r < 0.70) return "smash";
        if (r < 0.85) return "heavy_strike";
        return "bone_throw";
    }
}
