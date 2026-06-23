package com.dungeontales.core.battle;

import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.Inventory;
import com.dungeontales.core.model.StatusEffect;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.core.model.enemy.Enemy;
import com.dungeontales.core.model.items.Potion;
import com.dungeontales.core.model.items.RareItem;

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
    private boolean               partyTookDamage; // true si algún enemigo dañó a la party

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

        // Veredicto del Alba: daño escala con DEF propia en vez de ATK
        int raw = actor.hasPassive(RareItem.PassiveType.DEFENSE_SCALES_DAMAGE)
                ? actor.getEffectiveDef() + new Random().nextInt(5) - 2
                : actor.getEffectiveAtk() + new Random().nextInt(5) - 2;

        // Devastadora de Huesos (Sed de Sangre): +2% de daño por cada 1% de HP faltante
        if (actor.hasPassive(RareItem.PassiveType.BERSERKER)) {
            double missingPct = 1.0 - (double) actor.getHp() / actor.getHpMax();
            raw = (int)(raw * (1.0 + 2.0 * missingPct));
        }

        // Beso de la Viuda: si el enemigo está envenenado, ignora 50% armadura
        boolean pierceDef = actor.hasPassive(RareItem.PassiveType.POISON_ARMOR_PIERCE)
                         && target.hasEffect(StatusEffect.Type.POISON);
        int dmg = target.receiveDamage(raw, pierceDef);
        events.add(dmg == -1
            ? BattleEvent.dodged(target.getName())
            : BattleEvent.damage(actor.getName(), target.getName(), dmg, pierceDef));

        // Veredicto del Alba: cura al aliado más débil el 10% del daño
        if (dmg > 0 && actor.hasPassive(RareItem.PassiveType.DEFENSE_SCALES_DAMAGE))
            healWeakestAlly(actor, dmg, events);

        if (!target.isAlive()) {
            events.add(BattleEvent.enemyDied(target.getName()));
            checkBattleOver(events);
        }
        turnIndex++;
        if (!battleOver) events.addAll(processUntilPlayerTurn());
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
                int dmg = applyDmgToChar(e, target, raw, false, events);
                if (dmg > 0) partyTookDamage = true;
            }
            case "double_attack" -> {
                events.add(BattleEvent.log(e.getName() + ": ¡Doble Ataque!"));
                for (int i = 0; i < 2; i++) {
                    Character t = alive.get(new Random().nextInt(alive.size()));
                    int raw = (int)(e.calculateRawDamage() * 0.7);
                    int dmg = applyDmgToChar(e, t, raw, false, events);
                    if (dmg > 0) partyTookDamage = true;
                }
            }
            case "heavy_strike", "smash" -> {
                events.add(BattleEvent.log(e.getName() + ": ¡Golpe Aplastante!"));
                int raw = (int)(e.calculateRawDamage() * 1.6);
                int dmg = applyDmgToChar(e, target, raw, false, events);
                if (dmg > 0) partyTookDamage = true;
            }
            case "bone_throw" -> {
                int raw = (int)(e.getAtk() * 0.9);
                int dmg = applyDmgToChar(e, target, raw, true, events);
                if (dmg > 0) partyTookDamage = true;
            }
            case "roar" -> {
                events.add(BattleEvent.log(e.getName() + ": ¡Rugido Aterrador! (−3 ATK party)"));
                party.stream().filter(Character::isAlive).forEach(p ->
                    p.applyEffect(new StatusEffect(StatusEffect.Type.ATTACK_UP, 1, -3)));
            }
            default -> {
                int raw = e.calculateRawDamage();
                int dmg = applyDmgToChar(e, target, raw, false, events);
                if (dmg > 0) partyTookDamage = true;
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
            // Báculo del Vacío Inestable: habilidades cuestan 1 PA menos (mínimo 0)
            int paDiscount = npc.hasPassive(RareItem.PassiveType.PA_DISCOUNT_RECOIL) ? 1 : 0;
            List<Ability> affordable = npc.getAbilities().stream()
                .filter(a -> npc.getPa() >= Math.max(0, a.getPaCost() - paDiscount)).toList();
            if (!affordable.isEmpty()) {
                Ability ab = affordable.get(new Random().nextInt(affordable.size()));
                npc.consumePA(Math.max(0, ab.getPaCost() - paDiscount));
                events.add(BattleEvent.log(npc.getName() + " lanza " + ab.getName() + "!"));
                events.addAll(applyNpcAbility(npc, ab, aliveEnemies, aliveParty));
                // Báculo: recoil de 6 HP por hechizo lanzado
                if (paDiscount > 0 && npc.isAlive()) {
                    int recoil = npc.receiveDamage(6, true);
                    if (recoil > 0)
                        events.add(BattleEvent.log("¡Recoil Arcano! " + npc.getName()
                            + " recibe " + recoil + " de retroceso."));
                    if (!npc.isAlive())
                        events.add(BattleEvent.characterDied(npc.getName()));
                }
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
            applyDmgToChar(npc, t, raw, false, events);
            if (!t.isAlive()) events.add(BattleEvent.characterDied(t.getName()));
        } else {
            events.add(BattleEvent.log(npc.getName() + " se golpea a sí mismo!"));
            applyDmgToChar(npc, npc, raw, false, events);
            if (!npc.isAlive()) events.add(BattleEvent.characterDied(npc.getName()));
        }
        return events;
    }

    /** Daño de habilidad sobre un Character aliado (Santi disparando a su propio equipo). */
    private List<BattleEvent> applyDamageAbilityToAlly(Character actor, Ability ab, Character target) {
        List<BattleEvent> events = new ArrayList<>();
        int raw = (int)(actor.getEffectiveAtk() * ab.getDamageMultiplier());
        applyDmgToChar(actor, target, raw, ab.isIgnoreDefense(), events);
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

        // Veredicto del Alba: base = DEF en vez de ATK
        int baseAtk = actor.hasPassive(RareItem.PassiveType.DEFENSE_SCALES_DAMAGE)
                    ? actor.getEffectiveDef()
                    : actor.getEffectiveAtk();
        int raw = (int)(baseAtk * ab.getDamageMultiplier());

        // Devastadora de Huesos
        if (actor.hasPassive(RareItem.PassiveType.BERSERKER)) {
            double missingPct = 1.0 - (double) actor.getHp() / actor.getHpMax();
            raw = (int)(raw * (1.0 + 2.0 * missingPct));
        }

        // Bonus de Golpe Furtivo si el objetivo está debilitado
        if (ab.getName().equals("Golpe Furtivo") &&
            (target.hasEffect(StatusEffect.Type.POISON) || target.hasEffect(StatusEffect.Type.STUN))) {
            raw = (int)(raw * 1.5);
            events.add(BattleEvent.log("¡Golpe Furtivo potenciado!"));
        }

        // Beso de la Viuda: si el enemigo está envenenado, ignora 50% armadura
        boolean pierceDef = actor.hasPassive(RareItem.PassiveType.POISON_ARMOR_PIERCE)
                         && target.hasEffect(StatusEffect.Type.POISON);
        boolean ignores = ab.isIgnoreDefense() || pierceDef;

        int dmg = target.receiveDamage(raw, ignores);
        events.add(dmg == -1
            ? BattleEvent.dodged(target.getName())
            : BattleEvent.damage(actor.getName(), target.getName(), dmg, ignores));

        // Veredicto del Alba: cura al aliado más débil
        if (dmg > 0 && actor.hasPassive(RareItem.PassiveType.DEFENSE_SCALES_DAMAGE))
            healWeakestAlly(actor, dmg, events);

        // Segundo golpe (Doble Ataque)
        if (ab.isDoubleHit()) {
            int raw2 = (int)(baseAtk * ab.getDamageMultiplier());
            int dmg2 = target.receiveDamage(raw2, false);
            if (dmg2 != -1) events.add(BattleEvent.damage(actor.getName(), target.getName(), dmg2, false));
        }

        // Efecto de estado sobre el objetivo
        if (ab.hasEffect()) events.addAll(applyEffect(actor.getName(), target.getName(), ab, null, target));

        if (!target.isAlive()) events.add(BattleEvent.enemyDied(target.getName()));
        return events;
    }

    // ── Helpers de pasivos ────────────────────────────────────────────────

    /** Veredicto del Alba: cura al aliado vivo con menos HP% un 10% del daño. */
    private void healWeakestAlly(Character actor, int dmg, List<BattleEvent> events) {
        party.stream()
            .filter(c -> c.isAlive() && c != actor)
            .min(java.util.Comparator.comparingDouble(c -> (double) c.getHp() / c.getHpMax()))
            .ifPresent(weakest -> {
                int healed = weakest.healHp(Math.max(1, dmg / 10));
                if (healed > 0)
                    events.add(BattleEvent.heal(actor.getName(), weakest.getName(), healed));
            });
    }

    /**
     * Aplica daño de un atacante (Enemy o Character NPC) a un personaje jugable,
     * manejando todos los pasivos defensivos y ofensivos:
     *  -1 → esquivado, -2 → bloqueado (Escudo Arcano / invencibilidad), -3 → Death Save
     * También aplica: Égida del Mártir (absorbe 20%) y Espinas (refleja 30%).
     */
    private int applyDmgToChar(Object attacker, Character target, int rawDmg,
                                boolean ignoreDefense, List<BattleEvent> events) {
        String attackerName = attacker instanceof Character c ? c.getName() : ((Enemy) attacker).getName();

        // Égida del Mártir: si Aldric está vivo con este pasivo, absorbe 20% del raw
        int modRaw = rawDmg;
        Character shieldBearer = party.stream()
            .filter(c -> c.isAlive() && c != target && c.hasPassive(RareItem.PassiveType.DAMAGE_SHARE))
            .findFirst().orElse(null);
        int sharedAmount = 0;
        if (shieldBearer != null) {
            sharedAmount = (int)(rawDmg * 0.20);
            modRaw = rawDmg - sharedAmount;
        }

        int dmg = target.receiveDamage(modRaw, ignoreDefense);

        if (dmg == -1) {
            events.add(BattleEvent.dodged(target.getName()));
        } else if (dmg == -2) {
            if (target.isArcaneShieldActive() || target.isDeathSaveInvincible()) {
                // isArcaneShieldActive ya se consumió dentro de receiveDamage, pero chequeamos el tipo
            }
            events.add(BattleEvent.log("¡Golpe bloqueado! " + target.getName() + " es intocable."));
            dmg = 0;
        } else if (dmg == -3) {
            events.add(BattleEvent.log(
                "¡Manto del Espectro! " + target.getName() + " sobrevive con 1 HP — intocable 1 turno."));
            dmg = 0;
        } else {
            events.add(BattleEvent.damage(attackerName, target.getName(), dmg, ignoreDefense));

            // Égida del Mártir: aplicar porción absorbida a Aldric
            if (shieldBearer != null && sharedAmount > 0) {
                int aldricDmg = shieldBearer.receiveDamage(sharedAmount, true);
                if (aldricDmg > 0) {
                    events.add(BattleEvent.log(
                        shieldBearer.getName() + " absorbe " + aldricDmg + " dmg (Égida del Mártir)."));
                    if (!shieldBearer.isAlive())
                        events.add(BattleEvent.characterDied(shieldBearer.getName()));
                }
            }

            // Espinas: refleja 30% del daño al atacante si es un Enemy
            if (dmg > 0 && target.hasPassive(RareItem.PassiveType.THORNS)
                    && attacker instanceof Enemy e) {
                int thornDmg = Math.max(1, (int)(dmg * 0.30));
                int reflected = e.receiveDamage(thornDmg, true);
                events.add(BattleEvent.log(
                    "¡Espinas! " + reflected + " dmg reflejado a " + e.getName() + "."));
                if (!e.isAlive()) events.add(BattleEvent.enemyDied(e.getName()));
            }
        }
        return dmg;
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
    public boolean isBattleOver()      { return battleOver; }
    public boolean isPlayerWon()       { return playerWon; }
    public boolean isPartyTookDamage() { return partyTookDamage; }
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
