package com.dungeontales.core.model.enemy;

public class ReyEsqueleto extends Enemy {
    private int phase = 1;

    public ReyEsqueleto() {
        super("El Rey Esqueleto", 460 + RNG.nextInt(40), 38 + RNG.nextInt(6),
              16, 10, 500, 200);
    }
    @Override public String getSpriteName() { return "boss/rey_esqueleto"; }
    @Override public void startTurn() {
        super.startTurn();
        if (phase == 1 && hp < hpMax / 2) {
            phase = 2;
            atk += 12; def -= 4;
        }
    }
    @Override public String decideAction() {
        if (phase == 2) {
            double r = Math.random();
            if (r < 0.45) return "heavy_strike";
            if (r < 0.65) return "smash";
            if (r < 0.85) return "attack";
            return "double_attack";
        }
        double r = Math.random();
        if (r < 0.45) return "attack";
        if (r < 0.65) return "heavy_strike";
        if (r < 0.80) return "defend";
        return "smash";
    }
}
