package com.dungeontales.core.model.character;

import com.dungeontales.core.model.Ability;
import com.dungeontales.core.model.StatusEffect;

public class Paladin extends Character {
    public Paladin(String name) { super(name, "Paladín", 110, 10, 2, 14, 16, 10); }

    @Override public String getSpriteName() { return "paladin"; }

    @Override protected void initAbilities() {
        abilities.add(new Ability.Builder("Golpe Divino",
            "2.2x ATK sagrado. Ignora la defensa del enemigo.", 3)
            .damage(2.2).ignoreDefense().build());

        abilities.add(new Ability.Builder("Imposición de Manos",
            "Sana a un aliado por 35 HP.", 3)
            .target(Ability.TargetType.SINGLE_ALLY).heal(35).build());

        abilities.add(new Ability.Builder("Escudo de Fe",
            "+10 DEF propia por 2 turnos.", 2)
            .target(Ability.TargetType.SELF)
            .effect(StatusEffect.Type.DEFENSE_UP, 2, 10).build());

        abilities.add(new Ability.Builder("Castigo Sagrado",
            "0.9x ATK sagrado a TODOS los enemigos.", 4)
            .target(Ability.TargetType.ALL_ENEMIES)
            .damage(0.9).hitsAll().ignoreDefense().build());
    }
}
