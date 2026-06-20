package com.dungeontales.core.model.enemy;

public class CaballeroOscuro extends Enemy {
    public CaballeroOscuro() {
        super("Caballero Oscuro", 220 + RNG.nextInt(30), 30 + RNG.nextInt(7),
              18, 8 + RNG.nextInt(5), 140, 50);
    }
    @Override public String getSpriteName() { return "miniboss/caballero_oscuro"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.40) return "attack";
        if (r < 0.65) return "heavy_strike";
        if (r < 0.80) return "defend";
        return "smash";
    }
}
