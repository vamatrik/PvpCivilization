package com.pvpciv.client.screen;

import com.google.gson.JsonObject;
import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Skin;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.11 — молекулярный стол NaZa. Слева: предмет в руке, текущее зачарование, 8 гнёзд модулей, катализаторы, доступные чары.
 * Справа: режимы x1/x3/x10 и варианты синтеза (выбор 1 из N; «Оставить старое» — при Якоре).
 */
public class EnchantScreen extends PlainScreen {
    private static final int[] MODES = {1, 3, 10};
    private static final int CYAN = 0xFF40D0FF;
    private static final int[] TIER_COL = {0xFF2A2236, 0xFF40D0FF, 0xFF60FF90, 0xFFC060FF};
    private static final int[] RANK_COL = {0, 0xFFCFCFCF, 0xFF6AD0FF, 0xFFC070FF, 0xFFFFB030};

    public EnchantScreen() { super("Молекулярный стол", 440, 222, false); }

    private JsonObject j() { return ClientState.ench; }
    private int i(String k) { return j().has(k) ? j().get(k).getAsInt() : 0; }
    private String s(String k) { return j().has(k) ? j().get(k).getAsString() : ""; }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int lx = left + 10, ly = top + 14, lw = 160;
        Skin.inset(g, lx, ly, lw, h - 24);
        boolean ok = j().has("ok") && j().get("ok").getAsBoolean();
        g.drawString(font, ok ? Skin.fit(font, s("item"), lw - 10) : "Возьми в руку оружие или броню", lx + 5, ly + 5, ok ? Skin.GOLD_L : Skin.BAD, false);
        String cur = s("cur");
        Skin.para(g, font, cur.isEmpty() ? "Без зачарования" : "⚛ " + cur, lx + 5, ly + 16, lw - 10, 9, 2, cur.isEmpty() ? Skin.FAINT : CYAN, false);
        // гнёзда модулей
        g.drawString(font, "Модули (ПКМ у стола):", lx + 5, ly + 38, Skin.DIM, false);
        var mods = j().has("mods") ? j().getAsJsonArray("mods") : new com.google.gson.JsonArray();
        for (int k = 0; k < 8; k++) {
            int t = k < mods.size() ? mods.get(k).getAsInt() : 0;
            int sx = lx + 6 + (k % 4) * 37, sy = ly + 49 + (k / 4) * 16;
            g.fill(sx, sy, sx + 34, sy + 13, 0xFF0C0A12);
            g.fill(sx + 1, sy + 1, sx + 33, sy + 12, t > 0 ? (TIER_COL[t] & 0x60FFFFFF) : 0xFF16121E);
            g.drawCenteredString(font, t == 0 ? "—" : new String[]{"", "I", "II", "III"}[t], sx + 17, sy + 3, t > 0 ? TIER_COL[t] : Skin.FAINT);
        }
        // ресурсы
        int ry = ly + 86;
        g.drawString(font, "Лазуратрон: " + i("laz") + (ok ? "  (x1 = " + i("cost") + ")" : ""), lx + 5, ry, i("laz") >= i("cost") && ok ? Skin.TEXT : Skin.BAD, false);
        g.drawString(font, "Фокус " + i("focus") + " · Якорь " + i("anchor") + " · Резон. " + i("reso"), lx + 5, ry + 11, Skin.DIM, false);
        g.pose().pushPose(); g.pose().translate(lx + 5, ry + 23, 0); g.pose().scale(0.75f, 0.75f, 1);
        Skin.para(g, font, "Режим xN забирает до N катализаторов, все действуют: Фокус +1 вариант, Якорь — можно оставить старое, Резонатор — +1 ранг.", 0, 0, (int) ((lw - 10) / 0.75f), 10, 4, Skin.FAINT, false);
        g.pose().popPose();
        // доступные чары
        var pool = j().has("pool") ? j().getAsJsonArray("pool") : new com.google.gson.JsonArray();
        StringBuilder sb = new StringBuilder();
        for (var e : pool) { if (sb.length() > 0) sb.append(", "); sb.append(e.getAsString()); }
        g.drawString(font, "Доступно:", lx + 5, ry + 58, Skin.DIM, false);
        g.pose().pushPose(); g.pose().translate(lx + 5, ry + 69, 0); g.pose().scale(0.75f, 0.75f, 1);
        Skin.para(g, font, sb.length() == 0 ? "—" : sb.toString(), 0, 0, (int) ((lw - 10) / 0.75f), 10, 4, Skin.TEXT, false);
        g.pose().popPose();

        // справа: режимы и варианты
        int rx = left + 178, rw = h > 0 ? w - 188 : 0;
        Skin.inset(g, rx, ly, rw, h - 24);
        g.drawString(font, "Синтез", rx + 5, ly + 5, Skin.GOLD_L, false);
        for (int k = 0; k < 3; k++) {
            int m = MODES[k], bx = rx + 5 + k * ((rw - 10) / 3);
            boolean can = ok && i("laz") >= i("cost") * m;
            Skin.button(g, font, bx, ly + 16, (rw - 10) / 3 - 3, "x" + m + " · " + i("cost") * m, mx, my, can, k == 0);
        }
        g.pose().pushPose(); g.pose().translate(rx + 5, ly + 35, 0); g.pose().scale(0.7f, 0.7f, 1);
        g.drawString(font, Skin.fit(font, "x3: +1 вариант, ранг — лучший из 2 · x10: +2 варианта, лучший из 3", (int) ((rw - 10) / 0.7f)), 0, 0, Skin.FAINT, false);
        g.pose().popPose();
        var opts = j().has("opts") ? j().getAsJsonArray("opts") : new com.google.gson.JsonArray();
        if (opts.isEmpty()) {
            Skin.paraCentered(g, font, "Выбери режим — стол предложит варианты, оставишь один.", rx + rw / 2, ly + 80, rw - 20, 10, 3, Skin.FAINT);
        }
        for (int k = 0; k < opts.size() && k < 8; k++) {
            JsonObject o = opts.get(k).getAsJsonObject();
            int oy = ly + 46 + k * 19;
            boolean hv = Skin.hover(mx, my, rx + 4, oy, rw - 8, 17);
            int rank = o.get("rank").getAsInt();
            g.fill(rx + 4, oy, rx + rw - 4, oy + 17, hv ? 0xFF40D0FF : 0xFF2A2236);
            g.fill(rx + 5, oy + 1, rx + rw - 5, oy + 16, hv ? 0xFF102030 : 0xFF14101C);
            g.drawString(font, o.get("name").getAsString(), rx + 9, oy + 5, RANK_COL[Math.min(4, rank)], false);
            String nm = o.get("name").getAsString();
            String d = Skin.fit(font, o.get("desc").getAsString(), (int) ((rw - 24 - font.width(nm)) / 0.75f));
            g.pose().pushPose(); g.pose().translate(rx + rw - 8 - font.width(d) * 0.75f, oy + 6, 0); g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, d, 0, 0, Skin.DIM, false);
            g.pose().popPose();
        }
        if (!opts.isEmpty() && j().has("keep") && j().get("keep").getAsBoolean())
            Skin.button(g, font, rx + 5, top + h - 30, rw - 10, s("cur").isEmpty() ? "Отказаться от вариантов" : "Оставить старое (Якорь)", mx, my, true, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        int rx = left + 178, rw = w - 188, ly = top + 14;
        boolean ok = j().has("ok") && j().get("ok").getAsBoolean();
        for (int k = 0; k < 3; k++) {
            int bx = rx + 5 + k * ((rw - 10) / 3);
            if (ok && Skin.hover((int) mx, (int) my, bx, ly + 16, (rw - 10) / 3 - 3, 16) && i("laz") >= i("cost") * MODES[k]) {
                send("ench:roll:" + MODES[k]);
                return true;
            }
        }
        var opts = j().has("opts") ? j().getAsJsonArray("opts") : new com.google.gson.JsonArray();
        for (int k = 0; k < opts.size() && k < 8; k++)
            if (Skin.hover((int) mx, (int) my, rx + 4, ly + 46 + k * 19, rw - 8, 17)) { send("ench:pick:" + k); return true; }
        if (!opts.isEmpty() && j().has("keep") && j().get("keep").getAsBoolean() && Skin.hover((int) mx, (int) my, rx + 5, top + h - 30, rw - 10, 16)) { send("ench:pick:-1"); return true; }
        return super.mouseClicked(mx, my, b);
    }

    private static void send(String c) { PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd(c)); }
}
