package com.dungeontales.core.model.enemy;

public class CultistaOscuro extends Enemy {
    public CultistaOscuro() {
        super("Cultista Oscuro", 120 + RNG.nextInt(25), 22 + RNG.nextInt(6),
              6, 12 + RNG.nextInt(4), 75, 28);
    }
    @Override public String getSpriteName() { return "normales/cultista"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.45) return "attack";
        if (r < 0.70) return "heavy_strike";
        if (r < 0.85) return "roar";
        return "bone_throw";
    }
}
