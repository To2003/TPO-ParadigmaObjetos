package com.dungeontales.core.model;

import java.io.Serializable;

/** Habilidad o ataque especial de un personaje. */
public class Ability implements Serializable {
    public enum TargetType { SINGLE_ENEMY, ALL_ENEMIES, SINGLE_ALLY, ALL_ALLIES, SELF }

    private final String name;
    private final String description;
    private final int paCost;
    private final TargetType targetType;
    private final double damageMultiplier;
    private final int healAmount;
    private final StatusEffect.Type effectType;
    private final int effectDuration;
    private final int effectValue;
    private final boolean ignoreDefense;
    private final boolean hitsAll;
    private final boolean doubleHit;

    private Ability(Builder b) {
        this.name             = b.name;
        this.description      = b.description;
        this.paCost           = b.paCost;
        this.targetType       = b.targetType;
        this.damageMultiplier = b.damageMultiplier;
        this.healAmount       = b.healAmount;
        this.effectType       = b.effectType;
        this.effectDuration   = b.effectDuration;
        this.effectValue      = b.effectValue;
        this.ignoreDefense    = b.ignoreDefense;
        this.hitsAll          = b.hitsAll;
        this.doubleHit        = b.doubleHit;
    }

    public String getName()             { return name; }
    public String getDescription()      { return description; }
    public int getPaCost()              { return paCost; }
    public TargetType getTargetType()   { return targetType; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public int getHealAmount()          { return healAmount; }
    public StatusEffect.Type getEffectType()  { return effectType; }
    public int getEffectDuration()      { return effectDuration; }
    public int getEffectValue()         { return effectValue; }
    public boolean isIgnoreDefense()    { return ignoreDefense; }
    public boolean isHitsAll()          { return hitsAll; }
    public boolean isDoubleHit()        { return doubleHit; }
    public boolean isDamage()           { return damageMultiplier > 0; }
    public boolean isHeal()             { return healAmount > 0; }
    public boolean hasEffect()          { return effectType != null; }

    public static class Builder {
        String name, description; int paCost;
        TargetType targetType = TargetType.SINGLE_ENEMY;
        double damageMultiplier = 0; int healAmount = 0;
        StatusEffect.Type effectType = null;
        int effectDuration = 0, effectValue = 0;
        boolean ignoreDefense = false, hitsAll = false, doubleHit = false;

        public Builder(String name, String desc, int pa) {
            this.name = name; this.description = desc; this.paCost = pa;
        }
        public Builder target(TargetType t)   { this.targetType = t; return this; }
        public Builder damage(double d)        { this.damageMultiplier = d; return this; }
        public Builder heal(int a)             { this.healAmount = a; return this; }
        public Builder effect(StatusEffect.Type t, int dur, int val) {
            this.effectType = t; this.effectDuration = dur; this.effectValue = val; return this;
        }
        public Builder ignoreDefense()         { this.ignoreDefense = true; return this; }
        public Builder hitsAll()               { this.hitsAll = true; return this; }
        public Builder doubleHit()             { this.doubleHit = true; return this; }
        public Ability build()                 { return new Ability(this); }
    }
}
