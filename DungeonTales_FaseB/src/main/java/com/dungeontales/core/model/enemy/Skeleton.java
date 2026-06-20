package com.dungeontales.core.model.enemy;

public class Skeleton extends Enemy {
    public Skeleton() {
        super("Esqueleto Guerrero", 140 + RNG.nextInt(30), 22 + RNG.nextInt(6),
              7, 10 + RNG.nextInt(5), 55, 18);
    }
    @Override public String getSpriteName() { return "normales/skeleton"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.50) return "attack";
        if (r < 0.75) return "heavy_strike";
        if (r < 0.90) return "defend";
        return "bone_throw";
    }
}
