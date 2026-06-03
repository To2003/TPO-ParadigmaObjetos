package com.dungeontales.core.model;

import java.io.Serializable;

/** Efecto de estado aplicable a personajes o enemigos. */
public class StatusEffect implements Serializable {
    public enum Type { POISON, STUN, DEFENSE_UP, ATTACK_UP, EVASION }

    private final Type type;
    private int duration;
    private final int value;

    public StatusEffect(Type type, int duration, int value) {
        this.type = type; this.duration = duration; this.value = value;
    }

    public Type getType()     { return type; }
    public int getDuration()  { return duration; }
    public int getValue()     { return value; }
    public boolean isActive() { return duration > 0; }
    public void tick()        { if (duration > 0) duration--; }

    public String getDisplayName() {
        return switch (type) {
            case POISON     -> "Veneno";
            case STUN       -> "Aturdido";
            case DEFENSE_UP -> "Escudo(+" + value + ")";
            case ATTACK_UP  -> value >= 0 ? "Furia(+" + value + ")" : "Debil(" + value + ")";
            case EVASION    -> "Evasión";
        };
    }

    @Override public String toString() {
        return getDisplayName() + "(" + duration + "t)";
    }
}
