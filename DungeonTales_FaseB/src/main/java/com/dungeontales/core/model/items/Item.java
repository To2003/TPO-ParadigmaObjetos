package com.dungeontales.core.model.items;

import java.io.Serializable;

/** Item base abstracto. */
public abstract class Item implements Serializable {
    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC }

    protected String name;
    protected String description;
    protected Rarity rarity;
    protected int value; // precio en oro

    protected Item(String name, String description, Rarity rarity, int value) {
        this.name = name; this.description = description;
        this.rarity = rarity; this.value = value;
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public Rarity getRarity()      { return rarity; }
    public int getValue()          { return value; }

    public java.awt.Color getRarityColor() {
        return switch (rarity) {
            case COMMON   -> new java.awt.Color(0x90, 0x90, 0x90);
            case UNCOMMON -> new java.awt.Color(0x40, 0xC0, 0x40);
            case RARE     -> new java.awt.Color(0x40, 0x80, 0xFF);
            case EPIC     -> new java.awt.Color(0xC0, 0x40, 0xE0);
        };
    }

    @Override public String toString() { return name + " [" + rarity + "]"; }
}
