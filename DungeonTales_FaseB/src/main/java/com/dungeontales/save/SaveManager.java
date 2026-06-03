package com.dungeontales.save;

import com.google.gson.*;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.dungeontales.core.GameState;
import com.dungeontales.core.model.character.Character;
import com.dungeontales.core.model.character.Paladin;
import com.dungeontales.core.model.character.Rogue;
import com.dungeontales.core.model.character.Warrior;
import com.dungeontales.core.model.items.Armor;
import com.dungeontales.core.model.items.Item;
import com.dungeontales.core.model.items.Potion;
import com.dungeontales.core.model.items.Weapon;

import java.io.*;
import java.nio.file.*;

/**
 * Guarda y carga el GameState como JSON en el directorio del usuario.
 * Usa Gson con RuntimeTypeAdapterFactory para manejar polimorfismo.
 */
public class SaveManager {

    private static final String SAVE_DIR  = System.getProperty("user.home") + File.separator + ".dungeontales";
    private static final String SAVE_FILE = SAVE_DIR + File.separator + "savegame.json";

    private static final Gson GSON;

    static {
        // Adaptadores para clases polimórficas
        RuntimeTypeAdapterFactory<Character> charFactory = RuntimeTypeAdapterFactory
            .of(Character.class, "type")
            .registerSubtype(Rogue.class,   "Rogue")
            .registerSubtype(Paladin.class, "Paladin")
            .registerSubtype(Warrior.class, "Warrior");

        RuntimeTypeAdapterFactory<Item> itemFactory = RuntimeTypeAdapterFactory
            .of(Item.class, "type")
            .registerSubtype(Weapon.class, "Weapon")
            .registerSubtype(Armor.class,  "Armor")
            .registerSubtype(Potion.class, "Potion");

        GSON = new GsonBuilder()
            .registerTypeAdapterFactory(charFactory)
            .registerTypeAdapterFactory(itemFactory)
            .setPrettyPrinting()
            .create();
    }

    /** Guarda la partida en JSON. Devuelve true si tuvo éxito. */
    public static boolean save(GameState state) {
        try {
            Files.createDirectories(Paths.get(SAVE_DIR));
            String json = GSON.toJson(state);
            Files.writeString(Paths.get(SAVE_FILE), json);
            return true;
        } catch (IOException e) {
            System.err.println("Error al guardar: " + e.getMessage());
            return false;
        }
    }

    /** Carga la partida desde JSON. Devuelve null si no existe o hay error. */
    public static GameState load() {
        try {
            if (!saveExists()) return null;
            String json = Files.readString(Paths.get(SAVE_FILE));
            return GSON.fromJson(json, GameState.class);
        } catch (Exception e) {
            System.err.println("Error al cargar guardado: " + e.getMessage());
            return null;
        }
    }

    public static boolean saveExists() {
        return Files.exists(Paths.get(SAVE_FILE));
    }

    public static void deleteSave() {
        try { Files.deleteIfExists(Paths.get(SAVE_FILE)); }
        catch (IOException ignored) {}
    }

    public static String getSavePath() { return SAVE_FILE; }
}
