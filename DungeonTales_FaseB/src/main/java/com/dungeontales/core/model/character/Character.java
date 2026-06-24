package com.dungeontales.core.model.character;

import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.StatusEffect;
import com.dungeontales.core.model.items.Armor;
import com.dungeontales.core.model.items.RareItem;
import com.dungeontales.core.model.items.Weapon;

import java.io.Serializable;
import java.util.*;

public abstract class Character implements Serializable {

    // si es true el engine lo mueve automáticamente (no el jugador)
    protected boolean npc = false;

    protected String name;
    protected String className;
    protected int hpMax, hp;
    protected int paMax, pa, paRegen;
    protected int baseAtk, baseDef, baseSpd;
    protected int level, exp, expToNext;

    protected Weapon equippedWeapon;
    protected Armor  equippedArmor;

    protected RareItem equippedRareWeapon;
    protected RareItem equippedRareArmor;

    // pasivos de combate — se resetean al inicio de cada batalla
    protected boolean arcaneShieldActive;    // Túnica del Tejedor — absorbe primer golpe
    protected boolean deathSavedThisCombat;  // Manto del Espectro — solo 1 vez por combate
    protected boolean deathSaveInvincible;   // Manto del Espectro — intocable el turno siguiente

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

    public int getEffectiveAtk() {
        int bonus = (equippedWeapon != null ? equippedWeapon.getAtkBonus() : 0);
        if (equippedRareWeapon != null) bonus += equippedRareWeapon.getAtkBonus();
        int effectBonus = effects.stream()
            .filter(e -> e.getType() == StatusEffect.Type.ATTACK_UP && e.isActive())
            .mapToInt(StatusEffect::getValue).sum();
        return baseAtk + bonus + effectBonus;
    }

    public int getEffectiveDef() {
        int bonus = (equippedArmor != null ? equippedArmor.getDefBonus() : 0);
        if (equippedRareArmor != null) bonus += equippedRareArmor.getDefBonus();
        int effectBonus = effects.stream()
            .filter(e -> e.getType() == StatusEffect.Type.DEFENSE_UP && e.isActive())
            .mapToInt(StatusEffect::getValue).sum();
        return baseDef + bonus + effectBonus;
    }

    public int getEffectiveSpd() {
        int bonus = (equippedArmor != null ? equippedArmor.getSpdBonus() : 0);
        if (equippedRareArmor != null) bonus += equippedRareArmor.getSpdBonus();
        if (equippedRareWeapon != null) bonus += equippedRareWeapon.getSpdBonus();
        return baseSpd + bonus;
    }

    public int getHpMax() {
        int extra = (equippedRareArmor != null ? equippedRareArmor.getHpMaxBonus() : 0);
        return hpMax + extra;
    }

    public int getPaRegen() {
        int extra = (equippedRareArmor != null ? equippedRareArmor.getPaRegenBonus() : 0);
        return paRegen + extra;
    }

    public void startCombat() {
        this.pa = 4;
        // Activar escudo arcano al inicio si porta la Túnica del Tejedor
        this.arcaneShieldActive   = hasPassive(RareItem.PassiveType.ARCANE_SHIELD);
        this.deathSavedThisCombat = false;
        this.deathSaveInvincible  = false;
    }

    public void startTurn() {
        pa = Math.min(paMax, pa + getPaRegen());
        // El turno de invencibilidad del Manto del Espectro expira
        deathSaveInvincible = false;
        Iterator<StatusEffect> it = effects.iterator();
        while (it.hasNext()) {
            StatusEffect e = it.next();
            e.tick();
            if (!e.isActive()) it.remove();
        }
    }

    /**
     * Aplica daño al personaje.
     * Retorna:
     *  -1  → ataque esquivado (evasión)
     *  -2  → golpe bloqueado (Escudo Arcano o invencibilidad del Manto)
     *  -3  → Manto del Espectro activado: sobrevivió con 1 HP (ahora intocable 1 turno)
     *  N>0 → daño real aplicado
     */
    public int receiveDamage(int rawDmg, boolean ignoreDefense) {
        // Evasión por efecto de estado
        if (hasEffect(StatusEffect.Type.EVASION) && Math.random() < 0.5) {
            removeEffect(StatusEffect.Type.EVASION);
            return -1;
        }
        // Invencibilidad post-death-save (1 turno)
        if (deathSaveInvincible) {
            return -2;
        }
        // Escudo Arcano (Túnica del Tejedor — bloquea 1 golpe al inicio del combate)
        if (arcaneShieldActive) {
            arcaneShieldActive = false;
            return -2;
        }

        int effectiveDef = ignoreDefense ? 0 : getEffectiveDef();
        int dmg = Math.max(1, rawDmg - effectiveDef / 2);

        // Death Save (Manto del Espectro — sobrevive 1 vez con 1 HP)
        if (hp - dmg <= 0 && !deathSavedThisCombat && hasPassive(RareItem.PassiveType.DEATH_SAVE)) {
            hp = 1;
            deathSavedThisCombat = true;
            deathSaveInvincible  = true;
            return -3;
        }

        hp = Math.max(0, hp - dmg);
        return dmg;
    }

    public int healHp(int amount) {
        int before = hp;
        hp = Math.min(getHpMax(), hp + amount);
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

    public boolean equipWeapon(Weapon w) {
        if (w.fitsClass(className)) { equippedWeapon = w; return true; }
        return false;
    }

    public boolean equipArmor(Armor a) {
        if (a.fitsClass(className)) { equippedArmor = a; return true; }
        return false;
    }

    public boolean equipRareWeapon(RareItem r) {
        if (r.getSlot() == RareItem.Slot.WEAPON && r.fitsClass(className)) {
            equippedRareWeapon = r; return true;
        }
        return false;
    }

    public boolean equipRareArmor(RareItem r) {
        if (r.getSlot() == RareItem.Slot.ARMOR && r.fitsClass(className)) {
            equippedRareArmor = r; return true;
        }
        return false;
    }

    public boolean hasPassive(RareItem.PassiveType type) {
        return (equippedRareWeapon != null && equippedRareWeapon.getPassive() == type)
            || (equippedRareArmor  != null && equippedRareArmor.getPassive()  == type);
    }

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
        hp      = Math.min(hp + hpGain, getHpMax());
        baseAtk += atkGain;
        baseDef += defGain;
    }

    public boolean isNpc()        { return npc; }
    public String getName()       { return name; }
    public String getClassName()  { return className; }
    public int getHp()            { return hp; }
    public int getPa()            { return pa; }
    public int getPaMax()         { return paMax; }
    public int getSpd()           { return getEffectiveSpd(); }
    public int getLevel()         { return level; }
    public int getExp()           { return exp; }
    public int getExpToNext()     { return expToNext; }
    public boolean isAlive()      { return hp > 0; }
    public boolean isStunned()    { return hasEffect(StatusEffect.Type.STUN); }
    public boolean isArcaneShieldActive()  { return arcaneShieldActive; }
    public boolean isDeathSaveInvincible() { return deathSaveInvincible; }
    public List<Ability>      getAbilities()    { return Collections.unmodifiableList(abilities); }
    public List<StatusEffect> getEffects()      { return Collections.unmodifiableList(effects); }
    public Weapon    getEquippedWeapon()         { return equippedWeapon; }
    public Armor     getEquippedArmor()          { return equippedArmor; }
    public RareItem  getEquippedRareWeapon()     { return equippedRareWeapon; }
    public RareItem  getEquippedRareArmor()      { return equippedRareArmor; }
}
