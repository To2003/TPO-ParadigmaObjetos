package com.dungeontales.core.model.items;

/** Armadura equipable: aumenta DEF y puede dar bonus de velocidad. */
public class Armor extends Item {
    private final int defBonus;
    private final int spdBonus;
    private final String allowedClass;

    public Armor(String name, String desc, Rarity rarity, int value,
                 int defBonus, int spdBonus, String allowedClass) {
        super(name, desc, rarity, value);
        this.defBonus = defBonus; this.spdBonus = spdBonus;
        this.allowedClass = allowedClass;
    }

    public int getDefBonus()       { return defBonus; }
    public int getSpdBonus()       { return spdBonus; }
    public boolean fitsClass(String cls) {
        return allowedClass == null || allowedClass.equalsIgnoreCase(cls);
    }

    public static Armor leatherArmor() {
        return new Armor("Armadura de Cuero", "+4 DEF, +2 VEL. Ligera y silenciosa.",
            Rarity.COMMON, 50, 4, 2, "Pícaro");
    }
    public static Armor plateArmor() {
        return new Armor("Armadura de Placas", "+8 DEF. Pesada pero protectora.",
            Rarity.UNCOMMON, 100, 8, -1, "Guerrero");
    }
    public static Armor holyShield() {
        return new Armor("Escudo Bendito", "+6 DEF. Consagrado para el Paladín.",
            Rarity.RARE, 110, 6, 0, "Paladín");
    }
    public static Armor ironMail() {
        return new Armor("Cota de Malla", "+3 DEF. Protección básica.",
            Rarity.COMMON, 35, 3, 0, null);
    }
}
