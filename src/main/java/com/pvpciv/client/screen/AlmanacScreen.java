package com.pvpciv.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pvpciv.client.ui.Scroll;
import com.pvpciv.client.ui.Skin;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Альманах (этаж 3): бестиарий, книга оружия, книга брони. Слева — список, справа — страница. */
public class AlmanacScreen extends PlainScreen {
    private final JsonObject j;
    private int tab = 0, sel = 0;
    private final Scroll list = new Scroll(), page = new Scroll();
    private static final String[] TABS = {"Бестиарий", "Оружие", "Броня"};
    private static final String[] KEYS = {"mobs", "weapons", "armor"};
    private static final int LX = 10, LY = 42, LW = 128, LH = 168, PX = 146, PW = 224, ROW = 13;

    public AlmanacScreen(JsonObject j) { super("Альманах", 380, 220, false); this.j = j; }

    private JsonArray items() { return j.getAsJsonArray(KEYS[tab]); }

    private record Line(String s, int color) {}

    private List<Line> pageLines(JsonObject o) {
        List<Line> out = new ArrayList<>();
        int w = PW - 14;
        java.util.function.BiConsumer<String, Integer> add = (s, c) -> { for (String l : Skin.wrap(font, s, w)) out.add(new Line(l, c)); };
        switch (tab) {
            case 0 -> {
                if (!o.get("known").getAsBoolean()) { add.accept("Страница пуста.", Skin.DIM); add.accept("Убей этого зверя хотя бы раз — Альманах запишет всё сам.", Skin.FAINT); return out; }
                add.accept("Уровень " + o.get("lvl").getAsInt() + " · HP " + o.get("hp").getAsString() + " · урон " + o.get("dmg").getAsString(), Skin.GOLD_L);
                add.accept("Где: " + o.get("where").getAsString(), Skin.TEXT);
                add.accept("Убито: " + o.get("kills").getAsInt() + (o.has("elite") ? " (элитных: " + o.get("elite").getAsInt() + ")" : ""), Skin.DIM);
                out.add(new Line("", 0));
                add.accept(o.has("boss") ? "Личная награда (≥5% урона):" : "Добыча:", Skin.GOLD);
                for (JsonElement d : o.getAsJsonArray("drops")) add.accept(" • " + d.getAsJsonObject().get("n").getAsString() + " — " + d.getAsJsonObject().get("c").getAsString(), Skin.TEXT);
                JsonArray e = o.getAsJsonArray("edrops");
                if (e != null && !e.isEmpty()) {
                    out.add(new Line("", 0));
                    add.accept(o.has("boss") ? "На землю у логова:" : "Элитный вариант:", Skin.GOLD);
                    for (JsonElement d : e) add.accept(" • " + d.getAsJsonObject().get("n").getAsString() + " — " + d.getAsJsonObject().get("c").getAsString(), Skin.TEXT);
                }
            }
            case 1 -> {
                add.accept(o.get("cls").getAsString() + " · тир " + o.get("tier").getAsInt() + " · от «" + o.get("min").getAsString() + "»", Skin.GOLD_L);
                add.accept("Урон: " + o.get("dmg").getAsString() + " (на +10: " + o.get("dmg10").getAsString() + ")", Skin.TEXT);
                if (!o.get("lore").getAsString().isEmpty()) add.accept(o.get("lore").getAsString(), Skin.DIM);
                out.add(new Line("", 0));
                add.accept("Откуда:", Skin.GOLD);
                JsonArray s = o.getAsJsonArray("src");
                if (s.isEmpty()) add.accept(" • особая награда", Skin.FAINT);
                for (JsonElement d : s) add.accept(" • " + d.getAsJsonObject().get("n").getAsString() + " — " + d.getAsJsonObject().get("c").getAsString(), Skin.TEXT);
                out.add(new Line("", 0));
                add.accept("Улучшение: наковальня в квартире — звёзды заточки. До +5 без риска поломки не обойтись: неудача ломает оружие; благословенная звезда — только −1. +11 — особый путь.", Skin.DIM);
            }
            default -> {
                add.accept(o.get("set").getAsString() + " · тир " + o.get("tier").getAsString(), Skin.GOLD_L);
                add.accept("Броня +" + o.get("armor").getAsString() + " · здоровье +" + o.get("hp").getAsString() + " (обычная, +0)", Skin.TEXT);
                out.add(new Line("", 0));
                add.accept("Откуда:", Skin.GOLD);
                JsonArray s = o.getAsJsonArray("src");
                if (s.isEmpty()) add.accept(" • особая награда", Skin.FAINT);
                for (JsonElement d : s) add.accept(" • " + d.getAsJsonObject().get("n").getAsString() + " — " + d.getAsJsonObject().get("c").getAsString(), Skin.TEXT);
                out.add(new Line("", 0));
                add.accept("Улучшение (стол брони):", Skin.GOLD);
                add.accept(" • опыт брони — " + o.get("junk").getAsString(), Skin.TEXT);
                add.accept(" • ядро: " + o.get("core").getAsString(), Skin.TEXT);
                add.accept(" • свиток закалки на +5 и +10; +11 — благословенные и кровавые звёзды, 50%", Skin.TEXT);
            }
        }
        return out;
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        for (int i = 0; i < 3; i++) Skin.tab(g, font, left + 10 + i * 82, top + 22, 78, TABS[i], tab == i, mx, my);
        JsonArray it = items();
        if (it.isEmpty()) return;
        sel = Math.min(sel, it.size() - 1);
        // список
        Skin.inset(g, left + LX - 2, top + LY - 2, LW + 4, LH + 4);
        list.view(LH).content(it.size() * ROW);
        Scroll.begin(g, left + LX, top + LY, LW, LH);
        for (int i = 0; i < it.size(); i++) {
            int y = top + LY + i * ROW - list.off;
            if (!visible(y, ROW, top + LY, LH)) continue;
            JsonObject o = it.get(i).getAsJsonObject();
            boolean known = !o.has("known") || o.get("known").getAsBoolean();
            boolean hv = Skin.row(g, font, left + LX, y, LW - 4, "", sel == i, mx, my);
            g.drawString(font, Skin.fit(font, o.get("name").getAsString(), LW - 14), left + LX + 6, y + 3, sel == i ? Skin.GOLD_L : known ? (hv ? Skin.TEXT : Skin.DIM) : Skin.FAINT, false);
        }
        Scroll.end(g);
        list.bar(g, left + LX + LW - 3, top + LY, LH);
        // страница
        JsonObject o = it.get(sel).getAsJsonObject();
        Skin.inset(g, left + PX - 2, top + LY - 2, PW + 4, LH + 4);
        g.drawString(font, Skin.fit(font, o.get("name").getAsString(), PW - 10), left + PX + 4, top + LY + 2, Skin.GOLD_L, false);
        List<Line> lines = pageLines(o);
        int body = LH - 16;
        page.view(body).content(lines.size() * 11);
        Scroll.begin(g, left + PX, top + LY + 14, PW, body);
        for (int i = 0; i < lines.size(); i++) {
            int y = top + LY + 16 + i * 11 - page.off;
            if (visible(y, 11, top + LY + 14, body) && !lines.get(i).s().isEmpty()) g.drawString(font, lines.get(i).s(), left + PX + 4, y, lines.get(i).color(), false);
        }
        Scroll.end(g);
        page.bar(g, left + PX + PW - 3, top + LY + 14, body);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (in(mx, my, LX, LY, LW, LH)) return list.wheel(sy);
        if (in(mx, my, PX, LY, PW, LH)) return page.wheel(sy);
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < 3; i++) if (in(mx, my, 10 + i * 82, 22, 78, 14)) { tab = i; sel = 0; list.off = 0; page.off = 0; return true; }
        if (in(mx, my, LX, LY, LW, LH)) {
            int i = (int) ((my - top - LY + list.off) / ROW);
            if (i >= 0 && i < items().size()) { sel = i; page.off = 0; return true; }
        }
        return super.mouseClicked(mx, my, b);
    }
}
