package com.dungeontales.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** Loads and slices uniform sprite sheets. */
public class SpriteSheet {

    public static BufferedImage load(String resourcePath) {
        try (InputStream is = SpriteSheet.class.getResourceAsStream(resourcePath)) {
            if (is != null) return ImageIO.read(is);
        } catch (Exception ignored) {}
        return null;
    }

    /** Returns {@code count} frames from the given row starting at startCol. */
    public static BufferedImage[] sliceRow(BufferedImage sheet, int frameW, int frameH,
                                            int row, int startCol, int count) {
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            frames[i] = sheet.getSubimage((startCol + i) * frameW, row * frameH, frameW, frameH);
        }
        return frames;
    }

    /** Scales a frame to the desired display size. */
    public static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
