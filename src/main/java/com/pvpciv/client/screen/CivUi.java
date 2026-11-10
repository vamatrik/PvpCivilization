package com.pvpciv.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Отрисовка элементов интерфейса цивилизации: стеклянные панели, кнопки, вкладки, слоты. */
public final class CivUi {
    public static final int GOLD = 0xFFE8B04A, CYAN = 0xFF7FDBFF, TEXT = 0xFFE8ECF4, DIM = 0xFF8C96A8;

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, 0xF0161B2C, 0xF00C0F1A);
        g.fill(x, y, x + w, y + 1, 0xFF3A4566);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF05070C);
        g.fill(x, y, x + 1, y + h, 0xFF2A3350);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF2A3350);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x30FFFFFF);
        // уголки
        corner(g, x, y); corner(g, x + w - 6, y); corner(g, x, y + h - 6); corner(g, x + w - 6, y + h - 6);
    }

    private static void corner(GuiGraphics g, int x, int y) {
        g.fill(x + 2, y + 2, x + 4, y + 4, GOLD);
    }

    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0x80000000);
        g.fill(x, y, x + w, y + 1, 0x60000000);
        g.fill(x, y + h - 1, x + w, y + h, 0x25FFFFFF);
    }

    public static void header(GuiGraphics g, Font f, int x, int y, int w, String title) {
        g.fill(x + 1, y + 1, x + w - 1, y + 16, 0x60222B45);
        g.fill(x + 1, y + 16, x + w - 1, y + 17, 0x80E8B04A);
        g.drawString(f, title, x + 8, y + 5, GOLD, true);
    }

    public static boolean button(GuiGraphics g, Font f, int x, int y, int w, int h, String label, int mx, int my, boolean enabled, int accent) {
        boolean hover = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        int top = !enabled ? 0xFF2A2E3A : hover ? (0xFF000000 | brighten(accent)) : (0xFF000000 | accent);
        int bot = !enabled ? 0xFF1E222C : (0xFF000000 | darken(accent));
        g.fillGradient(x, y, x + w, y + h, top, bot);
        g.fill(x, y, x + w, y + 1, 0x50FFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0x80000000);
        int tc = enabled ? 0xFFFFFFFF : 0xFF707684;
        g.drawCenteredString(f, label, x + w / 2, y + (h - 8) / 2, tc);
        return hover;
    }

    public static boolean tab(GuiGraphics g, Font f, int x, int y, int w, String label, boolean active, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 14;
        g.fill(x, y, x + w, y + 14, active ? 0xFF2E3A60 : hover ? 0xFF222B45 : 0xFF161B2C);
        if (active) g.fill(x, y + 13, x + w, y + 14, GOLD);
        g.drawCenteredString(f, label, x + w / 2, y + 3, active ? GOLD : hover ? TEXT : DIM);
        return hover;
    }

    public static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF2C3550);
        g.fill(x, y, x + 16, y + 16, 0xFF0B0E18);
        g.fill(x, y, x + 16, y + 1, 0xFF05060A);
    }

    public static void lockedSlot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF1A1E2A);
        g.fill(x, y, x + 16, y + 16, 0xFF101320);
        // замочек
        g.fill(x + 5, y + 4, x + 11, y + 5, 0xFF3A4050);
        g.fill(x + 5, y + 4, x + 6, y + 8, 0xFF3A4050);
        g.fill(x + 10, y + 4, x + 11, y + 8, 0xFF3A4050);
        g.fill(x + 4, y + 8, x + 12, y + 13, 0xFF3A4050);
    }

    public static void rarityFrame(GuiGraphics g, int x, int y, int color) {
        int c = 0xFF000000 | color;
        g.fill(x - 1, y - 1, x + 17, y, c); g.fill(x - 1, y + 16, x + 17, y + 17, c);
        g.fill(x - 1, y, x, y + 16, c); g.fill(x + 16, y, x + 17, y + 16, c);
        g.fillGradient(x, y + 10, x + 16, y + 16, 0x00000000, (0x60 << 24) | color);
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h, float f, int color) {
        g.fill(x, y, x + w, y + h, 0xFF0A0C14);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF1C2235);
        int fw = (int) ((w - 2) * Math.max(0, Math.min(1, f)));
        g.fillGradient(x + 1, y + 1, x + 1 + fw, y + h - 1, 0xFF000000 | brighten(color), 0xFF000000 | color);
        g.fill(x + 1, y + 1, x + 1 + fw, y + 2, 0x40FFFFFF);
    }

    public static int brighten(int c) {
        int r = Math.min(255, ((c >> 16) & 255) + 40), gg = Math.min(255, ((c >> 8) & 255) + 40), b = Math.min(255, (c & 255) + 40);
        return r << 16 | gg << 8 | b;
    }

    public static int darken(int c) {
        int r = ((c >> 16) & 255) * 3 / 5, gg = ((c >> 8) & 255) * 3 / 5, b = (c & 255) * 3 / 5;
        return r << 16 | gg << 8 | b;
    }
}
