package com.dungeontales.ui.components;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Animator that loads individual PNG frames from:
 *   /sprites/characters/{Name}/{Action}/{Name}-{Action}-{N}.png
 *
 * Stand.png is shown as the IDLE static frame.
 * Combat animations (Atk, Spells, TakeDmg, Die) play frames 1→2→3
 * then return to Stand automatically.
 */
public class CharacterAnimator {

    public enum State { IDLE, ATTACK, SKILL, HIT, DEATH }

    private static final int FRAME_MS = 220;

    private final Map<State, BufferedImage[]> frames = new EnumMap<>(State.class);
    private State    currentState = State.IDLE;
    private int      frameIndex   = 0;
    private Timer    timer;
    private Runnable onFrameChange;
    private Runnable onAnimComplete;

    public CharacterAnimator(String characterName, int displayW, int displayH) {
        loadFrames(characterName, displayW, displayH);
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setOnFrameChange(Runnable r)  { this.onFrameChange  = r; }
    public void setOnAnimComplete(Runnable r)  { this.onAnimComplete = r; }

    public BufferedImage getCurrentFrame() {
        BufferedImage[] arr = frames.get(currentState);
        if (arr == null || arr.length == 0) return null;
        return arr[Math.min(frameIndex, arr.length - 1)];
    }

    /** IDLE shows Stand.png statically. Combat animations play once then return to IDLE. */
    public void play(State state) {
        if (currentState == State.DEATH && state != State.IDLE) return;
        if (timer != null) timer.stop();
        currentState = state;
        frameIndex   = 0;
        notifyFrameChange();

        BufferedImage[] arr = frames.get(state);
        if (arr == null || arr.length == 0) {
            if (state == State.DEATH) { if (onAnimComplete != null) onAnimComplete.run(); }
            else if (state != State.IDLE) returnToIdle();
            return;
        }
        if (state == State.IDLE) return; // static frame — no timer needed

        timer = new Timer(FRAME_MS, null);
        timer.addActionListener(e -> advance(state, arr));
        timer.start();
    }

    public void dispose() { if (timer != null) timer.stop(); }

    // ── Internal ──────────────────────────────────────────────────────────

    private void advance(State state, BufferedImage[] arr) {
        frameIndex++;
        if (frameIndex < arr.length) { notifyFrameChange(); return; }
        timer.stop();
        frameIndex = arr.length - 1;
        notifyFrameChange();
        if (state == State.DEATH) {
            if (onAnimComplete != null) onAnimComplete.run();
        } else {
            Timer delay = new Timer(FRAME_MS, e -> {
                if (onAnimComplete != null) onAnimComplete.run();
                returnToIdle();
            });
            delay.setRepeats(false);
            delay.start();
        }
    }

    private void returnToIdle() {
        currentState = State.IDLE;
        frameIndex   = 0;
        notifyFrameChange();
    }

    private void notifyFrameChange() { if (onFrameChange != null) onFrameChange.run(); }

    // ── Frame loading ─────────────────────────────────────────────────────

    private void loadFrames(String name, int dw, int dh) {
        String base = Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
        frames.put(State.IDLE,   loadStand(base, dw, dh));
        frames.put(State.ATTACK, loadAction(base, "Atk",     3, dw, dh));
        frames.put(State.SKILL,  loadAction(base, "Spells",  3, dw, dh));
        frames.put(State.HIT,    loadAction(base, "TakeDmg", 3, dw, dh));
        frames.put(State.DEATH,  loadAction(base, "Die",     3, dw, dh));
    }

    private BufferedImage[] loadStand(String base, int dw, int dh) {
        BufferedImage img = loadImg("/sprites/characters/" + base + "/" + base + "-Stand.png");
        return img != null ? new BufferedImage[]{ scale(img, dw, dh) } : new BufferedImage[0];
    }

    private BufferedImage[] loadAction(String base, String action, int count, int dw, int dh) {
        BufferedImage[] arr = new BufferedImage[count];
        int loaded = 0;
        for (int i = 1; i <= count; i++) {
            BufferedImage img = tryLoadFrame(base, action, i);
            if (img != null) { arr[i - 1] = scale(img, dw, dh); loaded++; }
        }
        return loaded > 0 ? arr : new BufferedImage[0];
    }

    /**
     * Tries several naming/folder variants to handle per-character inconsistencies:
     *   - Folder: "Atk" vs "ATk" (Warrior)
     *   - Filename: Name-Action-N.png vs Name-ActionN.png (Paladin TakeDmg)
     *   - Singular: Name-Spell-N.png vs Name-Spells-N.png (Rogue frame 1)
     */
    private BufferedImage tryLoadFrame(String base, String action, int n) {
        String[] folders = action.equals("Atk")
            ? new String[]{ "Atk", "ATk" }
            : new String[]{ action };

        for (String folder : folders) {
            String dir = "/sprites/characters/" + base + "/" + folder + "/";
            // Pattern 1: Name-Action-N.png  (standard)
            BufferedImage img = loadImg(dir + base + "-" + action + "-" + n + ".png");
            if (img != null) return img;
            // Pattern 2: Name-ActionN.png  (no dash before number)
            img = loadImg(dir + base + "-" + action + n + ".png");
            if (img != null) return img;
            // Pattern 3: singular action name (Spell vs Spells)
            if (action.equals("Spells")) {
                img = loadImg(dir + base + "-Spell-" + n + ".png");
                if (img != null) return img;
            }
        }
        return null;
    }

    private static BufferedImage loadImg(String path) {
        try {
            InputStream is = CharacterAnimator.class.getResourceAsStream(path);
            return is != null ? ImageIO.read(is) : null;
        } catch (Exception e) { return null; }
    }

    private static BufferedImage scale(BufferedImage src, int maxW, int maxH) {
        int iw = src.getWidth(), ih = src.getHeight();
        double s = Math.min((double) maxW / iw, (double) maxH / ih);
        int dw = (int)(iw * s), dh = (int)(ih * s);
        BufferedImage out = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(src, (maxW - dw) / 2, (maxH - dh) / 2, dw, dh, null);
        g2.dispose();
        return out;
    }
}
