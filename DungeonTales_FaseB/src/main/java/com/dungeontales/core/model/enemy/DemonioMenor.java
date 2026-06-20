package com.dungeontales.core.model.enemy;

public class DemonioMenor extends Enemy {
    public DemonioMenor() {
        super("Demonio Menor", 150 + RNG.nextInt(30), 28 + RNG.nextInt(7),
              8, 14 + RNG.nextInt(5), 80, 30);
    }
    @Override public String getSpriteName() { return "normales/demonio"; }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.50) return "attack";
        if (r < 0.75) return "heavy_strike";
        if (r < 0.90) return "smash";
        return "roar";
    }
}
