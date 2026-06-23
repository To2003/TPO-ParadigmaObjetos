package com.dungeontales.core.model.items;

/**
 * Ítem épico único por clase.
 * Cada pieza tiene stats base + un efecto pasivo especial que se activa en combate.
 */
public class RareItem extends Item {

    public enum Slot { WEAPON, ARMOR }

    public enum PassiveType {
        NONE,
        /** Beso de la Viuda: si el enemigo está envenenado, ignora el 50% de su armadura. */
        POISON_ARMOR_PIERCE,
        /** Manto del Espectro: una vez por combate, sobrevive con 1 HP al golpe letal + 1 turno intocable. */
        DEATH_SAVE,
        /** Veredicto del Alba: el daño escala con DEF propia (no ATK); cura al aliado más débil un 10%. */
        DEFENSE_SCALES_DAMAGE,
        /** Égida del Mártir: absorbe automáticamente el 20% del daño que reciban los aliados. */
        DAMAGE_SHARE,
        /** Devastadora de Huesos (Sed de Sangre): +2% de daño por cada 1% de HP faltante. */
        BERSERKER,
        /** Hombreras del Gladiador: devuelve el 30% del daño físico melee al atacante. */
        THORNS,
        /** Báculo del Vacío: habilidades cuestan 1 PA menos, pero cada hechizo causa recoil. */
        PA_DISCOUNT_RECOIL,
        /** Túnica del Tejedor: al inicio del combate, escudo arcano que absorbe el primer golpe. */
        ARCANE_SHIELD
    }

    private final Slot        slot;
    private final String      requiredClass;
    private final PassiveType passive;
    private final int         atkBonus;
    private final int         defBonus;
    private final int         spdBonus;
    private final int         hpMaxBonus;
    private final int         paRegenBonus;

    public RareItem(String name, String description, String requiredClass, Slot slot,
                    PassiveType passive, int atkBonus, int defBonus, int spdBonus,
                    int hpMaxBonus, int paRegenBonus) {
        super(name, description, Rarity.EPIC, 300);
        this.requiredClass = requiredClass;
        this.slot          = slot;
        this.passive       = passive;
        this.atkBonus      = atkBonus;
        this.defBonus      = defBonus;
        this.spdBonus      = spdBonus;
        this.hpMaxBonus    = hpMaxBonus;
        this.paRegenBonus  = paRegenBonus;
    }

    public Slot        getSlot()          { return slot; }
    public String      getRequiredClass() { return requiredClass; }
    public PassiveType getPassive()       { return passive; }
    public int         getAtkBonus()      { return atkBonus; }
    public int         getDefBonus()      { return defBonus; }
    public int         getSpdBonus()      { return spdBonus; }
    public int         getHpMaxBonus()    { return hpMaxBonus; }
    public int         getPaRegenBonus()  { return paRegenBonus; }

    public boolean hasPassive() { return passive != PassiveType.NONE; }

    public boolean fitsClass(String className) {
        return requiredClass.equalsIgnoreCase(className);
    }
}
