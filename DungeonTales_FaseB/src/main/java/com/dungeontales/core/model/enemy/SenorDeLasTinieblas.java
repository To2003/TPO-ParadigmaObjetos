package com.dungeontales.core.model.enemy;

public class SenorDeLasTinieblas extends Enemy {
    private int phase = 1;

    public SenorDeLasTinieblas() {
        super("El Señor de las Tinieblas", 560 + RNG.nextInt(60), 48 + RNG.nextInt(8),
              22, 12, 600, 300);
    }
    @Override public String getSpriteName() { return "boss/senor_tinieblas"; }
    @Override public void startTurn() {
        super.startTurn();
        if (phase == 1 && hp < hpMax / 2) {
            phase = 2;
            atk += 10; def += 6; spd += 3;
        }
    }
    @Override public String decideAction() {
        if (phase == 2) {
            double r = Math.random();
            if (r < 0.35) return "heavy_strike";
            if (r < 0.55) return "smash";
            if (r < 0.75) return "roar";
            if (r < 0.90) return "attack";
            return "double_attack";
        }
        double r = Math.random();
        if (r < 0.45) return "attack";
        if (r < 0.65) return "heavy_strike";
        if (r < 0.80) return "roar";
        return "smash";
    }
}
