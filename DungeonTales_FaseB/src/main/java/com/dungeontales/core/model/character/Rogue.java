package com.dungeontales.core.model.character;

import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.StatusEffect;

public class Rogue extends Character {
    public Rogue(String name) { super(name, "Pícaro", 65, 12, 4, 20, 6, 20); }

    @Override public String getSpriteName() { return "rogue"; }

    @Override protected void initAbilities() {
        abilities.add(new Ability.Builder("Golpe Furtivo",
            "2.5x ATK. x1.5 si el objetivo está envenenado o aturdido.", 3)
            .damage(2.5).build());

        abilities.add(new Ability.Builder("Envenenar",
            "0.6x ATK + Veneno: 5 daño/turno por 3 turnos.", 2)
            .damage(0.6)
            .effect(StatusEffect.Type.POISON, 3, 5)
            .build());

        abilities.add(new Ability.Builder("Evasión",
            "50% de esquivar el próximo ataque recibido.", 2)
            .target(Ability.TargetType.SELF)
            .effect(StatusEffect.Type.EVASION, 2, 0)
            .build());

        abilities.add(new Ability.Builder("Doble Ataque",
            "Dos golpes de 0.8x ATK cada uno.", 4)
            .damage(0.8).doubleHit().build());
    }
}
