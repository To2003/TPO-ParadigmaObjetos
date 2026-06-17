package com.dungeontales.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga sprites desde resources/sprites/.
 * Si el archivo no existe, genera un placeholder con color y letra.
 * Así el juego funciona aunque no tengan todos los assets todavía.
 */
public class SpriteLoader {

    private static final Map<String, BufferedImage> cache = new HashMap<>();

    // Colores de placeholder por tipo
    private static final Map<String, Color> PLACEHOLDER_COLORS = Map.of(
            "rogue", new Color(0x40, 0xC0, 0xC0),
            "paladin", new Color(0xC0, 0xC0, 0x40),
            "warrior", new Color(0xC0, 0x60, 0x40),
            "goblin", new Color(0x60, 0xC0, 0x60),
            "skeleton", new Color(0xD0, 0xD0, 0xD0),
            "troll", new Color(0xA0, 0x60, 0xC0),
            "boss", new Color(0xE0, 0x40, 0x40));

    /**
     * Carga la imagen del personaje.
     * Busca en: /sprites/characters/{nombre}.png
     * Fallback: placeholder coloreado con inicial.
     */
    public static BufferedImage getCharacterSprite(String name, int width, int height) {
        String key = "character_" + name + "_" + width + "x" + height;
        return cache.computeIfAbsent(key, k -> {
            String baseName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            return loadOrPlaceholder(
                "/sprites/characters/" + baseName + "/sprite-" + name.toLowerCase() + ".png",
                name, width, height, true
            );
        });
    }

    /**
     * Carga la imagen del enemigo.
     * Busca en: /sprites/enemies/{nombre}.png
     */
    public static BufferedImage getEnemySprite(String name, int width, int height) {
        String key = "enemy_" + name + "_" + width + "x" + height;
        return cache.computeIfAbsent(key, k -> loadOrPlaceholder(
                "/sprites/enemies/" + name.toLowerCase().replace(" ", "_") + ".png",
                name, width, height, false));
    }

    /**
     * Carga una imagen de UI (iconos, fondos).
     * Busca en: /sprites/ui/{nombre}.png
     */
    public static BufferedImage getUISprite(String name, int width, int height) {
        String key = "ui_" + name + "_" + width + "x" + height;
        return cache.computeIfAbsent(key, k -> loadOrPlaceholder(
                "/sprites/ui/" + name.toLowerCase() + ".png",
                name, width, height, true));
    }

    /**
     * Carga un ícono de la tienda eliminando el fondo negro.
     * Busca en: /sprites/icon-shop/{nombre}.png
     */
    public static BufferedImage getShopIcon(String name, int size) {
        String key = "shopicon_" + name + "_" + size;
        return cache.computeIfAbsent(key, k -> {
            BufferedImage raw = loadOrPlaceholder(
                    "/sprites/icon-shop/" + name + ".png", name, size, size, false);
            return removeBlackBackground(raw);
        });
    }

    /**
     * Carga un ícono de la pantalla de descanso eliminando el fondo negro.
     * Busca en: /sprites/icon-rest/{nombre}.png
     */
    public static BufferedImage getRestIcon(String name, int size) {
        String key = "resticon_" + name + "_" + size;
        return cache.computeIfAbsent(key, k -> {
            BufferedImage raw = loadOrPlaceholder(
                    "/sprites/icon-rest/" + name + ".png", name, size, size, false);
            return removeBlackBackground(raw);
        });
    }

    /**
     * Carga un ícono de la sala del tesoro.
     * La imagen ya tiene el fondo transparente.
     * Busca en: /sprites/icon-treasure/{nombre}.png
     */
    public static BufferedImage getTreasureIcon(String name, int size) {
        String key = "treasureicon_" + name + "_" + size;
        return cache.computeIfAbsent(key, k -> {
            return loadOrPlaceholder(
                    "/sprites/icon-treasure/" + name + ".png", name, size, size, false);
        });
    }

    /**
     * Carga un fondo de pantalla sin escalar ni modificar.
     * Busca en: /sprites/backgrounds/{nombre}-bg.png
     */
    public static BufferedImage getBackground(String name) {
        String key = "bg_" + name;
        return cache.computeIfAbsent(key, k -> {
            try {
                InputStream is = SpriteLoader.class.getResourceAsStream("/sprites/backgrounds/" + name + "-bg.png");
                if (is == null) {
                    is = SpriteLoader.class.getResourceAsStream("/sprites/backgrounds/" + name + "-bg.jpg");
                }
                if (is != null)
                    return ImageIO.read(is);
            } catch (Exception ignored) {
            }
            return makePlaceholder(name, 1344, 768);
        });
    }

    /**
     * Carga un ícono del mapa y elimina el fondo negro convirtiéndolo en
     * transparencia.
     * Busca en: /sprites/icon-map/{nombre}.png
     */
    public static BufferedImage getMapIcon(String name, int size) {
        String key = "mapicon_" + name + "_" + size;
        return cache.computeIfAbsent(key, k -> {
            BufferedImage raw = loadOrPlaceholder(
                    "/sprites/icon-map/" + name + ".png", name, size, size, false);
            return removeBlackBackground(raw);
        });
    }

    /**
     * Convierte el fondo negro en transparencia usando el brillo del píxel como
     * alpha.
     * Negro (brillo=0) → completamente transparente.
     * Blanco (brillo=255) → completamente opaco.
     */
    private static BufferedImage removeBlackBackground(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int alpha = Math.max(r, Math.max(g, b)); // brillo = alpha
                out.setRGB(x, y, (alpha << 24) | (rgb & 0x00FFFFFF));
            }
        }
        return out;
    }

    private static BufferedImage loadOrPlaceholder(String path, String name,
            int w, int h, boolean flip) {
        try {
            InputStream is = SpriteLoader.class.getResourceAsStream(path);
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                return scale(img, w, h);
            }
        } catch (Exception ignored) {
        }
        return makePlaceholder(name, w, h);
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /**
     * Genera un placeholder rectangular con el color del personaje/enemigo,
     * borde redondeado y la inicial del nombre.
     */
    private static BufferedImage makePlaceholder(String name, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String key = name.toLowerCase().split(" ")[0];
        Color base = PLACEHOLDER_COLORS.getOrDefault(key, new Color(0x80, 0x80, 0x80));
        Color dark = base.darker().darker();

        // Fondo con gradiente simulado
        g.setColor(dark);
        g.fillRoundRect(0, 0, w, h, 12, 12);
        g.setColor(base.darker());
        g.fillRoundRect(2, 2, w - 4, h / 2, 10, 10);

        // Borde
        g.setColor(base);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(1, 1, w - 2, h - 2, 10, 10);

        // Inicial centrada
        g.setColor(new Color(255, 255, 255, 200));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.min(w, h) / 2));
        FontMetrics fm = g.getFontMetrics();
        String letter = name.substring(0, 1).toUpperCase();
        int tx = (w - fm.stringWidth(letter)) / 2;
        int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(letter, tx, ty);

        // Nombre en la parte inferior (si el sprite es grande)
        if (h > 80) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            fm = g.getFontMetrics();
            String shortName = name.length() > 10 ? name.substring(0, 10) : name;
            g.setColor(new Color(255, 255, 255, 160));
            g.drawString(shortName, (w - fm.stringWidth(shortName)) / 2, h - 6);
        }

        g.dispose();
        return img;
    }
}
