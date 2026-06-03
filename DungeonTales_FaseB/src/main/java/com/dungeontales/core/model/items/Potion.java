package com.dungeontales.core.model.items;

public class Potion extends Item {
    public enum Effect { HEAL_SMALL, HEAL_LARGE, ANTIDOTE, STRENGTH, REVIVE }

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
        return new Potion("Poción de Vida", "Recupera 40 HP.", Rarity.COMMON, 20, Effect.HEAL_SMALL, 40);
    }
    public static Potion largeHeal() {
        return new Potion("Poción Mayor de Vida", "Recupera 80 HP.", Rarity.UNCOMMON, 40, Effect.HEAL_LARGE, 80);
    }
    public static Potion antidote() {
        return new Potion("Antídoto", "Cura el Veneno.", Rarity.COMMON, 15, Effect.ANTIDOTE, 0);
    }
    public static Potion strengthPotion() {
        return new Potion("Poción de Fuerza", "+8 ATK por 2 turnos.", Rarity.UNCOMMON, 35, Effect.STRENGTH, 8);
    }
    public static Potion revivePotion() {
        return new Potion("Elixir de Vida", "Revive a un personaje caído con 30% HP.", Rarity.RARE, 120, Effect.REVIVE, 30);
    }
}
