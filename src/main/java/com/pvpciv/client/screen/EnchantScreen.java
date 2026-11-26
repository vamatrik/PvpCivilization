package com.pvpciv.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Skin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.2 — молекулярный стол NaZa, свои слоты под всё.
 * Слева — снаряжение иконками (колесо — прокрутка), под ним — ресурсы в сумке (щелчок — положить в стол).
 * В центре — слот предмета, слоты лазуратрона и катализаторов в столе (щелчок — вернуть в сумку), гнёзда модулей.
 * Справа — синтез x1/x3/x10 и варианты; «Оставить старое» — всегда.
 */
public class EnchantScreen extends PlainScreen {
    private static final int[] MODES = {1, 3, 10};
    private static final int CYAN = 0xFF40D0FF;
    private static final int[] TIER_COL = {0xFF2A2236, 0xFF40D0FF, 0xFF60FF90, 0xFFC060FF};
    private static final int[] RANK_COL = {0, 0xFFCFCFCF, 0xFF6AD0FF, 0xFFC070FF, 0xFFFFB030};
    private static final String[] RES = {"laz", "cmp", "focus", "reso"};
    private static final String[] RES_ID = {"pvpciv:lazuratron", "pvpciv:cmp_lazuratron", "pvpciv:cat_focus", "pvpciv:cat_resonator"};
    private static final String[] RES_NAME = {"Лазуратрон", "Сжатый лазуратрон (= 8)", "Фокус", "Резонатор"};
    private static final String[] RES_TIP = {"Плата за синтез.", "Плата за синтез: один сжатый = 8 обычных (сдача — обычным).",
            "Каждый синтез тратит 1: +1 вариант на выбор.", "Каждый синтез тратит 1: случайный вариант на ранг выше."};
    private static final int COLS = 8, ROWS = 5, CELL = 19;
    private static final int LX = 8, LW = 166, MX = 180, MW = 112, RX = 298;

    public EnchantScreen() { super("Молекулярный стол", 456, 252, false); }

    private JsonObject j() { return ClientState.ench; }
    private int scroll;
    private final Map<String, ItemStack> icons = new HashMap<>();
    private ItemStack icon(String id) {
        return icons.computeIfAbsent(id, k -> { var it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(k)); return new ItemStack(it); });
    }
    private JsonArray cand() { return j().has("cand") ? j().getAsJsonArray("cand") : new JsonArray(); }
    private int i(String k) { return j().has(k) ? j().get(k).getAsInt() : 0; }
    private String s(String k) { return j().has(k) ? j().get(k).getAsString() : ""; }
    private int res(String where, String k) { return j().has(where) && j().getAsJsonObject(where).has(k) ? j().getAsJsonObject(where).get(k).getAsInt() : 0; }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int rows = (cand().size() + COLS - 1) / COLS;
        scroll = Math.max(0, Math.min(Math.max(0, rows - ROWS), scroll - (int) Math.signum(dy)));
        return true;
    }

    private void slotBox(GuiGraphics g, int x, int y, int size, int border) {
        g.fill(x, y, x + size, y + size, border);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF0E0B14);
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        if (j() == null) return;
        List<Component> tip = null;
        int ly = top + 14;
        // ---------------- слева: снаряжение
        Skin.inset(g, left + LX, ly, LW, h - 24);
        g.drawString(font, "Снаряжение", left + LX + 5, ly + 4, Skin.GOLD_L, false);
        JsonArray cand = cand();
        int gx = left + LX + 6, gy = ly + 15;
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) {
            int idx = (r + scroll) * COLS + c, x = gx + c * CELL, y = gy + r * CELL;
            slotBox(g, x, y, 18, 0xFF2A2236);
            if (idx >= cand.size()) continue;
            JsonObject o = cand.get(idx).getAsJsonObject();
            boolean hv = Skin.hover(mx, my, x, y, 18, 18);
            int rc = 0xFF000000 | o.get("rar").getAsInt();
            g.fill(x, y, x + 18, y + 18, hv ? 0xFFFFFFFF : rc);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0E0B14);
            g.renderItem(icon(o.get("id").getAsString()), x + 1, y + 1);
            if (!o.get("ench").getAsString().isEmpty()) g.fill(x + 13, y + 2, x + 16, y + 5, CYAN);
            if (hv) {
                tip = new ArrayList<>();
                tip.add(Component.literal(o.get("name").getAsString()).withStyle(st -> st.withColor(rc & 0xFFFFFF)));
                int stars = o.get("stars").getAsInt();
                if (stars > 0) tip.add(Component.literal("+" + stars).withStyle(net.minecraft.ChatFormatting.GOLD));
                String e = o.get("ench").getAsString();
                tip.add(Component.literal(e.isEmpty() ? "без зачарования" : "⚛ " + e).withStyle(st -> st.withColor(e.isEmpty() ? 0x7A7088 : 0x40D0FF)));
                tip.add(Component.literal("щелчок — положить в стол").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
        int rows = (cand.size() + COLS - 1) / COLS;
        if (cand.isEmpty()) Skin.paraCentered(g, font, "Нет оружия и брони в хотбаре, броне и сумке", left + LX + LW / 2, gy + 40, LW - 20, 10, 3, Skin.FAINT);
        if (rows > ROWS) {   // полоса прокрутки
            int bh = ROWS * CELL, th = Math.max(10, bh * ROWS / rows), ty = gy + (bh - th) * scroll / Math.max(1, rows - ROWS);
            g.fill(gx + COLS * CELL + 1, gy, gx + COLS * CELL + 3, gy + bh, 0xFF1A1622);
            g.fill(gx + COLS * CELL + 1, ty, gx + COLS * CELL + 3, ty + th, 0xFF8A70B0);
        }
        // ресурсы в сумке
        int by = gy + ROWS * CELL + 8;
        g.drawString(font, "В сумке (щелчок — в стол)", left + LX + 5, by, Skin.DIM, false);
        for (int k = 0; k < 4; k++) {
            int x = gx + k * 40, y = by + 11, n = res("bag", RES[k]);
            boolean hv = n > 0 && Skin.hover(mx, my, x, y, 18, 18);
            slotBox(g, x, y, 18, hv ? 0xFFFFE08A : 0xFF2A2236);
            g.renderItem(icon(RES_ID[k]), x + 1, y + 1);
            if (n == 0) g.fill(x + 1, y + 1, x + 17, y + 17, 0xB00E0B14);
            g.drawString(font, "×" + n, x + 20, y + 5, n > 0 ? Skin.TEXT : Skin.FAINT, false);
            if (Skin.hover(mx, my, x, y, 18, 18)) tip = List.of(Component.literal(RES_NAME[k]), Component.literal(RES_TIP[k]).withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal(n > 0 ? "щелчок — положить все " + n + " в стол" : "в сумке нет").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
        Skin.paraCentered(g, font, "Колесо мыши — прокрутка снаряжения", left + LX + LW / 2, top + h - 20, LW - 10, 9, 1, Skin.FAINT);

        // ---------------- центр: стол
        int cx = left + MX;
        Skin.inset(g, cx, ly, MW, h - 24);
        g.drawString(font, "Стол", cx + 5, ly + 4, Skin.GOLD_L, false);
        boolean ok = j().has("ok") && j().get("ok").getAsBoolean();
        int sx = cx + MW / 2 - 13, sy = ly + 16;
        boolean hvItem = ok && Skin.hover(mx, my, sx, sy, 26, 26);
        slotBox(g, sx, sy, 26, ok ? (0xFF000000 | i("rar")) : 0xFF3A3048);
        if (ok) {
            g.pose().pushPose(); g.pose().translate(sx + 1, sy + 1, 0); g.pose().scale(1.5f, 1.5f, 1);
            g.renderItem(icon(s("itemId")), 0, 0);
            g.pose().popPose();
            if (hvItem) tip = List.of(Component.literal(s("item")), Component.literal(s("cur").isEmpty() ? "без зачарования" : "⚛ " + s("cur")).withStyle(net.minecraft.ChatFormatting.AQUA),
                    Component.literal("щелчок — забрать в сумку").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        } else g.drawCenteredString(font, "?", sx + 13, sy + 9, Skin.FAINT);
        int ty = sy + 30;
        if (ok) {
            g.drawCenteredString(font, Skin.fit(font, s("item"), MW - 8), cx + MW / 2, ty, Skin.TEXT);
            String cur = s("cur");
            g.drawCenteredString(font, Skin.fit(font, cur.isEmpty() ? "без зачарования" : "⚛ " + cur.split(" — ")[0], MW - 8), cx + MW / 2, ty + 10, cur.isEmpty() ? Skin.FAINT : CYAN);
        } else Skin.paraCentered(g, font, "Выбери предмет слева", cx + MW / 2, ty + 2, MW - 10, 10, 2, Skin.FAINT);
        // ресурсы в столе
        int ry = ty + 26;
        g.drawString(font, "В столе", cx + 5, ry, Skin.DIM, false);
        for (int k = 0; k < 4; k++) {
            int x = cx + 6 + (k % 2) * 52, y = ry + 11 + (k / 2) * 22, n = res("table", RES[k]);
            boolean hv = Skin.hover(mx, my, x, y, 18, 18);
            slotBox(g, x, y, 18, hv && n > 0 ? 0xFFFFE08A : n > 0 ? CYAN : 0xFF2A2236);
            g.renderItem(icon(RES_ID[k]), x + 1, y + 1);
            if (n == 0) g.fill(x + 1, y + 1, x + 17, y + 17, 0xB00E0B14);
            g.drawString(font, String.valueOf(n), x + 21, y + 5, n > 0 ? Skin.TEXT : Skin.FAINT, false);
            if (hv) tip = List.of(Component.literal(RES_NAME[k] + " в столе: " + n), Component.literal(RES_TIP[k]).withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal(n > 0 ? "щелчок — вернуть в сумку" : "положи из сумки (слева)").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
        // модули
        int my0 = ry + 60;
        g.drawString(font, "Модули (ПКМ у стола)", cx + 5, my0, Skin.DIM, false);
        var mods = j().has("mods") ? j().getAsJsonArray("mods") : new JsonArray();
        for (int k = 0; k < 8; k++) {
            int t = k < mods.size() ? mods.get(k).getAsInt() : 0;
            int x = cx + 6 + (k % 4) * 25, y = my0 + 11 + (k / 4) * 15;
            g.fill(x, y, x + 23, y + 13, 0xFF0C0A12);
            g.fill(x + 1, y + 1, x + 22, y + 12, t > 0 ? (TIER_COL[t] & 0x60FFFFFF) : 0xFF16121E);
            g.drawCenteredString(font, t == 0 ? "—" : new String[]{"", "I", "II", "III"}[t], x + 12, y + 3, t > 0 ? TIER_COL[t] : Skin.FAINT);
        }
        var pool = j().has("pool") ? j().getAsJsonArray("pool") : new JsonArray();
        String pl = "Доступно чар: " + pool.size() + " (?)";
        int ply = my0 + 44;
        g.drawString(font, pl, cx + 5, ply, Skin.DIM, false);
        if (Skin.hover(mx, my, cx + 5, ply - 1, font.width(pl), 10)) {
            tip = new ArrayList<>();
            tip.add(Component.literal("Может выпасть:"));
            for (var e : pool) tip.add(Component.literal("· " + e.getAsString()).withStyle(net.minecraft.ChatFormatting.AQUA));
        }

        // ---------------- справа: синтез
        int rx = left + RX, rw = w - RX - 8;
        Skin.inset(g, rx, ly, rw, h - 24);
        g.drawString(font, "Синтез", rx + 5, ly + 4, Skin.GOLD_L, false);
        int cost = i("cost"), tl = i("tlaz");
        String lz = "лазуратрона: " + tl;
        g.drawString(font, lz, rx + rw - 5 - font.width(lz), ly + 4, ok && tl >= cost ? Skin.DIM : Skin.BAD, false);
        int bw = (rw - 10) / 3;
        for (int k = 0; k < 3; k++) {
            int m = MODES[k], bx = rx + 5 + k * bw;
            boolean can = ok && tl >= cost * m;
            Skin.button(g, font, bx, ly + 15, bw - 3, "x" + m + "·" + cost * m, mx, my, can, k == 0);
            if (Skin.hover(mx, my, bx, ly + 15, bw - 3, 16)) tip = List.of(Component.literal("Синтез x" + m + " — " + cost * m + " лазуратрона"),
                    Component.literal(m == 1 ? "Варианты = модули стола" : m == 3 ? "+1 вариант, ранг — лучший из 2 бросков" : "+2 варианта, ранг — лучший из 3 бросков").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("Фокус и Резонатор в столе — по 1 за синтез").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
        var opts = j().has("opts") ? j().getAsJsonArray("opts") : new JsonArray();
        if (opts.isEmpty()) Skin.paraCentered(g, font, ok ? "Выбери режим — стол предложит варианты, оставишь один (или старое)." : "Сначала положи предмет в стол.", rx + rw / 2, ly + 80, rw - 16, 10, 4, Skin.FAINT);
        for (int k = 0; k < opts.size() && k < 8; k++) {
            JsonObject o = opts.get(k).getAsJsonObject();
            int oy = ly + 36 + k * 22;
            boolean hv = Skin.hover(mx, my, rx + 4, oy, rw - 8, 20);
            int rank = o.get("rank").getAsInt();
            g.fill(rx + 4, oy, rx + rw - 4, oy + 20, hv ? CYAN : 0xFF2A2236);
            g.fill(rx + 5, oy + 1, rx + rw - 5, oy + 19, hv ? 0xFF102030 : 0xFF14101C);
            g.drawString(font, Skin.fit(font, o.get("name").getAsString(), rw - 16), rx + 8, oy + 2, RANK_COL[Math.min(4, rank)], false);
            g.pose().pushPose(); g.pose().translate(rx + 8, oy + 12, 0); g.pose().scale(0.7f, 0.7f, 1);
            g.drawString(font, Skin.fit(font, o.get("desc").getAsString(), (int) ((rw - 16) / 0.7f)), 0, 0, Skin.DIM, false);
            g.pose().popPose();
            if (hv) tip = List.of(Component.literal(o.get("name").getAsString()), Component.literal(o.get("desc").getAsString()).withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("щелчок — оставить это").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
        if (!opts.isEmpty()) Skin.button(g, font, rx + 5, top + h - 30, rw - 10, s("cur").isEmpty() ? "Отказаться" : "Оставить старое", mx, my, true, false);
        if (tip != null) g.renderComponentTooltip(font, tip, mx, my);
    }

    @Override
    public boolean mouseClicked(double mxd, double myd, int b) {
        if (j() == null) return super.mouseClicked(mxd, myd, b);
        int mx = (int) mxd, my = (int) myd, ly = top + 14;
        JsonArray cand = cand();
        int gx = left + LX + 6, gy = ly + 15;
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) {
            int idx = (r + scroll) * COLS + c;
            if (idx < cand.size() && Skin.hover(mx, my, gx + c * CELL, gy + r * CELL, 18, 18)) { send("ench:put:" + cand.get(idx).getAsJsonObject().get("src").getAsString()); return true; }
        }
        int by = gy + ROWS * CELL + 8;
        for (int k = 0; k < 4; k++) if (res("bag", RES[k]) > 0 && Skin.hover(mx, my, gx + k * 40, by + 11, 18, 18)) { send("ench:load:" + RES[k]); return true; }
        int cx = left + MX, sx = cx + MW / 2 - 13, sy = ly + 16;
        boolean ok = j().has("ok") && j().get("ok").getAsBoolean();
        if (ok && Skin.hover(mx, my, sx, sy, 26, 26)) { send("ench:take"); return true; }
        int ry = sy + 30 + 26;
        for (int k = 0; k < 4; k++) if (res("table", RES[k]) > 0 && Skin.hover(mx, my, cx + 6 + (k % 2) * 52, ry + 11 + (k / 2) * 22, 18, 18)) { send("ench:unload:" + RES[k]); return true; }
        int rx = left + RX, rw = w - RX - 8, bw = (rw - 10) / 3;
        for (int k = 0; k < 3; k++)
            if (ok && Skin.hover(mx, my, rx + 5 + k * bw, ly + 15, bw - 3, 16) && i("tlaz") >= i("cost") * MODES[k]) { send("ench:roll:" + MODES[k]); return true; }
        var opts = j().has("opts") ? j().getAsJsonArray("opts") : new JsonArray();
        for (int k = 0; k < opts.size() && k < 8; k++)
            if (Skin.hover(mx, my, rx + 4, ly + 36 + k * 22, rw - 8, 20)) { send("ench:pick:" + k); return true; }
        if (!opts.isEmpty() && Skin.hover(mx, my, rx + 5, top + h - 30, rw - 10, 16)) { send("ench:pick:-1"); return true; }
        return super.mouseClicked(mxd, myd, b);
    }

    private static void send(String c) { PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd(c)); }
}
