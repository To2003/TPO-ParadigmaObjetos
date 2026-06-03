package com.dungeontales.core.model.character;

import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.StatusEffect;

public class Warrior extends Character {
    public Warrior(String name) { super(name, "Guerrero", 130, 10, 3, 16, 18, 8); }

    @Override public String getSpriteName() { return "warrior"; }

    @Override protected void initAbilities() {
        abilities.add(new Ability.Builder("Golpe Brutal",
            "3.0x ATK. El golpe más potente de la party.", 4)
            .damage(3.0).build());

        abilities.add(new Ability.Builder("Carga",
            "1.2x ATK + Aturde al objetivo 1 turno.", 2)
            .damage(1.2)
            .effect(StatusEffect.Type.STUN, 1, 0).build());

        abilities.add(new Ability.Builder("Grito de Batalla",
            "+5 ATK a toda la party por 2 turnos.", 3)
            .target(Ability.TargetType.ALL_ALLIES)
            .effect(StatusEffect.Type.ATTACK_UP, 2, 5).build());

        abilities.add(new Ability.Builder("Torbellino",
            "1.0x ATK a TODOS los enemigos simultáneamente.", 5)
            .target(Ability.TargetType.ALL_ENEMIES)
            .damage(1.0).hitsAll().build());
    }
}
