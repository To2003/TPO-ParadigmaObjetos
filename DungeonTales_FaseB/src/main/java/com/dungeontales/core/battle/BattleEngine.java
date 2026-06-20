package com.dungeontales.core.battle;

import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.Inventory;
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
    private final Inventory       inventory;
    private List<Object>          turnOrder;
    private int                   turnIndex;
    private int                   turnNumber;
    private boolean               battleOver;
    private boolean               playerWon;

    public BattleEngine(List<Character> party, List<Enemy> enemies, Inventory inventory) {
        this.party     = party;
        this.enemies   = enemies;
        this.inventory = inventory;
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
        return c instanceof Character ch && !ch.isNpc();
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
                    events.addAll(applyEffect(actor.getName(), target.getName(), ability, target, null));
            }
            case ALL_ALLIES -> {
                party.stream().filter(Character::isAlive).forEach(a -> {
                    if (ability.hasEffect())
                        events.addAll(applyEffect(actor.getName(), a.getName(), ability, a, null));
                });
            }
            case SELF -> {
                if (ability.hasEffect())
                    events.addAll(applyEffect(actor.getName(), actor.getName(), ability, actor, null));
            }
        }

        checkBattleOver(events);
        return events;
    }

    /** Usar una poción del inventario. */
    public List<BattleEvent> playerUsePotion(Potion potion, Character target) {
        Character actor = (Character) getCurrentCombatant();
        return applyPotionEffect(actor.getName(), potion, target);
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
            } else if (current instanceof Character ch && ch.isNpc()) {
                if (ch.isStunned()) {
                    ch.removeEffect(StatusEffect.Type.STUN);
                    events.add(BattleEvent.stunned(ch.getName(), true));
                } else {
                    events.addAll(processNpcCharacterAction(ch));
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

    // ── IA de personajes NPC (Santi) ──────────────────────────────────────

    private List<BattleEvent> processNpcCharacterAction(Character npc) {
        List<BattleEvent> events = new ArrayList<>();
        List<Enemy>     aliveEnemies = enemies.stream().filter(Enemy::isAlive).toList();
        List<Character> aliveParty   = party.stream().filter(Character::isAlive).toList();

        if (aliveEnemies.isEmpty()) return events;

        int roll = new Random().nextInt(100);

        // 20%: confundido, pierde el turno
        if (roll < 20) {
            events.add(BattleEvent.log(npc.getName() + " está confundido y no hace nada..."));
            return events;
        }

        // 8%: usa poción aleatoria del inventario en objetivo aleatorio de la party
        if (roll < 28 && inventory != null && !inventory.getPotions().isEmpty()) {
            List<Potion> available = inventory.getPotions();
            Potion pot = available.get(new Random().nextInt(available.size()));
            Character target = aliveParty.get(new Random().nextInt(aliveParty.size()));
            inventory.removeItem(pot);
            events.add(BattleEvent.log(npc.getName() + " usa " + pot.getName()
                + " en " + target.getName() + " al azar!"));
            events.addAll(applyPotionEffect(npc.getName(), pot, target));
            return events;
        }

        // 52%: intentar usar habilidad aleatoria
        if (roll < 80) {
            List<Ability> affordable = npc.getAbilities().stream()
                .filter(a -> npc.getPa() >= a.getPaCost()).toList();
            if (!affordable.isEmpty()) {
                Ability ab = affordable.get(new Random().nextInt(affordable.size()));
                npc.consumePA(ab.getPaCost());
                events.add(BattleEvent.log(npc.getName() + " lanza " + ab.getName() + "!"));
                events.addAll(applyNpcAbility(npc, ab, aliveEnemies, aliveParty));
                checkBattleOver(events);
                return events;
            }
        }

        // 20% o sin PA: ataque básico con objetivo caótico
        events.addAll(applyNpcBasicAttack(npc, aliveEnemies, aliveParty));
        checkBattleOver(events);
        return events;
    }

    /** Elige objetivo caótico y aplica la habilidad. */
    private List<BattleEvent> applyNpcAbility(Character npc, Ability ab,
                                               List<Enemy> aliveEnemies, List<Character> aliveParty) {
        List<BattleEvent> events = new ArrayList<>();
        Random rand = new Random();

        switch (ab.getTargetType()) {
            case ALL_ENEMIES -> {
                // Siempre golpea todos los enemigos
                for (Enemy e : aliveEnemies)
                    events.addAll(applyDamageAbility(npc, ab, e));
                // 30% chance: también golpea a 1 aliado random (excepto a sí mismo)
                List<Character> others = aliveParty.stream().filter(c -> c != npc).toList();
                if (rand.nextInt(100) < 30 && !others.isEmpty()) {
                    Character unlucky = others.get(rand.nextInt(others.size()));
                    events.add(BattleEvent.log("¡La magia de " + npc.getName() + " se descontrola!"));
                    events.addAll(applyDamageAbilityToAlly(npc, ab, unlucky));
                }
            }
            case SINGLE_ENEMY -> {
                // 65% → enemigo, 25% → aliado, 10% → sí mismo
                int targetRoll = rand.nextInt(100);
                if (targetRoll < 65 && !aliveEnemies.isEmpty()) {
                    Enemy t = aliveEnemies.get(rand.nextInt(aliveEnemies.size()));
                    events.addAll(applyDamageAbility(npc, ab, t));
                } else if (targetRoll < 90) {
                    List<Character> others = aliveParty.stream()
                        .filter(c -> c != npc).toList();
                    if (!others.isEmpty()) {
                        events.add(BattleEvent.log(npc.getName() + " apunta mal!"));
                        events.addAll(applyDamageAbilityToAlly(npc, ab, others.get(rand.nextInt(others.size()))));
                    } else {
                        events.addAll(applyDamageAbilityToAlly(npc, ab, npc));
                    }
                } else {
                    events.add(BattleEvent.log(npc.getName() + " se hechiza a sí mismo!"));
                    events.addAll(applyDamageAbilityToAlly(npc, ab, npc));
                }
            }
            default -> {
                // Para cualquier otro tipo, atacar enemigo aleatorio
                if (!aliveEnemies.isEmpty())
                    events.addAll(applyDamageAbility(npc, ab, aliveEnemies.get(rand.nextInt(aliveEnemies.size()))));
            }
        }
        return events;
    }

    /** Ataque básico con targeting caótico: 65% enemigo, 25% aliado, 10% sí mismo. */
    private List<BattleEvent> applyNpcBasicAttack(Character npc,
                                                   List<Enemy> aliveEnemies, List<Character> aliveParty) {
        List<BattleEvent> events = new ArrayList<>();
        Random rand = new Random();
        int targetRoll = rand.nextInt(100);
        int raw = npc.getEffectiveAtk() + rand.nextInt(5) - 2;

        if (targetRoll < 65 && !aliveEnemies.isEmpty()) {
            Enemy t = aliveEnemies.get(rand.nextInt(aliveEnemies.size()));
            int dmg = t.receiveDamage(raw, false);
            events.add(dmg == -1 ? BattleEvent.dodged(t.getName())
                : BattleEvent.damage(npc.getName(), t.getName(), dmg, false));
            if (!t.isAlive()) events.add(BattleEvent.enemyDied(t.getName()));
        } else if (targetRoll < 90) {
            List<Character> others = aliveParty.stream().filter(c -> c != npc).toList();
            Character t = others.isEmpty() ? npc : others.get(rand.nextInt(others.size()));
            events.add(BattleEvent.log(npc.getName() + " ataca a " + t.getName() + " por error!"));
            int dmg = t.receiveDamage(raw, false);
            events.add(dmg == -1 ? BattleEvent.dodged(t.getName())
                : BattleEvent.damage(npc.getName(), t.getName(), dmg, false));
            if (!t.isAlive()) events.add(BattleEvent.characterDied(t.getName()));
        } else {
            events.add(BattleEvent.log(npc.getName() + " se golpea a sí mismo!"));
            int dmg = npc.receiveDamage(raw, false);
            events.add(dmg == -1 ? BattleEvent.dodged(npc.getName())
                : BattleEvent.damage(npc.getName(), npc.getName(), dmg, false));
            if (!npc.isAlive()) events.add(BattleEvent.characterDied(npc.getName()));
        }
        return events;
    }

    /** Daño de habilidad sobre un Character aliado (Santi disparando a su propio equipo). */
    private List<BattleEvent> applyDamageAbilityToAlly(Character actor, Ability ab, Character target) {
        List<BattleEvent> events = new ArrayList<>();
        int raw = (int)(actor.getEffectiveAtk() * ab.getDamageMultiplier());
        int dmg = target.receiveDamage(raw, ab.isIgnoreDefense());
        events.add(dmg == -1 ? BattleEvent.dodged(target.getName())
            : BattleEvent.damage(actor.getName(), target.getName(), dmg, ab.isIgnoreDefense()));
        if (!target.isAlive()) events.add(BattleEvent.characterDied(target.getName()));
        return events;
    }

    /** Aplica efecto de poción sobre un Character (reutilizable por NPC y jugador). */
    private List<BattleEvent> applyPotionEffect(String actorName, Potion pot, Character target) {
        List<BattleEvent> events = new ArrayList<>();
        switch (pot.getEffect()) {
            case HEAL_SMALL, HEAL_LARGE -> {
                int healed = target.healHp(pot.getPower());
                events.add(BattleEvent.heal(actorName, target.getName(), healed));
            }
            case PA_RESTORE -> {
                int restored = target.restorePA(pot.getPower());
                events.add(BattleEvent.log(target.getName() + " recupera " + restored + " PA."));
            }
            case ANTIDOTE -> {
                target.removeEffect(StatusEffect.Type.POISON);
                events.add(BattleEvent.log(target.getName() + ": veneno curado"));
            }
            case STRENGTH -> {
                target.applyEffect(new StatusEffect(StatusEffect.Type.ATTACK_UP, 2, pot.getPower()));
                events.add(BattleEvent.status(actorName, target.getName(), "Fuerza (+" + pot.getPower() + " ATK)"));
            }
            case REVIVE -> {
                if (!target.isAlive()) {
                    int reviveHp = (int)(target.getHpMax() * pot.getPower() / 100.0);
                    target.healHp(reviveHp);
                    events.add(BattleEvent.heal(actorName, target.getName(), reviveHp));
                }
            }
        }
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
        if (ab.hasEffect()) events.addAll(applyEffect(actor.getName(), target.getName(), ab, null, target));

        if (!target.isAlive()) events.add(BattleEvent.enemyDied(target.getName()));
        return events;
    }

    private List<BattleEvent> applyEffect(String actorName, String targetName, Ability ab,
                                           Character charTarget, Enemy enemyTarget) {
        List<BattleEvent> events = new ArrayList<>();
        StatusEffect effect = new StatusEffect(ab.getEffectType(), ab.getEffectDuration(), ab.getEffectValue());
        if (charTarget  != null) charTarget.applyEffect(effect);
        if (enemyTarget != null) enemyTarget.applyEffect(effect);
        events.add(BattleEvent.status(actorName, targetName, effect.getDisplayName()));
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
