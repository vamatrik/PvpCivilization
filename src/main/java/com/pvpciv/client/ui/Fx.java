package com.pvpciv.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Лёгкие экранные эффекты: искры, всплывающий текст, вспышка, тряска. Один экземпляр на экран. */
public final class Fx {
    private record Spark(float x, float y, float vx, float vy, long born, int life, int color) {}
    private record Pop(String text, float x, float y, long born, int color) {}
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Pop> pops = new ArrayList<>();
    private final Random rnd = new Random();
    private long flashAt = -1, shakeAt = -1; private int flashColor, flashLen = 400, shakeLen = 350;

    public void burst(int x, int y, int n, int color, float speed) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2; float v = speed * (0.4f + rnd.nextFloat());
            sparks.add(new Spark(x, y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - speed * 0.5f, t, 500 + rnd.nextInt(500), color));
        }
    }
    public void pop(String text, int x, int y, int color) { pops.add(new Pop(text, x, y, System.currentTimeMillis(), color)); }
    public void flash(int rgb, int ms) { flashAt = System.currentTimeMillis(); flashColor = rgb & 0xFFFFFF; flashLen = ms; }
    public void shake(int ms) { shakeAt = System.currentTimeMillis(); shakeLen = ms; }

    /** Смещение для тряски (применяй к pose до рисования окна). */
    public int shakeX() {
        long d = System.currentTimeMillis() - shakeAt;
        if (shakeAt < 0 || d > shakeLen) return 0;
        return Math.round((float) Math.sin(d * 0.09) * 4f * (1 - d / (float) shakeLen));
    }

    public void render(GuiGraphics g, Font f, int screenW, int screenH) {
        long t = System.currentTimeMillis();
        for (Iterator<Spark> it = sparks.iterator(); it.hasNext(); ) {
            Spark s = it.next(); float k = (t - s.born) / (float) s.life;
            if (k >= 1) { it.remove(); continue; }
            float sec = (t - s.born) / 1000f;
            int x = Math.round(s.x + s.vx * sec), y = Math.round(s.y + s.vy * sec + 60 * sec * sec);
            int c = Skin.alpha(0xFF000000 | s.color, 1 - k);
            g.fill(x, y, x + 2, y + 2, c); g.fill(x, y, x + 1, y + 1, Skin.alpha(0xFFFFFFFF, 1 - k));
        }
        for (Iterator<Pop> it = pops.iterator(); it.hasNext(); ) {
            Pop p = it.next(); float k = (t - p.born) / 1100f;
            if (k >= 1) { it.remove(); continue; }
            int a = Math.max(8, Math.round(255 * Math.min(1, 2 - 2 * k)));
            g.drawCenteredString(f, p.text, Math.round(p.x), Math.round(p.y - 22 * k), (a << 24) | (p.color & 0xFFFFFF));
        }
        long d = t - flashAt;
        if (flashAt >= 0 && d < flashLen) g.fill(0, 0, screenW, screenH, (Math.round(110 * (1 - d / (float) flashLen)) << 24) | flashColor);
    }
}
