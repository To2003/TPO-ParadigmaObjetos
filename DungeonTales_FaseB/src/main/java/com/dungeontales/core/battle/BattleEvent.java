package com.dungeontales.core.battle;

/** Evento emitido por BattleEngine. La UI los anima uno por uno. */
public class BattleEvent {
    public enum Type {
        TURN_START,         // empieza el turno de alguien
        PA_RESTORED,        // PA restaurados al inicio de turno
        DAMAGE_DEALT,       // daño aplicado
        DAMAGE_DODGED,      // ataque esquivado
        HEAL_APPLIED,       // curación aplicada
        STATUS_APPLIED,     // efecto de estado aplicado
        STATUS_EXPIRED,     // efecto de estado expiró
        POISON_TICK,        // daño de veneno
        REGEN_TICK,         // regeneración de HP
        CHARACTER_DIED,     // personaje cayó
        ENEMY_DIED,         // enemigo cayó
        ENEMY_STUNNED,      // enemigo pierde turno
        CHARACTER_STUNNED,  // personaje pierde turno
        BATTLE_WON,         // victoria
        BATTLE_LOST,        // derrota
        LEVEL_UP,           // personaje subió de nivel
        LOG_MESSAGE,        // mensaje al log de combate
    }

    public final Type type;
    public final String actorName;   // quién realiza la acción
    public final String targetName;  // quién la recibe
    public final int value;          // daño, curación, etc.
    public final String message;     // texto para el log

    private BattleEvent(Type type, String actor, String target, int value, String msg) {
        this.type = type; this.actorName = actor;
        this.targetName = target; this.value = value; this.message = msg;
    }

    // Fábrica estática para cada tipo
    public static BattleEvent turnStart(String actor) {
        return new BattleEvent(Type.TURN_START, actor, null, 0, actor + " actúa");
    }
    public static BattleEvent damage(String actor, String target, int dmg, boolean sacred) {
        String msg = actor + " → " + target + ": " + dmg + " DMG" + (sacred ? " (sagrado)" : "");
        return new BattleEvent(Type.DAMAGE_DEALT, actor, target, dmg, msg);
    }
    public static BattleEvent dodged(String target) {
        return new BattleEvent(Type.DAMAGE_DODGED, null, target, 0, target + " esquiva el ataque!");
    }
    public static BattleEvent heal(String actor, String target, int amount) {
        return new BattleEvent(Type.HEAL_APPLIED, actor, target, amount,
            target + " recupera " + amount + " HP");
    }
    public static BattleEvent status(String actor, String target, String effectName) {
        return new BattleEvent(Type.STATUS_APPLIED, actor, target, 0,
            target + " recibe: " + effectName);
    }
    public static BattleEvent poisonTick(String target, int dmg) {
        return new BattleEvent(Type.POISON_TICK, null, target, dmg,
            target + " sufre " + dmg + " de veneno");
    }
    public static BattleEvent regenTick(String target, int amount) {
        return new BattleEvent(Type.REGEN_TICK, null, target, amount,
            target + " se regenera " + amount + " HP");
    }
    public static BattleEvent characterDied(String name) {
        return new BattleEvent(Type.CHARACTER_DIED, null, name, 0, name + " ha caído!");
    }
    public static BattleEvent enemyDied(String name) {
        return new BattleEvent(Type.ENEMY_DIED, null, name, 0, name + " es derrotado!");
    }
    public static BattleEvent stunned(String name, boolean isCharacter) {
        return new BattleEvent(isCharacter ? Type.CHARACTER_STUNNED : Type.ENEMY_STUNNED,
            null, name, 0, name + " está aturdido y pierde su turno!");
    }
    public static BattleEvent won() {
        return new BattleEvent(Type.BATTLE_WON, null, null, 0, "¡¡ VICTORIA !!");
    }
    public static BattleEvent lost() {
        return new BattleEvent(Type.BATTLE_LOST, null, null, 0, "La party ha sido derrotada...");
    }
    public static BattleEvent levelUp(String name, int newLevel) {
        return new BattleEvent(Type.LEVEL_UP, name, null, newLevel,
            "★ " + name + " sube al nivel " + newLevel + "!");
    }
    public static BattleEvent log(String msg) {
        return new BattleEvent(Type.LOG_MESSAGE, null, null, 0, msg);
    }
}
