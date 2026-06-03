package com.dungeontales.core.model;

import com.dungeontales.core.model.items.Item;
import com.dungeontales.core.model.items.Potion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Inventario compartido de la party. */
public class Inventory implements Serializable {
    private static final int MAX_ITEMS = 20;
    private final List<Item> items = new ArrayList<>();

    public boolean addItem(Item item) {
        if (items.size() >= MAX_ITEMS) return false;
        items.add(item);
        return true;
    }

    public boolean removeItem(Item item) { return items.remove(item); }

    public List<Potion> getPotions() {
        return items.stream()
            .filter(i -> i instanceof Potion)
            .map(i -> (Potion) i)
            .toList();
    }

    public List<Item> getAllItems() { return Collections.unmodifiableList(items); }
    public int size()               { return items.size(); }
    public boolean isFull()         { return items.size() >= MAX_ITEMS; }
}
