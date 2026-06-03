package com.dungeontales.ui.components;

import com.dungeontales.util.SpriteSheet;

import javax.swing.Timer;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Frame-by-frame animator for characters with a sprite sheet.
 *
 * Sheet layout (256×256 per frame, 6 cols × 4 rows):
 *   Row 0: IDLE[0-2]  | ATTACK[0-2]
 *   Row 1: SKILL[0-2] | HIT[0-2]
 *   Row 2: DEATH[0-2] | DEATH[3] (ghost, col 5)
 */
public class CharacterAnimator {

    public enum State { IDLE, ATTACK, SKILL, HIT, DEATH }

    private static final int FRAME_W    = 256;
    private static final int FRAME_H    = 256;
    private static final int IDLE_MS    = 180;
    private static final int COMBAT_MS  = 130;

    private final Map<State, BufferedImage[]> frames = new EnumMap<>(State.class);
    private State    currentState  = State.IDLE;
    private int      frameIndex    = 0;
    private Timer    timer;
    private Runnable onFrameChange;
    private Runnable onAnimComplete;

    public CharacterAnimator(String characterName, int displayW, int displayH) {
        loadFrames(characterName, displayW, displayH);
        startIdleLoop();
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setOnFrameChange(Runnable r)   { this.onFrameChange  = r; }
    public void setOnAnimComplete(Runnable r)   { this.onAnimComplete = r; }

    public BufferedImage getCurrentFrame() {
        BufferedImage[] arr = frames.get(currentState);
        if (arr == null || arr.length == 0) return null;
        return arr[Math.min(frameIndex, arr.length - 1)];
    }

    /** Play an animation; IDLE loops, others play once then return to IDLE. */
    public void play(State state) {
        if (currentState == State.DEATH) return; // no recovery from death
        if (timer != null) timer.stop();
        currentState = state;
        frameIndex   = 0;
        notifyFrameChange();

        BufferedImage[] arr = frames.get(state);
        if (arr == null || arr.length == 0) {
            if (state == State.DEATH) {
                // Sin frames de muerte: completar inmediatamente y mantener estado DEATH
                if (onAnimComplete != null) onAnimComplete.run();
            } else if (state != State.IDLE) {
                startIdleLoop();
            }
            return;
        }

        int ms = (state == State.IDLE) ? IDLE_MS : COMBAT_MS;
        timer = new Timer(ms, null);
        timer.addActionListener(e -> advance(state, arr));
        timer.start();
    }

    public void dispose() {
        if (timer != null) timer.stop();
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private void advance(State state, BufferedImage[] arr) {
        frameIndex++;
        if (frameIndex < arr.length) {
            notifyFrameChange();
            return;
        }
        // Último frame alcanzado
        timer.stop();
        frameIndex = arr.length - 1;
        notifyFrameChange(); // muestra el último frame
        if (state == State.IDLE) {
            frameIndex = 0;
            notifyFrameChange();
            timer.start();
        } else if (state == State.DEATH) {
            if (onAnimComplete != null) onAnimComplete.run();
        } else {
            // Esperar un intervalo antes de volver a IDLE para que el último frame sea visible
            Timer endDelay = new Timer(COMBAT_MS, e -> {
                if (onAnimComplete != null) onAnimComplete.run();
                startIdleLoop();
            });
            endDelay.setRepeats(false);
            endDelay.start();
        }
    }

    private void startIdleLoop() {
        if (timer != null) timer.stop();
        currentState = State.IDLE;
        frameIndex   = 0;
        notifyFrameChange();

        BufferedImage[] arr = frames.get(State.IDLE);
        if (arr == null || arr.length <= 1) return; // single frame — no timer needed

        timer = new Timer(IDLE_MS, null);
        timer.addActionListener(e -> advance(State.IDLE, arr));
        timer.start();
    }

    private void notifyFrameChange() {
        if (onFrameChange != null) onFrameChange.run();
    }

    private void loadFrames(String name, int dw, int dh) {
        BufferedImage sheet = SpriteSheet.load("/sprites/characters/" + name + "_sheet.png");
        if (sheet == null) return;

        frames.put(State.IDLE,   scaled(SpriteSheet.sliceRow(sheet, FRAME_W, FRAME_H, 0, 0, 1), dw, dh));
        frames.put(State.ATTACK, scaled(SpriteSheet.sliceRow(sheet, FRAME_W, FRAME_H, 0, 3, 3), dw, dh));
        frames.put(State.SKILL,  scaled(SpriteSheet.sliceRow(sheet, FRAME_W, FRAME_H, 1, 0, 3), dw, dh));
        frames.put(State.HIT,    scaled(SpriteSheet.sliceRow(sheet, FRAME_W, FRAME_H, 1, 3, 3), dw, dh));

        // Death: 3 falling frames + ghost frame (row 2 col 5)
        BufferedImage[] falling = SpriteSheet.sliceRow(sheet, FRAME_W, FRAME_H, 2, 0, 3);
        BufferedImage[] ghost   = SpriteSheet.sliceRow(sheet, FRAME_W, FRAME_H, 2, 5, 1);
        BufferedImage[] death   = new BufferedImage[4];
        for (int i = 0; i < 3; i++) death[i] = SpriteSheet.scale(falling[i], dw, dh);
        death[3] = SpriteSheet.scale(ghost[0], dw, dh);
        frames.put(State.DEATH, death);
    }

    private static BufferedImage[] scaled(BufferedImage[] src, int w, int h) {
        BufferedImage[] out = new BufferedImage[src.length];
        for (int i = 0; i < src.length; i++) out[i] = SpriteSheet.scale(src[i], w, h);
        return out;
    }
}
