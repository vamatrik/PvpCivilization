package com.pvpciv.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Прокрутка списка: смещение в пикселях, колесо мыши, полоска справа, обрезка по окну (scissor).
 * Использование: s.view(h); s.content(h); begin(g,…); рисовать с y − s.off; end(g); s.bar(g,…).
 */
public final class Scroll {
    public int off, content, view;

    public Scroll view(int h) { view = h; clamp(); return this; }
    public Scroll content(int h) { content = h; clamp(); return this; }
    public boolean needed() { return content > view; }

    public void clamp() { off = Math.max(0, Math.min(off, Math.max(0, content - view))); }

    /** Колесо: true — прокрутили. */
    public boolean wheel(double dy) {
        if (!needed()) return false;
        off -= (int) Math.round(dy * 14);
        clamp();
        return true;
    }

    public static void begin(GuiGraphics g, int x, int y, int w, int h) { g.enableScissor(x, y, x + w, y + h); }
    public static void end(GuiGraphics g) { g.disableScissor(); }

    /** Полоска прокрутки высотой h у правого края (x — её левый край, ширина 3). */
    public void bar(GuiGraphics g, int x, int y, int h) {
        if (!needed()) return;
        g.fill(x, y, x + 3, y + h, 0x30FFFFFF);
        int th = Math.max(12, h * view / content), ty = y + (h - th) * off / Math.max(1, content - view);
        g.fill(x, ty, x + 3, ty + th, 0xC0D9A441);
    }
}
