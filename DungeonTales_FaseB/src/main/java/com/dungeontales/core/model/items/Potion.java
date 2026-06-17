package com.dungeontales.core.model.items;

public class Potion extends Item {
    public enum Effect { HEAL_SMALL, HEAL_LARGE, ANTIDOTE, STRENGTH, REVIVE, PA_RESTORE }

    private final Effect effect;
    private final int power;

    public Potion(String name, String desc, Rarity rarity, int value,
                Effect effect, int power) {
        super(name, desc, rarity, value);
        this.effect = effect; this.power = power;
    }

    public Effect getEffect() { return effect; }
    public int getPower()     { return power; }

    public static Potion smallHeal() {
        return new Potion("Poción de Vida", "Recupera 50 HP.", Rarity.COMMON, 25, Effect.HEAL_SMALL, 50);
    }
    public static Potion paPotion() {
        return new Potion("Poción de PA", "Recupera 4 PA al instante.", Rarity.UNCOMMON, 40, Effect.PA_RESTORE, 4);
    }
    public static Potion revivePotion() {
        return new Potion("Elixir de Revivir", "Revive a un aliado caído con 40% HP.", Rarity.RARE, 110, Effect.REVIVE, 40);
    }
}
