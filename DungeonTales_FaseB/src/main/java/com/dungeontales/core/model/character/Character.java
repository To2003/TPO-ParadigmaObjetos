package com.dungeontales.core.model.character;

import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.StatusEffect;
import com.dungeontales.core.model.items.Armor;
import com.dungeontales.core.model.items.Weapon;

import java.io.Serializable;
import java.util.*;

public abstract class Character implements Serializable {

    // Control: si es true el engine lo mueve automáticamente (no el jugador)
    protected boolean npc = false;

    // Stats base
    protected String name;
    protected String className;
    protected int hpMax, hp;
    protected int paMax, pa, paRegen;
    protected int baseAtk, baseDef, baseSpd;
    protected int level, exp, expToNext;

    // Equipamiento
    protected Weapon equippedWeapon;
    protected Armor  equippedArmor;

    // Habilidades y efectos
    protected final List<Ability>      abilities = new ArrayList<>();
    protected final List<StatusEffect> effects   = new ArrayList<>();

    protected static final Random RNG = new Random();

    protected Character(String name, String className,
                        int hp, int paMax, int paRegen, int atk, int def, int spd) {
        this.name = name; this.className = className;
        this.hpMax = hp; this.hp = hp;
        this.paMax = paMax; this.pa = 4; this.paRegen = paRegen;
        this.baseAtk = atk; this.baseDef = def; this.baseSpd = spd;
        this.level = 1; this.exp = 0; this.expToNext = 100;
        initAbilities();
    }

    protected abstract void initAbilities();
    public abstract String getSpriteName();

    // ── Stats efectivos (base + equipo + efectos) ──────────────────────────
    public int getEffectiveAtk() {
        int bonus = (equippedWeapon != null ? equippedWeapon.getAtkBonus() : 0);
        int effectBonus = effects.stream()
            .filter(e -> e.getType() == StatusEffect.Type.ATTACK_UP && e.isActive())
            .mapToInt(StatusEffect::getValue).sum();
        return baseAtk + bonus + effectBonus;
    }

    public int getEffectiveDef() {
        int bonus = (equippedArmor != null ? equippedArmor.getDefBonus() : 0);
        int effectBonus = effects.stream()
            .filter(e -> e.getType() == StatusEffect.Type.DEFENSE_UP && e.isActive())
            .mapToInt(StatusEffect::getValue).sum();
        return baseDef + bonus + effectBonus;
    }

    public int getEffectiveSpd() {
        int bonus = (equippedArmor != null ? equippedArmor.getSpdBonus() : 0);
        return baseSpd + bonus;
    }

    // ── Combate ───────────────────────────────────────────────────────────
    public void startCombat() {
        this.pa = 4;
    }

    public void startTurn() {
        pa = Math.min(paMax, pa + paRegen);
        Iterator<StatusEffect> it = effects.iterator();
        while (it.hasNext()) {
            StatusEffect e = it.next();
            e.tick();
            if (!e.isActive()) it.remove();
        }
    }

    public int receiveDamage(int rawDmg, boolean ignoreDefense) {
        if (hasEffect(StatusEffect.Type.EVASION) && Math.random() < 0.5) {
            removeEffect(StatusEffect.Type.EVASION);
            return -1; // -1 = esquivado
        }
        int effectiveDef = ignoreDefense ? 0 : getEffectiveDef();
        int dmg = Math.max(1, rawDmg - effectiveDef / 2);
        hp = Math.max(0, hp - dmg);
        return dmg;
    }

    public int healHp(int amount) {
        int before = hp;
        hp = Math.min(hpMax, hp + amount);
        return hp - before;
    }

    public void applyEffect(StatusEffect effect) {
        effects.removeIf(e -> e.getType() == effect.getType());
        effects.add(effect);
    }

    public boolean hasEffect(StatusEffect.Type type) {
        return effects.stream().anyMatch(e -> e.getType() == type && e.isActive());
    }

    public void removeEffect(StatusEffect.Type type) {
        effects.removeIf(e -> e.getType() == type);
    }

    public boolean consumePA(int cost) {
        if (pa >= cost) { pa -= cost; return true; }
        return false;
    }

    public int restorePA(int amount) {
        int before = pa;
        pa = Math.min(paMax, pa + amount);
        return pa - before;
    }

    // ── Equipo ────────────────────────────────────────────────────────────
    public boolean equipWeapon(Weapon w) {
        if (w.fitsClass(className)) { equippedWeapon = w; return true; }
        return false;
    }

    public boolean equipArmor(Armor a) {
        if (a.fitsClass(className)) { equippedArmor = a; return true; }
        return false;
    }

    // ── Progresión ────────────────────────────────────────────────────────
    public boolean gainExp(int amount) {
        exp += amount;
        if (exp >= expToNext) {
            exp -= expToNext;
            levelUp();
            return true;
        }
        return false;
    }

    protected void levelUp() {
        level++;
        expToNext = (int)(expToNext * 1.5);
        int hpGain  = 10 + RNG.nextInt(6);
        int atkGain = 1  + RNG.nextInt(3);
        int defGain = 1  + RNG.nextInt(2);
        hpMax  += hpGain;
        hp      = Math.min(hp + hpGain, hpMax);
        baseAtk += atkGain;
        baseDef += defGain;
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public boolean isNpc()        { return npc; }
    public String getName()      { return name; }
    public String getClassName() { return className; }
    public int getHp()           { return hp; }
    public int getHpMax()        { return hpMax; }
    public int getPa()           { return pa; }
    public int getPaMax()        { return paMax; }
    public int getPaRegen()      { return paRegen; }
    public int getSpd()          { return getEffectiveSpd(); }
    public int getLevel()        { return level; }
    public int getExp()          { return exp; }
    public int getExpToNext()    { return expToNext; }
    public boolean isAlive()     { return hp > 0; }
    public boolean isStunned()   { return hasEffect(StatusEffect.Type.STUN); }
    public List<Ability>      getAbilities() { return Collections.unmodifiableList(abilities); }
    public List<StatusEffect> getEffects()   { return Collections.unmodifiableList(effects); }
    public Weapon getEquippedWeapon()        { return equippedWeapon; }
    public Armor  getEquippedArmor()         { return equippedArmor; }
}
