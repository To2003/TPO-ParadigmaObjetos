package com.dungeontales.core.model.items;

/** Arma equipable: aumenta ATK y puede cambiar tipo de daño. */
public class Weapon extends Item {
    private final int atkBonus;
    private final String allowedClass; // null = cualquier clase

    public Weapon(String name, String desc, Rarity rarity, int value,
                int atkBonus, String allowedClass) {
        super(name, desc, rarity, value);
        this.atkBonus = atkBonus;
        this.allowedClass = allowedClass;
    }

    public int getAtkBonus()      { return atkBonus; }
    public String getAllowedClass(){ return allowedClass; }
    public boolean fitsClass(String cls) {
        return allowedClass == null || allowedClass.equalsIgnoreCase(cls);
    }

    // Fábrica con ítems predefinidos
    public static Weapon daggerOfAssassin() {
        return new Weapon("Daga del Asesino", "+5 ATK. Diseñada para el Pícaro.",
            Rarity.UNCOMMON, 80, 5, "Pícaro");
    }
    public static Weapon holyBlade() {
        return new Weapon("Espada Sagrada", "+4 ATK. Brilla con luz divina.",
            Rarity.UNCOMMON, 90, 4, "Paladín");
    }
    public static Weapon warAxe() {
        return new Weapon("Hacha de Guerra", "+6 ATK. Pesada pero devastadora.",
            Rarity.RARE, 120, 6, "Guerrero");
    }
    public static Weapon ironSword() {
        return new Weapon("Espada de Hierro", "+3 ATK. Arma básica de cualquier aventurero.",
            Rarity.COMMON, 40, 3, null);
    }
}
