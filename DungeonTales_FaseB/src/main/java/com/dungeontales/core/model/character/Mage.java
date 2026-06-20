package com.dungeontales.core.model.character;

import com.dungeontales.core.model.Ability;

/**
 * Santi — mago glass cannon controlado por IA.
 * Alto daño mágico (ATK 28), muy poca defensa (DEF 3), HP bajo (52).
 * Actúa de forma caótica: puede atacar aliados, enemigos o a sí mismo.
 */
public class Mage extends Character {

    public Mage(String name) {
        super(name, "Mago", 52, 14, 3, 28, 3, 14);
        this.npc = true; // controlado por la IA del engine, no por el jugador
    }

    @Override public String getSpriteName() { return "mage"; }

    @Override protected void initAbilities() {
        abilities.add(new Ability.Builder("Bola de Fuego",
            "3.0x ATK a todos los enemigos. A veces también a los aliados.", 3)
            .damage(3.0)
            .target(Ability.TargetType.ALL_ENEMIES)
            .build());

        abilities.add(new Ability.Builder("Rayo",
            "2.8x ATK a un objetivo. Ignora defensa. Santi no siempre apunta bien.", 2)
            .damage(2.8)
            .target(Ability.TargetType.SINGLE_ENEMY)
            .ignoreDefense()
            .build());

        abilities.add(new Ability.Builder("Tormenta Arcana",
            "2.5x ATK a todos los enemigos. La magia se descontrola con frecuencia.", 4)
            .damage(2.5)
            .target(Ability.TargetType.ALL_ENEMIES)
            .hitsAll()
            .build());

        abilities.add(new Ability.Builder("Lluvia de Meteoros",
            "4.5x ATK devastador. Pero Santi apenas puede controlarlo.", 5)
            .damage(4.5)
            .target(Ability.TargetType.ALL_ENEMIES)
            .build());
    }
}
