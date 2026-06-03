package com.dungeontales.core.battle;

import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.StatusEffect;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.core.model.enemy.Enemy;
import com.dungeontales.core.model.items.Potion;

import java.util.*;

/**
 * Motor de combate desacoplado de la UI.
 * Cada acción devuelve una lista de BattleEvent que la pantalla anima.
 */
public class BattleEngine {

    private final List<Character> party;
    private final List<Enemy>     enemies;
    private List<Object>          turnOrder;
    private int                   turnIndex;
    private int                   turnNumber;
    private boolean               battleOver;
    private boolean                playerWon;

    public BattleEngine(List<Character> party, List<Enemy> enemies) {
        this.party   = party;
        this.enemies = enemies;
        this.turnNumber = 1;
        buildTurnOrder();
        party.forEach(Character::startCombat);
        advanceToNextCombatant();
    }

    // ── Inicialización ─────────────────────────────────────────────────────

    private void buildTurnOrder() {
        turnOrder = new ArrayList<>();
        party.stream().filter(Character::isAlive).forEach(turnOrder::add);
        enemies.stream().filter(Enemy::isAlive).forEach(turnOrder::add);
        turnOrder.sort((a, b) -> Integer.compare(getSpd(b), getSpd(a)));
        turnIndex = 0;
    }

    private int getSpd(Object o) {
        return o instanceof Character c ? c.getSpd() : ((Enemy)o).getSpd();
    }

    /** Avanza al siguiente combatiente vivo y retorna los eventos de inicio de turno. */
    public List<BattleEvent> advanceToNextCombatant() {
        List<BattleEvent> events = new ArrayList<>();
        if (battleOver) return events;

        // Reconstruir orden si todos los del turno actual se acabaron
        boolean cycled = false;
        int attempts = 0;
        while (attempts < turnOrder.size() * 2) {
            if (turnIndex >= turnOrder.size()) {
                turnIndex = 0;
                turnNumber++;
                buildTurnOrder();
                cycled = true;
            }

            Object current = turnOrder.get(turnIndex);
            boolean alive = current instanceof Character c ? c.isAlive() : ((Enemy)current).isAlive();
            if (!alive) { turnIndex++; attempts++; continue; }

            // Aplicar efectos de inicio de turno
            events.addAll(applyStartOfTurnEffects(current));
            events.add(BattleEvent.turnStart(getName(current)));

            // Verificar si murio por efectos de turno
            if (!isAlive(current)) {
                events.add(current instanceof Character
                    ? BattleEvent.characterDied(getName(current))
                    : BattleEvent.enemyDied(getName(current)));
                checkBattleOver(events);
                turnIndex++;
                if (!battleOver) events.addAll(advanceToNextCombatant());
            }
            return events;
        }
        return events;
    }

    private List<BattleEvent> applyStartOfTurnEffects(Object combatant) {
        List<BattleEvent> events = new ArrayList<>();
        if (combatant instanceof Character c) {
            c.startTurn();
            // Veneno
            if (c.hasEffect(StatusEffect.Type.POISON)) {
                // El tick ya se aplicó en startTurn, pero el daño se aplicó antes
                // Necesitamos trackear el daño de veneno
            }
        } else if (combatant instanceof Enemy e) {
            // Troll regeneration
            if (e.getSpriteName().equals("troll")) {
                int regenBefore = e.getHp();
                e.startTurn();
                int regenAmount = e.getHp() - regenBefore;
                if (regenAmount > 0)
                    events.add(BattleEvent.regenTick(e.getName(), regenAmount));
            } else {
                e.startTurn();
            }
        }
        return events;
    }

    // ── Turno del jugador ──────────────────────────────────────────────────

    public Object getCurrentCombatant() {
        if (turnIndex >= turnOrder.size()) return null;
        return turnOrder.get(turnIndex);
    }

    public boolean isPlayerTurn() {
        Object c = getCurrentCombatant();
        return c instanceof Character;
    }

    /** Ataque básico del personaje actual. */
    public List<BattleEvent> playerBasicAttack(Enemy target) {
        List<BattleEvent> events = new ArrayList<>();
        Character actor = (Character) getCurrentCombatant();

        int raw  = actor.getEffectiveAtk() + new Random().nextInt(5) - 2;
        int dmg  = target.receiveDamage(raw, false);
        events.add(dmg == -1
            ? BattleEvent.dodged(target.getName())
            : BattleEvent.damage(actor.getName(), target.getName(), dmg, false));

        if (!target.isAlive()) {
            events.add(BattleEvent.enemyDied(target.getName()));
            checkBattleOver(events);
        }

        turnIndex++;
        if (!battleOver) {
            events.addAll(processUntilPlayerTurn());
        }
        return events;
    }

    /** Usa una habilidad del personaje actual. */
    public List<BattleEvent> playerUseAbility(Ability ability,
                                               Enemy singleEnemy,
                                               Character singleAlly) {
        List<BattleEvent> events = new ArrayList<>();
        Character actor = (Character) getCurrentCombatant();

        if (!actor.consumePA(ability.getPaCost())) {
            events.add(BattleEvent.log("PA insuficiente para " + ability.getName()
                + " (necesitas " + ability.getPaCost() + " PA)"));
            return events;
        }

        events.add(BattleEvent.log(actor.getName() + " usa " + ability.getName() + "!"));

        switch (ability.getTargetType()) {
            case SINGLE_ENEMY -> {
                if (singleEnemy != null)
                    events.addAll(applyDamageAbility(actor, ability, singleEnemy));
            }
            case ALL_ENEMIES -> {
                enemies.stream().filter(Enemy::isAlive).forEach(e ->
                    events.addAll(applyDamageAbility(actor, ability, e)));
            }
            case SINGLE_ALLY -> {
                Character target = singleAlly != null ? singleAlly : actor;
                if (ability.isHeal()) {
                    int healed = target.healHp(ability.getHealAmount());
                    events.add(BattleEvent.heal(actor.getName(), target.getName(), healed));
                }
                if (ability.hasEffect())
                    events.addAll(applyEffect(target.getName(), ability, target, null));
            }
            case ALL_ALLIES -> {
                party.stream().filter(Character::isAlive).forEach(a -> {
                    if (ability.hasEffect())
                        events.addAll(applyEffect(a.getName(), ability, a, null));
                });
            }
            case SELF -> {
                if (ability.hasEffect())
                    events.addAll(applyEffect(actor.getName(), ability, actor, null));
            }
        }

        checkBattleOver(events);
        return events;
    }

    /** Usar una poción del inventario. */
    public List<BattleEvent> playerUsePotion(Potion potion, Character target) {
        List<BattleEvent> events = new ArrayList<>();
        Character actor = (Character) getCurrentCombatant();

        switch (potion.getEffect()) {
            case HEAL_SMALL, HEAL_LARGE -> {
                int healed = target.healHp(potion.getPower());
                events.add(BattleEvent.heal(actor.getName(), target.getName(), healed));
            }
            case ANTIDOTE -> {
                target.removeEffect(StatusEffect.Type.POISON);
                events.add(BattleEvent.log(target.getName() + ": veneno curado"));
            }
            case STRENGTH -> {
                target.applyEffect(new StatusEffect(StatusEffect.Type.ATTACK_UP, 2, potion.getPower()));
                events.add(BattleEvent.status(target.getName(), "Fuerza (+" + potion.getPower() + " ATK)"));
            }
            case REVIVE -> {
                if (!target.isAlive()) {
                    int reviveHp = (int)(target.getHpMax() * potion.getPower() / 100.0);
                    target.healHp(reviveHp);
                    events.add(BattleEvent.heal("Elixir", target.getName(), reviveHp));
                }
            }
        }
        return events;
    }

    /** El jugador termina su turno sin gastar todos los PA. */
    public List<BattleEvent> playerEndTurn() {
        List<BattleEvent> events = new ArrayList<>();
        turnIndex++;
        if (!battleOver) events.addAll(processUntilPlayerTurn());
        return events;
    }

    // ── Turno del enemigo ──────────────────────────────────────────────────

    /**
     * Procesa turnos de enemigos automáticamente hasta que llegue
     * el turno de algún personaje del jugador. Devuelve todos los eventos.
     */
    public List<BattleEvent> processUntilPlayerTurn() {
        List<BattleEvent> events = new ArrayList<>();
        if (battleOver) return events;

        events.addAll(advanceToNextCombatant());

        while (!battleOver && !isPlayerTurn()) {
            Object current = getCurrentCombatant();
            if (current instanceof Enemy e) {
                if (e.isStunned()) {
                    e.removeEffect(StatusEffect.Type.STUN);
                    events.add(BattleEvent.stunned(e.getName(), false));
                } else {
                    events.addAll(processEnemyAction(e));
                }
            }
            turnIndex++;
            if (!battleOver) events.addAll(advanceToNextCombatant());
        }
        return events;
    }

    private List<BattleEvent> processEnemyAction(Enemy e) {
        List<BattleEvent> events = new ArrayList<>();
        List<Character> alive = party.stream().filter(Character::isAlive).toList();
        if (alive.isEmpty()) return events;

        Character target = alive.get(new Random().nextInt(alive.size()));
        String action = e.decideAction();

        switch (action) {
            case "attack" -> {
                int raw = e.calculateRawDamage();
                int dmg = target.receiveDamage(raw, false);
                events.add(dmg == -1
                    ? BattleEvent.dodged(target.getName())
                    : BattleEvent.damage(e.getName(), target.getName(), dmg, false));
            }
            case "double_attack" -> {
                events.add(BattleEvent.log(e.getName() + ": ¡Doble Ataque!"));
                for (int i = 0; i < 2; i++) {
                    Character t = alive.get(new Random().nextInt(alive.size()));
                    int raw = (int)(e.calculateRawDamage() * 0.7);
                    int dmg = t.receiveDamage(raw, false);
                    events.add(dmg == -1
                        ? BattleEvent.dodged(t.getName())
                        : BattleEvent.damage(e.getName(), t.getName(), dmg, false));
                }
            }
            case "heavy_strike", "smash" -> {
                events.add(BattleEvent.log(e.getName() + ": ¡Golpe Aplastante!"));
                int raw = (int)(e.calculateRawDamage() * 1.6);
                int dmg = target.receiveDamage(raw, false);
                events.add(dmg == -1
                    ? BattleEvent.dodged(target.getName())
                    : BattleEvent.damage(e.getName(), target.getName(), dmg, false));
            }
            case "bone_throw" -> {
                int raw = (int)(e.getAtk() * 0.9);
                int dmg = target.receiveDamage(raw, true);
                events.add(BattleEvent.damage(e.getName(), target.getName(), dmg, false));
            }
            case "roar" -> {
                events.add(BattleEvent.log(e.getName() + ": ¡Rugido Aterrador! (−3 ATK party)"));
                party.stream().filter(Character::isAlive).forEach(p ->
                    p.applyEffect(new StatusEffect(StatusEffect.Type.ATTACK_UP, 1, -3)));
            }
            default -> {
                int raw = e.calculateRawDamage();
                int dmg = target.receiveDamage(raw, false);
                events.add(dmg == -1
                    ? BattleEvent.dodged(target.getName())
                    : BattleEvent.damage(e.getName(), target.getName(), dmg, false));
            }
        }

        if (!target.isAlive()) {
            events.add(BattleEvent.characterDied(target.getName()));
        }
        checkBattleOver(events);
        return events;
    }

    // ── Helpers internos ──────────────────────────────────────────────────

    private List<BattleEvent> applyDamageAbility(Character actor, Ability ab, Enemy target) {
        List<BattleEvent> events = new ArrayList<>();
        if (!ab.isDamage()) return events;

        int raw = (int)(actor.getEffectiveAtk() * ab.getDamageMultiplier());

        // Bonus de Golpe Furtivo si el objetivo está debilitado
        if (ab.getName().equals("Golpe Furtivo") &&
            (target.hasEffect(StatusEffect.Type.POISON) || target.hasEffect(StatusEffect.Type.STUN))) {
            raw = (int)(raw * 1.5);
            events.add(BattleEvent.log("¡Golpe Furtivo potenciado!"));
        }

        int dmg = target.receiveDamage(raw, ab.isIgnoreDefense());
        events.add(dmg == -1
            ? BattleEvent.dodged(target.getName())
            : BattleEvent.damage(actor.getName(), target.getName(), dmg, ab.isIgnoreDefense()));

        // Segundo golpe (Doble Ataque)
        if (ab.isDoubleHit()) {
            int raw2 = (int)(actor.getEffectiveAtk() * ab.getDamageMultiplier());
            int dmg2 = target.receiveDamage(raw2, false);
            if (dmg2 != -1) events.add(BattleEvent.damage(actor.getName(), target.getName(), dmg2, false));
        }

        // Efecto de estado sobre el objetivo
        if (ab.hasEffect()) events.addAll(applyEffect(target.getName(), ab, null, target));

        if (!target.isAlive()) events.add(BattleEvent.enemyDied(target.getName()));
        return events;
    }

    private List<BattleEvent> applyEffect(String targetName, Ability ab,
                                           Character charTarget, Enemy enemyTarget) {
        List<BattleEvent> events = new ArrayList<>();
        StatusEffect effect = new StatusEffect(ab.getEffectType(), ab.getEffectDuration(), ab.getEffectValue());
        if (charTarget  != null) charTarget.applyEffect(effect);
        if (enemyTarget != null) enemyTarget.applyEffect(effect);
        events.add(BattleEvent.status(targetName, effect.getDisplayName()));
        return events;
    }

    private void checkBattleOver(List<BattleEvent> events) {
        if (battleOver) return;
        boolean allEnemiesDead = enemies.stream().noneMatch(Enemy::isAlive);
        boolean allPartyDead   = party.stream().noneMatch(Character::isAlive);

        if (allEnemiesDead) {
            battleOver = true; playerWon = true;
            events.add(BattleEvent.won());
        } else if (allPartyDead) {
            battleOver = true; playerWon = false;
            events.add(BattleEvent.lost());
        }
    }

    private boolean isAlive(Object o) {
        return o instanceof Character c ? c.isAlive() : ((Enemy)o).isAlive();
    }

    private String getName(Object o) {
        return o instanceof Character c ? c.getName() : ((Enemy)o).getName();
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public boolean isBattleOver()  { return battleOver; }
    public boolean isPlayerWon()   { return playerWon; }
    public int getTurnNumber()      { return turnNumber; }
    public List<Object> getTurnOrder() { return Collections.unmodifiableList(turnOrder); }
    public int getTurnIndex() { return turnIndex; }

    public List<BattleEvent> getStartEvents() {
        List<BattleEvent> events = new ArrayList<>();
        StringBuilder order = new StringBuilder("Orden de turno: ");
        turnOrder.forEach(o -> order.append(getName(o)).append(" → "));
        events.add(BattleEvent.log(order.toString()));
        return events;
    }
}
