package com.pvpciv.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pvpciv.client.ui.Scroll;
import com.pvpciv.client.ui.Skin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Альманах (этаж 3, 0.10 — каталог): бестиарий с 3D-моделями по локациям, оружие и броня карточками с редкостью, раздел «Качество».
 *  Слева — сетка карточек с заголовками разделов, справа — страница выбранного (модель/предмет крупно и описание). */
public class AlmanacScreen extends PlainScreen {
    private final JsonObject j;
    private int tab = 0, sel = 0;
    private final Scroll list = new Scroll(), page = new Scroll();
    private static final String[] TABS = {"Бестиарий", "Оружие", "Броня", "Качество"};
    private static final String[] KEYS = {"mobs", "weapons", "armor", "mobs"};
    private static final int LX = 10, LY = 42, LW = 200, LH = 168, PX = 218, PW = 152, ROW = 13;
    private static final int CW = 44, CH = 52, IW = 44, IH = 38, GAP = 3;
    private final java.util.Map<String, net.minecraft.world.entity.LivingEntity> models = new java.util.HashMap<>();

    public AlmanacScreen(JsonObject j) { super("Альманах", 380, 220, false); this.j = j; }

    private JsonArray items() { return j.getAsJsonArray(KEYS[tab]); }

    private record Line(String s, int color) {}

    /** Строки-заглушки под ряды иконок: «\u0000key:row», ряд = 2 строки (22 px). */
    private static final int PER_ROW = 6;
    private void icons(List<Line> out, String key, int n) {
        for (int r = 0; r * PER_ROW < n; r++) { out.add(new Line("\u0000" + key + ":" + r, 0)); out.add(new Line("", 0)); out.add(new Line("", 0)); }
    }

    private List<Line> pageLines(JsonObject o) {
        List<Line> out = new ArrayList<>();
        int w = PW - 12;
        java.util.function.BiConsumer<String, Integer> add = (s, c) -> { for (String l : Skin.wrap(font, s, w)) out.add(new Line(l, c)); };
        switch (tab) {
            case 0 -> {
                if (!o.get("known").getAsBoolean()) { add.accept("Страница пуста.", Skin.DIM); add.accept("Убей этого зверя хотя бы раз — Альманах запишет всё сам.", Skin.FAINT); return out; }
                add.accept("Уровень " + o.get("lvl").getAsInt() + " · HP " + o.get("hp").getAsString() + " · урон " + o.get("dmg").getAsString(), Skin.GOLD_L);
                add.accept("Где: " + o.get("where").getAsString(), Skin.TEXT);
                add.accept("Убито: " + o.get("kills").getAsInt() + (o.has("elite") ? " (элитных: " + o.get("elite").getAsInt() + ")" : ""), Skin.DIM);
                out.add(new Line("", 0));
                add.accept(o.has("boss") ? "Личная награда (≥5% урона):" : "Добыча:", Skin.GOLD);
                icons(out, "drops", o.getAsJsonArray("drops").size());   // 0.10.5: иконки предметов, шанс под ними, название — при наведении
                JsonArray e = o.getAsJsonArray("edrops");
                if (e != null && !e.isEmpty()) {
                    out.add(new Line("", 0));
                    add.accept(o.has("boss") ? "На землю у логова:" : "Элитный вариант:", Skin.GOLD);
                    icons(out, "edrops", e.size());
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

    // ------------------------------------------------------------------ раскладка каталога
    /** Элемент сетки: заголовок раздела (idx = -1) или карточка (idx — номер в массиве). */
    private record Cell(String header, int idx, int x, int y) {}
    private List<Cell> cells = new ArrayList<>();
    private int gridH;

    private void layout() {
        cells = new ArrayList<>();
        JsonArray it = items();
        int cw = tab == 0 ? CW : IW, ch = tab == 0 ? CH : IH, perRow = (LW - 6) / (cw + GAP);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < it.size(); i++) order.add(i);
        java.util.function.Function<Integer, String> group = i -> {
            JsonObject o = it.get(i).getAsJsonObject();
            if (tab == 0) return o.has("loc") ? o.get("loc").getAsString() : "";
            return "Тир " + (o.get("tier").isJsonPrimitive() ? o.get("tier").getAsString().replace(".0", "").replace(".5", "+") : "");
        };
        java.util.List<String> groups = new ArrayList<>();
        for (int i : order) { String gname = group.apply(i); if (!groups.contains(gname)) groups.add(gname); }
        int y = 0;
        for (String gname : groups) {
            cells.add(new Cell(gname, -1, 0, y)); y += 13;
            int col = 0;
            for (int i : order) {
                if (!group.apply(i).equals(gname)) continue;
                cells.add(new Cell(null, i, 3 + col * (cw + GAP), y));
                if (++col >= perRow) { col = 0; y += ch + GAP; }
            }
            if (col > 0) y += ch + GAP;
            y += 2;
        }
        gridH = y;
    }

    private net.minecraft.world.entity.LivingEntity model(String id) {
        return models.computeIfAbsent(id, k -> {
            var t = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.parse(k));
            var e = t.create(minecraft.level);
            return e instanceof net.minecraft.world.entity.LivingEntity le ? le : null;
        });
    }

    private void drawModel(GuiGraphics g, net.minecraft.world.entity.LivingEntity e, int x1, int y1, int x2, int y2, int mx, int my, float k) {
        if (e == null) return;
        float h = Math.max(e.getBbHeight(), e.getBbWidth() * 1.1f);
        int scale = Math.max(6, Math.round((y2 - y1) * 0.85f / h * k));
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, scale, 0.0625f, mx, my, e);
    }

    private ItemStack stack(JsonObject o) {
        var it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(o.get("id").getAsString()));
        ItemStack st = new ItemStack(it);
        com.pvpciv.loot.Gear.withRarity(st, com.pvpciv.loot.Rarity.byId(o.has("rar") ? o.get("rar").getAsInt() : 0));
        return st;
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        for (int i = 0; i < 4; i++) Skin.tab(g, font, left + 10 + i * 82, top + 22, 78, TABS[i], tab == i, mx, my);
        if (tab == 3) { quality(g, mx, my); return; }
        JsonArray it = items();
        if (it.isEmpty()) return;
        sel = Math.min(sel, it.size() - 1);
        layout();
        Skin.inset(g, left + LX - 2, top + LY - 2, LW + 4, LH + 4);
        list.view(LH).content(gridH);
        Scroll.begin(g, left + LX, top + LY, LW, LH);
        ItemStack tip = ItemStack.EMPTY;
        for (Cell c : cells) {
            int y = top + LY + c.y - list.off, x = left + LX + c.x;
            if (c.idx < 0) {
                if (!visible(y, 12, top + LY, LH)) continue;
                g.drawString(font, "◆ " + c.header, x + 4, y + 2, Skin.GOLD, false);
                g.fill(x + 10 + font.width("◆ " + c.header), y + 6, left + LX + LW - 8, y + 7, 0x40D9A441);
                continue;
            }
            int cw = tab == 0 ? CW : IW, ch = tab == 0 ? CH : IH;
            if (!visible(y, ch, top + LY, LH)) continue;
            JsonObject o = it.get(c.idx).getAsJsonObject();
            boolean known = !o.has("known") || o.get("known").getAsBoolean();
            boolean hv = Skin.hover(mx, my, x, y, cw, ch) && my >= top + LY && my < top + LY + LH;
            int rc = tab == 0 ? (o.has("boss") ? 0xFFFF5A5A : 0xFFD9A441) : (0xFF000000 | com.pvpciv.loot.Rarity.byId(o.get("rar").getAsInt()).color);
            // карточка: рамка цвета редкости/типа, утопленный фон
            g.fill(x, y, x + cw, y + ch, sel == c.idx ? rc : hv ? Skin.alpha(rc, 0.75f) : 0xFF2A2236);
            g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1, 0xFF120E1A);
            g.fillGradient(x + 1, y + 1, x + cw - 1, y + ch - 12, 0xFF1E1828, 0xFF120E1A);
            if (tab == 0) {
                if (known) drawModel(g, model(o.get("id").getAsString()), x + 2, y + 2, x + cw - 2, y + ch - 12, x + cw / 2 + 30, y + 8, 1f);
                else g.drawCenteredString(font, "?", x + cw / 2, y + ch / 2 - 10, Skin.FAINT);
                String nm = known ? o.get("name").getAsString() : "???";
                small(g, Skin.fit(font, nm, (int) ((cw - 4) / 0.6f)), x + cw / 2, y + ch - 9, known ? Skin.TEXT : Skin.FAINT, 0.6f);
                if (known && o.has("lvl")) { g.fill(x + 1, y + 1, x + 12, y + 8, 0xC0000000); small(g, o.get("lvl").getAsString(), x + 6, y + 2, Skin.GOLD_L, 0.6f); }
            } else {
                ItemStack st = stack(o);
                g.pose().pushPose();
                g.pose().translate(x + cw / 2f - 12, y + 3, 0);
                g.pose().scale(1.5f, 1.5f, 1f);
                g.renderItem(st, 0, 0);
                g.pose().popPose();
                small(g, com.pvpciv.loot.Rarity.byId(o.get("rar").getAsInt()).title, x + cw / 2, y + ch - 9, rc, 0.6f);
                if (hv) tip = st;
            }
        }
        Scroll.end(g);
        list.bar(g, left + LX + LW - 3, top + LY, LH);
        // страница
        JsonObject o = it.get(sel).getAsJsonObject();
        Skin.inset(g, left + PX - 2, top + LY - 2, PW + 4, LH + 4);
        boolean known = !o.has("known") || o.get("known").getAsBoolean();
        int pic = 54;
        g.fillGradient(left + PX, top + LY, left + PX + PW, top + LY + pic, 0xFF221A30, 0xFF120E1A);
        if (tab == 0) { if (known) drawModel(g, model(o.get("id").getAsString()), left + PX + 20, top + LY + 2, left + PX + PW - 20, top + LY + pic - 2, mx, my, 1.1f); }
        else {
            g.pose().pushPose();
            g.pose().translate(left + PX + PW / 2f - 20, top + LY + 7, 0);
            g.pose().scale(2.5f, 2.5f, 1f);
            g.renderItem(stack(o), 0, 0);
            g.pose().popPose();
        }
        g.drawString(font, Skin.fit(font, o.get("name").getAsString(), PW - 8), left + PX + 4, top + LY + pic + 2, Skin.GOLD_L, false);
        List<Line> lines = pageLines(o);
        int body = LH - pic - 14;
        page.view(body).content(lines.size() * 11);
        Scroll.begin(g, left + PX, top + LY + pic + 12, PW, body);
        for (int i = 0; i < lines.size(); i++) {
            int y = top + LY + pic + 14 + i * 11 - page.off;
            String ls = lines.get(i).s();
            if (ls.startsWith("\u0000")) { tip = iconRow(g, o, ls.substring(1), left + PX + 6, y + 1, mx, my, top + LY + pic + 12, body, tip); continue; }
            if (visible(y, 11, top + LY + pic + 12, body) && !ls.isEmpty()) g.drawString(font, ls, left + PX + 4, y, lines.get(i).color(), false);
        }
        Scroll.end(g);
        page.bar(g, left + PX + PW - 3, top + LY + pic + 12, body);
        if (!tip.isEmpty()) g.renderTooltip(font, tip, mx, my);
    }

    /** Ряд иконок добычи: слот, предмет, шанс под ним; при наведении — название. Возвращает предмет для подсказки. */
    private ItemStack iconRow(GuiGraphics g, JsonObject o, String spec, int x, int y, int mx, int my, int vy, int vh, ItemStack tip) {
        String key = spec.substring(0, spec.indexOf(':'));
        int row = Integer.parseInt(spec.substring(spec.indexOf(':') + 1));
        JsonArray arr = o.getAsJsonArray(key);
        for (int k = 0; k < PER_ROW; k++) {
            int idx = row * PER_ROW + k;
            if (idx >= arr.size()) break;
            JsonObject d = arr.get(idx).getAsJsonObject();
            String id = d.has("id") ? d.get("id").getAsString() : null;
            if (id == null && d.has("ids")) { JsonArray ids = d.getAsJsonArray("ids"); id = ids.get((int) (System.currentTimeMillis() / 900 % ids.size())).getAsString(); }
            int ix = x + k * 24;
            if (y + 22 < vy || y > vy + vh) continue;
            Skin.slot(g, ix, y);
            ItemStack st = id == null ? ItemStack.EMPTY : new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)));
            if (!st.isEmpty()) g.renderItem(st, ix, y);
            if (d.has("cnt")) { g.pose().pushPose(); g.pose().translate(0, 0, 200); small(g, d.get("cnt").getAsString(), ix + 12, y + 10, 0xFFFFFFFF, 0.6f); g.pose().popPose(); }
            small(g, d.get("c").getAsString(), ix + 8, y + 18, Skin.DIM, 0.6f);
            if (Skin.hover(mx, my, ix, y, 16, 16) && my >= vy && my < vy + vh) {
                ItemStack t = st.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.PAPER) : st.copy();
                t.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(d.get("n").getAsString() + " — " + d.get("c").getAsString()));
                tip = t;
            }
        }
        return tip;
    }

    private void small(GuiGraphics g, String s, int cx, int y, int color, float k) {
        g.pose().pushPose();
        g.pose().translate(cx - font.width(s) * k / 2f, y, 0);
        g.pose().scale(k, k, 1f);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ качество
    private final java.util.Map<Integer, List<ItemStack>> samples = new java.util.HashMap<>();

    private void quality(GuiGraphics g, int mx, int my) {
        Skin.inset(g, left + 8, top + LY - 2, w - 16, LH + 4);
        var vals = com.pvpciv.loot.Rarity.values();
        int total = 0, totalM = 0;
        for (var r : vals) { totalM += r.weight; if (r != com.pvpciv.loot.Rarity.MYTHIC) total += r.weight; }
        g.drawString(font, "Качество — множитель урона, брони и здоровья. Шанс — для обычной добычи.", left + 14, top + LY + 2, Skin.DIM, false);
        ItemStack tip = ItemStack.EMPTY;
        JsonArray pool = new JsonArray();
        pool.addAll(j.getAsJsonArray("weapons")); pool.addAll(j.getAsJsonArray("armor"));
        for (int i = 0; i < vals.length; i++) {
            var r = vals[i];
            int y = top + LY + 16 + i * 25, x = left + 14;
            int c = 0xFF000000 | r.color;
            g.fill(x, y, x + 3, y + 21, c);
            g.drawString(font, r.title, x + 8, y + 2, c, true);
            String ch = r == com.pvpciv.loot.Rarity.MYTHIC ? String.format(java.util.Locale.ROOT, "%.2f%% · боссы, тайники", 100.0 * r.weight / totalM)
                    : String.format(java.util.Locale.ROOT, "%.1f%%", 100.0 * r.weight / total);
            g.drawString(font, "×" + String.format(java.util.Locale.ROOT, "%.2f", r.mult) + "   шанс " + ch, x + 8, y + 12, Skin.DIM, false);
            List<ItemStack> ex = samples.computeIfAbsent(i, k -> {
                List<ItemStack> l = new ArrayList<>();
                var rnd = new java.util.Random(k * 31L + 7);
                for (int n = 0; n < 5 && !pool.isEmpty(); n++) {
                    JsonObject o = pool.get(rnd.nextInt(pool.size())).getAsJsonObject().deepCopy();
                    o.addProperty("rar", k);
                    l.add(stack(o));
                }
                return l;
            });
            for (int n = 0; n < ex.size(); n++) {
                int ix = left + 220 + n * 28, iy = y + 2;
                g.fill(ix - 2, iy - 2, ix + 18, iy + 18, Skin.alpha(c, 0.55f));
                g.fill(ix - 1, iy - 1, ix + 17, iy + 17, 0xFF120E1A);
                g.renderItem(ex.get(n), ix, iy);
                if (Skin.hover(mx, my, ix, iy, 16, 16)) tip = ex.get(n);
            }
        }
        if (!tip.isEmpty()) g.renderTooltip(font, tip, mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (in(mx, my, LX, LY, LW, LH)) return list.wheel(sy);
        if (in(mx, my, PX, LY, PW, LH)) return page.wheel(sy);
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < 4; i++) if (in(mx, my, 10 + i * 82, 22, 78, 14)) { tab = i; sel = 0; list.off = 0; page.off = 0; return true; }
        if (tab != 3 && in(mx, my, LX, LY, LW, LH)) {
            int cw = tab == 0 ? CW : IW, ch = tab == 0 ? CH : IH;
            for (Cell c : cells) {
                if (c.idx < 0) continue;
                int x = left + LX + c.x, y = top + LY + c.y - list.off;
                if (mx >= x && mx < x + cw && my >= y && my < y + ch) { sel = c.idx; page.off = 0; return true; }
            }
        }
        return super.mouseClicked(mx, my, b);
    }
}
