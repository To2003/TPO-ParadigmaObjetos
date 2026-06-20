package com.dungeontales.core.model.enemy;

public class Troll extends Enemy {
    private final int regeneration = 5;

    public Troll() {
        super("Troll de Piedra", 250 + RNG.nextInt(50), 30 + RNG.nextInt(10),
              12, 5 + RNG.nextInt(4), 120, 40);
    }
    @Override public String getSpriteName() { return "miniboss/troll"; }
    @Override public void startTurn() {
        super.startTurn();
        int regen = Math.min(regeneration, hpMax - hp);
        hp += regen;
    }
    @Override public String decideAction() {
        double r = Math.random();
        if (r < 0.40) return "attack";
        if (r < 0.65) return "smash";
        if (r < 0.80) return "roar";
        return "attack";
    }
    public int getRegenerationAmount() { return Math.min(regeneration, hpMax - hp); }
}
