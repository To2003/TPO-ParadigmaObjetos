package com.dungeontales.core.model;

import com.dungeontales.core.model.items.Item;
import com.dungeontales.core.model.items.Potion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Inventario compartido de la party. */
public class Inventory implements Serializable {
    private static final int MAX_ITEMS      = 20;
    private static final int MAX_PER_POTION = 5;
    private final List<Item> items = new ArrayList<>();

    public boolean addItem(Item item) {
        if (items.size() >= MAX_ITEMS) return false;
        if (item instanceof Potion p && getPotionCount(p.getEffect()) >= MAX_PER_POTION) return false;
        items.add(item);
        return true;
    }

    public boolean removeItem(Item item) { return items.remove(item); }

    /** Cuántas pociones de ese efecto hay en el inventario. */
    public int getPotionCount(Potion.Effect effect) {
        return (int) items.stream()
            .filter(i -> i instanceof Potion p && p.getEffect() == effect)
            .count();
    }

    /** true si se puede agregar una poción de ese tipo (no alcanzó el límite de 5). */
    public boolean canAddPotion(Potion.Effect effect) {
        return getPotionCount(effect) < MAX_PER_POTION;
    }

    public List<Potion> getPotions() {
        return items.stream()
            .filter(i -> i instanceof Potion)
            .map(i -> (Potion) i)
            .toList();
    }

    public List<Item> getAllItems() { return Collections.unmodifiableList(items); }
    public int size()               { return items.size(); }
    public boolean isFull()         { return items.size() >= MAX_ITEMS; }
    public int getMaxPerPotion()    { return MAX_PER_POTION; }
}
