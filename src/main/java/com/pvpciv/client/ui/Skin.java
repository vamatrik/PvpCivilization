package com.pvpciv.client.ui;

import com.pvpciv.PvpCiv;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Набор инструментов интерфейса цивилизации. ВСЕ экраны и HUD рисуются только через него.
 * Спрайты лежат в textures/gui/sprites (плотность x2), генерируются tools/gui_sprites.py.
 * Координаты и размеры везде — в GUI-пикселях. См. GUI_GUIDE.md.
 */
public final class Skin {
    // палитра
    public static final int GOLD = 0xFFD9A441, GOLD_L = 0xFFFFE08A, GOLD_D = 0xFF7A5420;
    public static final int TEXT = 0xFFECE6F5, DIM = 0xFF9A90B0, FAINT = 0xFF5E566F;
    public static final int GOOD = 0xFF7CFC9A, WARN = 0xFFFFD166, BAD = 0xFFFF5A5A, EMBER = 0xFFFF8C42, ICE = 0xFF7FDBFF;

    private static final Map<String, ResourceLocation> IDS = new HashMap<>();
    private static ResourceLocation id(String n) { return IDS.computeIfAbsent(n, PvpCiv::id); }

    /** Спрайт (в т.ч. nine-slice) в GUI-пикселях; текстура x2 выводится с масштабом 0.5. */
    public static void spr(GuiGraphics g, String name, int x, int y, int w, int h) {
        g.flush();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.blitSprite(id(name), 0, 0, w * 2, h * 2);
        g.pose().popPose();
    }

    /** Спрайт с оттенком и прозрачностью (argb). */
    public static void spr(GuiGraphics g, String name, int x, int y, int w, int h, int argb) {
        g.setColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
        spr(g, name, x, y, w, h);
        g.setColor(1, 1, 1, 1);
    }

    // ---------------------------------------------------------------- контейнеры
    /** Главная панель окна: фон с решёткой, золотая рама, кружево, камни в углах. Минимум 28×28. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) { spr(g, "panel", x, y, w, h); }
    /** Малая плита для HUD и всплывающих блоков. */
    public static void plate(GuiGraphics g, int x, int y, int w, int h) { spr(g, "plate", x, y, w, h); }
    /** Утопленная область внутри панели. */
    public static void inset(GuiGraphics g, int x, int y, int w, int h) { spr(g, "inset", x, y, w, h); }

    /** Лента-заголовок, центрируется по cx; y — верх ленты (высота 18). */
    public static void header(GuiGraphics g, Font f, int cx, int y, String title) {
        int w = Math.max(80, f.width(title) + 44);
        spr(g, "header", cx - w / 2, y, w, 18);
        g.drawCenteredString(f, title, cx, y + 5, GOLD_L);
    }

    public static void divider(GuiGraphics g, int cx, int y) { spr(g, "divider", cx - 20, y, 40, 7); }

    // ---------------------------------------------------------------- элементы управления
    public static boolean hover(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    /** Кнопка высотой 16. ember=true — главная (действие), иначе обычная. Возвращает hover. */
    public static boolean button(GuiGraphics g, Font f, int x, int y, int w, String label, int mx, int my, boolean enabled, boolean ember) {
        boolean hv = enabled && hover(mx, my, x, y, w, 16);
        String s = !enabled ? "button_off" : ember ? (hv ? "button_ember_hover" : "button_ember") : (hv ? "button_hover" : "button");
        spr(g, s, x, y, w, 16);
        g.drawCenteredString(f, label, x + w / 2, y + 4, enabled ? (hv ? 0xFFFFFFFF : 0xFFF4EAD8) : 0xFF6E6880);
        return hv;
    }

    /** Вкладка высотой 14. */
    public static boolean tab(GuiGraphics g, Font f, int x, int y, int w, String label, boolean active, int mx, int my) {
        boolean hv = hover(mx, my, x, y, w, 14);
        spr(g, active ? "tab_active" : "tab", x, y, w, 14);
        g.drawCenteredString(f, label, x + w / 2, y + 3, active ? GOLD_L : hv ? TEXT : DIM);
        return hv;
    }

    /** Строка-переключатель в списке (фильтры и т.п.), высота 13. */
    public static boolean row(GuiGraphics g, Font f, int x, int y, int w, String label, boolean active, int mx, int my) {
        boolean hv = hover(mx, my, x, y, w, 13);
        if (active) { g.fill(x, y, x + w, y + 13, 0x50D9A441); g.fill(x, y, x + 2, y + 13, GOLD); }
        else if (hv) g.fill(x, y, x + w, y + 13, 0x30FFFFFF);
        g.drawString(f, label, x + 6, y + 3, active ? GOLD_L : hv ? TEXT : DIM, false);
        return hv;
    }

    // ---------------------------------------------------------------- слоты
    /** Рамка слота 18×18 вокруг предмета, стоящего в (itemX, itemY). */
    public static void slot(GuiGraphics g, int itemX, int itemY) { spr(g, "slot", itemX - 1, itemY - 1, 18, 18); }
    public static void slotLocked(GuiGraphics g, int itemX, int itemY) { spr(g, "slot_locked", itemX - 1, itemY - 1, 18, 18); }
    /** Силуэт-подсказка в пустом слоте: weapon, star, helmet, chest, legs, boots, shield, artifact. */
    public static void hint(GuiGraphics g, String kind, int itemX, int itemY) { spr(g, "hint_" + kind, itemX, itemY, 16, 16); }

    /** Свечение редкости под предметом. */
    public static void rarity(GuiGraphics g, int itemX, int itemY, int rgb) {
        int c = 0xFF000000 | rgb;
        g.fill(itemX - 1, itemY - 1, itemX + 17, itemY, c); g.fill(itemX - 1, itemY + 16, itemX + 17, itemY + 17, c);
        g.fill(itemX - 1, itemY, itemX, itemY + 16, c); g.fill(itemX + 16, itemY, itemX + 17, itemY + 16, c);
        g.fillGradient(itemX, itemY + 7, itemX + 16, itemY + 16, rgb & 0xFFFFFF, 0x70000000 | (rgb & 0xFFFFFF));
    }

    // ---------------------------------------------------------------- полоски и значки
    /** Резная полоска высотой 12 с заливкой f∈[0..1]; gem — red/amber/blue/violet/green или null. */
    public static void bar(GuiGraphics g, int x, int y, int w, float f, int colTop, int colBot, String gem) {
        f = Math.max(0, Math.min(1, f));
        g.fill(x + 3, y + 2, x + w - 3, y + 10, 0xFF0D0A14);
        int fw = Math.round((w - 6) * f);
        if (fw > 0) {
            g.fillGradient(x + 3, y + 2, x + 3 + fw, y + 10, colTop, colBot);
            g.fill(x + 3, y + 3, x + 3 + fw, y + 4, 0x55FFFFFF);
            if (f < 1) g.fill(x + 2 + fw, y + 2, x + 3 + fw, y + 10, 0x90FFFFFF);
        }
        spr(g, "bar_frame", x, y, w, 12);
        if (gem != null) { gem(g, gem, x - 5, y + 1); gem(g, gem, x + w - 5, y + 1); }
    }

    /** Тонкая полоска без рамы (высота h), для опыта/прогресса. */
    public static void thinBar(GuiGraphics g, int x, int y, int w, int h, float f, int colTop, int colBot) {
        f = Math.max(0, Math.min(1, f));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF07060B);
        g.fill(x, y, x + w, y + h, 0xFF1C172A);
        int fw = Math.round(w * f);
        if (fw > 0) { g.fillGradient(x, y, x + fw, y + h, colTop, colBot); g.fill(x, y, x + fw, y + 1, 0x50FFFFFF); }
    }

    public static void gem(GuiGraphics g, String color, int x, int y) { spr(g, "gem_" + color, x, y, 10, 10); }
    /** Значок 10×10: spark, heart, food, shield, lock, arrow, clock; skull — 12×12. */
    public static void icon(GuiGraphics g, String name, int x, int y) {
        int s = name.equals("skull") ? 12 : 10;
        spr(g, "icon_" + name, x, y, s, s);
    }

    /** «✦ 1234» с иконкой, выравнивание по правому краю xRight. Возвращает левый край. */
    public static int money(GuiGraphics g, Font f, int xRight, int y, long amount, int color) {
        String s = String.valueOf(amount);
        int x = xRight - f.width(s);
        g.drawString(f, s, x, y, color, true);
        icon(g, "spark", x - 12, y - 1);
        return x - 12;
    }

    /** Мягкое круглое свечение радиуса r. */
    public static void glow(GuiGraphics g, int cx, int cy, int r, int argb) { spr(g, "glow", cx - r, cy - r, r * 2, r * 2, argb); }

    /** Строка звёзд заточки ★☆ (10 шт.). */
    public static void stars(GuiGraphics g, Font f, int x, int y, int stars, int color) {
        StringBuilder a = new StringBuilder(), b = new StringBuilder();
        for (int i = 0; i < 10; i++) (i < stars ? a : b).append(i < stars ? "★" : "☆");
        g.drawString(f, a.toString(), x, y, color, true);
        g.drawString(f, b.toString(), x + f.width(a.toString()), y, FAINT, false);
    }

    public static String fit(Font f, String s, int w) { return f.width(s) <= w ? s : f.plainSubstrByWidth(s, w - 6) + "…"; }

    public static int alpha(int argb, float a) { return (Math.round(((argb >>> 24) & 255) * Math.max(0, Math.min(1, a))) << 24) | (argb & 0xFFFFFF); }
}
