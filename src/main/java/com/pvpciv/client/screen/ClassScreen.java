package com.pvpciv.client.screen;

import com.google.gson.JsonObject;
import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Skin;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.0 — дерево профессий. Без класса: четыре карточки классов (выбор — 1 тёмная материя).
 * С классом: три узла класса и две эволюции (взаимоисключающие, после двух узлов).
 */
public class ClassScreen extends PlainScreen {
    private static final int W = 400, H = 230;

    public ClassScreen() { super("Дерево классов", W, H, true); }

    private JsonObject j() { return ClientState.classes; }

    private List<JsonObject> list(String key) {
        List<JsonObject> l = new ArrayList<>();
        if (j() != null) for (var el : j().getAsJsonArray(key)) l.add(el.getAsJsonObject());
        return l;
    }

    private JsonObject myClass() {
        if (j() == null) return null;
        String c = j().get("cls").getAsString();
        for (JsonObject o : list("classes")) if (o.get("id").getAsString().equals(c)) return o;
        return null;
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        if (j() == null) return;
        int dm = j().get("dm").getAsInt();
        String bal = "Тёмная материя: " + dm;
        g.drawString(font, bal, left + W - 12 - font.width(bal), top + 14, 0xFFC090FF, false);
        JsonObject mine = myClass();
        if (mine == null) {
            g.drawString(font, "Выбери класс (1 тёмная материя)", left + 12, top + 14, Skin.DIM, false);
            List<JsonObject> cl = list("classes");
            for (int i = 0; i < cl.size(); i++) {
                JsonObject o = cl.get(i);
                int x = left + 10 + i * 96, y = top + 30, cw = 92, ch = 150;
                boolean hv = Skin.hover(mx, my, x, y, cw, ch) && dm >= 1;
                int col = 0xFF000000 | o.get("color").getAsInt();
                g.fill(x, y, x + cw, y + ch, hv ? col : 0xFF3A3048);
                g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1, 0xFF14101C);
                g.drawCenteredString(font, o.get("name").getAsString(), x + cw / 2, y + 8, col);
                g.drawCenteredString(font, o.get("weapons").getAsString(), x + cw / 2, y + 22, Skin.GOLD_L);
                Skin.para(g, font, o.get("desc").getAsString(), x + 5, y + 40, cw - 10, 10, 8, Skin.DIM, false);
                if (hv) g.drawCenteredString(font, "▶ выбрать", x + cw / 2, y + ch - 14, Skin.GOOD);
            }
            Skin.para(g, font, "Класс усиливает своё оружие особыми механиками. Чужое оружие при выбранном классе: −35% урона и −15% скорости. "
                    + "Сменить класс — свиток забвения у Луны (тёмная материя вернётся).", left + 12, top + 188, W - 24, 10, 4, Skin.FAINT, false);
            return;
        }
        int col = 0xFF000000 | mine.get("color").getAsInt();
        g.drawString(font, "Класс: " + mine.get("name").getAsString() + "  ·  " + mine.get("weapons").getAsString(), left + 12, top + 14, col, false);
        List<JsonObject> base = new ArrayList<>(), evo = new ArrayList<>();
        for (JsonObject o : list("nodes")) (o.get("evo").getAsBoolean() ? evo : base).add(o);
        int baseHave = 0;
        boolean evoTaken = false;
        for (JsonObject o : base) if (o.get("have").getAsBoolean()) baseHave++;
        for (JsonObject o : evo) if (o.get("have").getAsBoolean()) evoTaken = true;
        g.drawString(font, "Узлы класса", left + 12, top + 28, Skin.GOLD_L, false);
        for (int i = 0; i < base.size(); i++) node(g, base.get(i), left + 10 + i * 128, top + 40, 124, 72, mx, my, dm, true);
        g.drawString(font, "Эволюция (одна на выбор, после 2 узлов)", left + 12, top + 120, Skin.GOLD_L, false);
        for (int i = 0; i < evo.size(); i++) node(g, evo.get(i), left + 10 + i * 192, top + 132, 188, 82, mx, my, dm, baseHave >= 2 && !evoTaken);
        // линии-связи
        for (int i = 0; i < base.size(); i++) g.fill(left + 72 + i * 128, top + 112, left + 73 + i * 128, top + 118, base.get(i).get("have").getAsBoolean() ? col : 0xFF3A3048);
    }

    private void node(GuiGraphics g, JsonObject o, int x, int y, int w, int h, int mx, int my, int dm, boolean avail) {
        boolean have = o.get("have").getAsBoolean();
        int cost = o.get("cost").getAsInt();
        boolean can = !have && avail && dm >= cost;
        boolean hv = can && Skin.hover(mx, my, x, y, w, h);
        int col = 0xFF000000 | myClass().get("color").getAsInt();
        g.fill(x, y, x + w, y + h, have ? col : hv ? 0xFF8A70B0 : 0xFF3A3048);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, have ? 0xFF221A30 : 0xFF14101C);
        g.drawString(font, Skin.fit(font, o.get("name").getAsString(), w - 30), x + 5, y + 4, have ? col : avail ? Skin.TEXT : Skin.FAINT, false);
        String c = have ? "✔" : cost + " ◆";
        g.drawString(font, c, x + w - 5 - font.width(c), y + 4, have ? Skin.GOOD : dm >= cost ? 0xFFC090FF : Skin.BAD, false);
        g.pose().pushPose();
        g.pose().translate(x + 5, y + 16, 0);
        g.pose().scale(0.8f, 0.8f, 1);
        Skin.para(g, font, o.get("desc").getAsString(), 0, 0, (int) ((w - 10) / 0.8f), 10, 7, have || avail ? Skin.DIM : Skin.FAINT, false);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (j() != null) {
            int dm = j().get("dm").getAsInt();
            if (myClass() == null) {
                List<JsonObject> cl = list("classes");
                for (int i = 0; i < cl.size(); i++) if (dm >= 1 && in(mx, my, 10 + i * 96, 30, 92, 150)) {
                    PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("cls:pick:" + cl.get(i).get("id").getAsString()));
                    return true;
                }
            } else {
                List<JsonObject> base = new ArrayList<>(), evo = new ArrayList<>();
                for (JsonObject o : list("nodes")) (o.get("evo").getAsBoolean() ? evo : base).add(o);
                for (int i = 0; i < base.size(); i++) if (in(mx, my, 10 + i * 128, 40, 124, 72) && !base.get(i).get("have").getAsBoolean()) { send(base.get(i)); return true; }
                for (int i = 0; i < evo.size(); i++) if (in(mx, my, 10 + i * 192, 132, 188, 82) && !evo.get(i).get("have").getAsBoolean()) { send(evo.get(i)); return true; }
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    private void send(JsonObject o) { PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("cls:node:" + o.get("id").getAsString())); }
}
