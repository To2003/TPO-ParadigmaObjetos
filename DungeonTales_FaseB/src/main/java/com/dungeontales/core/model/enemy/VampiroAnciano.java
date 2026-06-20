package com.dungeontales.core.model.enemy;

public class VampiroAnciano extends Enemy {
    public VampiroAnciano() {
        super("Vampiro Anciano", 180 + RNG.nextInt(30), 35 + RNG.nextInt(8),
              10, 16 + RNG.nextInt(5), 160, 60);
    }
    @Override public String getSpriteName() { return "miniboss/vampiro"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.50) return "attack";
        if (r < 0.70) return "double_attack";
        if (r < 0.85) return "heavy_strike";
        return "roar";
    }
}
