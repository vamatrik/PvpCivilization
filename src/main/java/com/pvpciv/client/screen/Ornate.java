package com.pvpciv.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/** «Кружевные» RPG-рамки: золотой кант, точечный орнамент, ромбы-камни. */
public final class Ornate {
    public static final int GOLD = 0xFFD9A441, GOLD_D = 0xFF7A5420, GOLD_L = 0xFFFFE08A, DARK = 0xFF0B0A10;

    /** Плита с резной рамкой. */
    public static void plate(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, DARK);
        g.fillGradient(x, y, x + w, y + h, 0xF0231C2A, 0xF0120E18);
        frame(g, x, y, w, h);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + 1, GOLD); g.fill(x, y + h - 1, x + w, y + h, GOLD_D);
        g.fill(x, y, x + 1, y + h, GOLD); g.fill(x + w - 1, y, x + w, y + h, GOLD_D);
        // кружевная внутренняя строчка
        for (int i = x + 3; i < x + w - 3; i += 3) { g.fill(i, y + 2, i + 1, y + 3, 0xA0D9A441); g.fill(i, y + h - 3, i + 1, y + h - 2, 0x707A5420); }
        // уголки-завитки
        curl(g, x, y, 1, 1); curl(g, x + w - 1, y, -1, 1); curl(g, x, y + h - 1, 1, -1); curl(g, x + w - 1, y + h - 1, -1, -1);
    }

    private static void curl(GuiGraphics g, int x, int y, int dx, int dy) {
        px(g, x + dx, y + dy, GOLD_L); px(g, x + 2 * dx, y + dy, GOLD); px(g, x + dx, y + 2 * dy, GOLD);
        px(g, x + 3 * dx, y + 2 * dy, GOLD_D); px(g, x + 2 * dx, y + 3 * dy, GOLD_D);
    }

    public static void px(GuiGraphics g, int x, int y, int c) { g.fill(x, y, x + 1, y + 1, c); }

    /** Ромб-камень. */
    public static void gem(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int w = r - Math.abs(dy);
            g.fill(cx - w - 1, cy + dy, cx + w + 2, cy + dy + 1, GOLD_D);
        }
        for (int dy = -r + 1; dy <= r - 1; dy++) {
            int w = r - 1 - Math.abs(dy);
            g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, 0xFF000000 | color);
        }
        px(g, cx - 1, cy - r + 2, 0xC0FFFFFF);
    }

    /** Полоска с орнаментом: заливка, блик, деления, камни на концах. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float f, int color, int gemColor) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, DARK);
        g.fill(x, y, x + w, y + h, 0xFF1A1420);
        int fw = (int) (w * Math.max(0, Math.min(1, f)));
        if (fw > 0) {
            g.fillGradient(x, y, x + fw, y + h, 0xFF000000 | CivUi.brighten(color), 0xFF000000 | CivUi.darken(color));
            g.fill(x, y + 1, x + fw, y + 2, 0x60FFFFFF);
            g.fill(x + fw - 1, y, x + fw, y + h, 0x80FFFFFF);
        }
        for (int i = 1; i < 10; i++) { int tx = x + w * i / 10; g.fill(tx, y, tx + 1, y + 2, 0x80000000); g.fill(tx, y + h - 2, tx + 1, y + h, 0x80000000); }
        g.fill(x, y - 1, x + w, y, GOLD); g.fill(x, y + h, x + w, y + h + 1, GOLD_D);
        for (int i = x + 2; i < x + w - 2; i += 4) px(g, i, y - 2, 0xB0D9A441);
        gem(g, x - 3, y + h / 2, 4, gemColor);
        gem(g, x + w + 2, y + h / 2, 4, gemColor);
    }

    /** Слот хотбара/навыка. */
    public static void slot(GuiGraphics g, int x, int y, int s, boolean selected) {
        g.fill(x, y, x + s, y + s, selected ? 0xFF3A2A12 : 0xFF0E0B14);
        g.fill(x, y, x + s, y + 1, selected ? GOLD_L : 0xFF3A3048);
        g.fill(x, y + s - 1, x + s, y + s, selected ? GOLD : 0xFF05040A);
        g.fill(x, y, x + 1, y + s, selected ? GOLD_L : 0xFF2A2236);
        g.fill(x + s - 1, y, x + s, y + s, selected ? GOLD : 0xFF05040A);
        if (selected) {
            g.fill(x - 1, y - 1, x + s + 1, y, GOLD_L);
            g.fill(x - 1, y + s, x + s + 1, y + s + 1, GOLD);
            g.fillGradient(x + 1, y + 1, x + s - 1, y + s - 1, 0x40FFD070, 0x10FFD070);
        }
    }
}
