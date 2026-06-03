package com.dungeontales.core.model.enemy;

import com.dungeontales.core.model.StatusEffect;
import com.dungeontales.core.model.character.Character;

import java.io.Serializable;
import java.util.*;

public abstract class Enemy implements Serializable {
    protected String name;
    protected int hpMax, hp, atk, def, spd;
    protected int expReward, goldReward;
    protected final List<StatusEffect> effects = new ArrayList<>();
    protected static final Random RNG = new Random();

    protected Enemy(String name, int hp, int atk, int def, int spd, int exp, int gold) {
        this.name = name; this.hpMax = hp; this.hp = hp;
        this.atk = atk; this.def = def; this.spd = spd;
        this.expReward = exp; this.goldReward = gold;
    }

    public abstract String decideAction();
    public abstract String getSpriteName();

    public void startTurn() {
        Iterator<StatusEffect> it = effects.iterator();
        while (it.hasNext()) {
            StatusEffect e = it.next();
            e.tick();
            if (!e.isActive()) it.remove();
        }
    }

    // Daño que este enemigo haría a un personaje
    public int calculateRawDamage() { return atk + RNG.nextInt(5) - 2; }

    public int receiveDamage(int rawDmg, boolean ignoreDefense) {
        int effectiveDef = ignoreDefense ? def / 4 : def;
        int dmg = Math.max(1, rawDmg - effectiveDef / 2);
        hp = Math.max(0, hp - dmg);
        return dmg;
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

    public boolean isAlive()   { return hp > 0; }
    public boolean isStunned() { return hasEffect(StatusEffect.Type.STUN); }

    public String getName()    { return name; }
    public int getHp()         { return hp; }
    public int getHpMax()      { return hpMax; }
    public int getAtk()        { return atk; }
    public int getDef()        { return def; }
    public int getSpd()        { return spd; }
    public int getExpReward()  { return expReward; }
    public int getGoldReward() { return goldReward; }
    public List<StatusEffect> getEffects() { return Collections.unmodifiableList(effects); }
}
