package com.dungeontales.core.model.enemy;

public class ShadowHound extends Enemy {
    public ShadowHound() {
        super("Shadow Hound", 165 + RNG.nextInt(30), 20 + RNG.nextInt(6),
              4, 14 + RNG.nextInt(5), 60, 20);
    }
    @Override public String getSpriteName() { return "normales/shadow_hound"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.50) return "attack";
        if (r < 0.75) return "double_attack";
        if (r < 0.90) return "shadow_bite";
        return "heavy_strike";
    }
}
