package com.dungeontales.core.model.enemy;

public class SombraAncestral extends Enemy {
    private int phase = 1;

    public SombraAncestral() {
        super("La Sombra Ancestral", 360 + RNG.nextInt(40), 30 + RNG.nextInt(5),
              12, 14, 400, 150);
    }
    @Override public String getSpriteName() { return "boss/sombra_ancestral"; }
    @Override public void startTurn() {
        super.startTurn();
        if (phase == 1 && hp < hpMax / 2) {
            phase = 2;
            atk += 6; spd += 5;
        }
    }
    @Override public String decideAction() {
        if (phase == 2) {
            double r = Math.random();
            if (r < 0.40) return "heavy_strike";
            if (r < 0.65) return "double_attack";
            if (r < 0.85) return "attack";
            return "roar";
        }
        double r = Math.random();
        if (r < 0.50) return "attack";
        if (r < 0.70) return "heavy_strike";
        if (r < 0.85) return "roar";
        return "smash";
    }
}
