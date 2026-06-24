package com.dungeontales.core.model.items;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Pool global de ítems raros/épicos.
 * - Cada ítem existe una sola vez: una vez otorgado se elimina del pool.
 * - Nunca se otorgan dos ítems en el mismo evento.
 * - Probabilidades: Boss = garantizado, Mini-boss = 40% (100% si perfect clear),
 *   Tesoro = 20%, Tienda = 25% de aparecer.
 */
public class RareItemPool implements Serializable {

    private static final double PROB_MINIBOSS = 0.40;
    private static final double PROB_TREASURE = 0.20;
    private static final double PROB_SHOP     = 0.25;

    private static final Random RNG = new Random();

    private final List<RareItem> pool;

    public RareItemPool() {
        pool = new ArrayList<>();

        pool.add(new RareItem(
            "Beso de la Viuda",
            "Si el enemigo está envenenado, ignora el 50% de su armadura.",
            "Pícaro", RareItem.Slot.WEAPON,
            RareItem.PassiveType.POISON_ARMOR_PIERCE,
            8, 0, 0, 0, 0));

        pool.add(new RareItem(
            "Manto del Espectro",
            "Una vez por combate, sobrevive con 1 HP al golpe letal y es intocable 1 turno.",
            "Pícaro", RareItem.Slot.ARMOR,
            RareItem.PassiveType.DEATH_SAVE,
            0, 5, 4, 0, 0));

        pool.add(new RareItem(
            "Veredicto del Alba",
            "El daño escala con la armadura de Aldric. Cura al aliado más débil un 10% del daño.",
            "Paladín", RareItem.Slot.WEAPON,
            RareItem.PassiveType.DEFENSE_SCALES_DAMAGE,
            0, 0, 0, 0, 0));

        pool.add(new RareItem(
            "Égida del Mártir",
            "Absorbe automáticamente el 20% del daño directo que reciban los aliados.",
            "Paladín", RareItem.Slot.ARMOR,
            RareItem.PassiveType.DAMAGE_SHARE,
            0, 12, 0, 30, 0));

        pool.add(new RareItem(
            "Devastadora de Huesos",
            "Sed de Sangre: el daño aumenta un 2% por cada 1% de HP faltante.",
            "Guerrero", RareItem.Slot.WEAPON,
            RareItem.PassiveType.BERSERKER,
            9, 0, 0, 0, 0));

        pool.add(new RareItem(
            "Hombreras del Gladiador Caído",
            "Espinas: devuelve el 30% del daño melee recibido directamente al atacante.",
            "Guerrero", RareItem.Slot.ARMOR,
            RareItem.PassiveType.THORNS,
            0, 10, 0, 25, 0));

        pool.add(new RareItem(
            "Báculo del Vacío Inestable",
            "Las habilidades cuestan 1 PA menos, pero cada hechizo inflige recoil a Santi.",
            "Mago", RareItem.Slot.WEAPON,
            RareItem.PassiveType.PA_DISCOUNT_RECOIL,
            10, 0, 0, 0, 0));

        pool.add(new RareItem(
            "Túnica del Tejedor de Estrellas",
            "Al inicio del combate, un escudo arcano bloquea por completo el primer ataque recibido.",
            "Mago", RareItem.Slot.ARMOR,
            RareItem.PassiveType.ARCANE_SHIELD,
            0, 4, 0, 0, 2));
    }


    public boolean isEmpty() { return pool.isEmpty(); }
    public int size()        { return pool.size(); }
    public List<RareItem> getAll() { return List.copyOf(pool); }

    /** Boss: drop garantizado. */
    public Optional<RareItem> rollBoss() {
        return rollGuaranteed();
    }

    /**
     * Mini-boss: 40% de probabilidad.
     * Si perfectClear == true (ningún personaje recibió daño), drop garantizado.
     */
    public Optional<RareItem> rollMiniBoss(boolean perfectClear) {
        return perfectClear ? rollGuaranteed() : roll(PROB_MINIBOSS);
    }

    /** Sala del tesoro: 20% de probabilidad. */
    public Optional<RareItem> rollTreasure() {
        return roll(PROB_TREASURE);
    }

    /**
     * Tienda: 25% de chance de aparecer 1 ítem épico a la venta.
     * NO elimina el ítem del pool — llama a remove() cuando el jugador lo compra.
     */
    public Optional<RareItem> peekShop() {
        if (pool.isEmpty() || RNG.nextDouble() > PROB_SHOP) return Optional.empty();
        return Optional.of(pool.get(RNG.nextInt(pool.size())));
    }

    /** Elimina un ítem del pool (al ser otorgado o comprado). */
    public void remove(RareItem item) {
        pool.remove(item);
    }


    private Optional<RareItem> rollGuaranteed() {
        if (pool.isEmpty()) return Optional.empty();
        return Optional.of(pool.remove(RNG.nextInt(pool.size())));
    }

    private Optional<RareItem> roll(double probability) {
        if (pool.isEmpty() || RNG.nextDouble() > probability) return Optional.empty();
        return Optional.of(pool.remove(RNG.nextInt(pool.size())));
    }
}
