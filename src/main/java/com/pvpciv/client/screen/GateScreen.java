package com.pvpciv.client.screen;

import com.google.gson.JsonObject;
import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Scroll;
import com.pvpciv.client.ui.Skin;
import com.pvpciv.game.Gates;
import com.pvpciv.net.Network;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Врата: вкладки «Мир 1 → Этаж 1/2», список врат с описанием, статусом и ценой починки; перенос. Место под картинку. */
public class GateScreen extends PlainScreen {
    private final JsonObject d;
    private final String from;
    private int floor, sel = -1;
    private final Scroll ls = new Scroll();

    public GateScreen(JsonObject d) {
        super(d.get("from").getAsString().equals("home") ? "Домашние врата" : "Врата телепортации", 360, 220, false);
        this.d = d;
        this.from = d.get("from").getAsString();
        Gates.Gate g = Gates.byId(from);
        floor = g == null ? 1 : g.floor();
    }

    private int st(String id) { return d.getAsJsonObject("st").has(id) ? d.getAsJsonObject("st").get(id).getAsInt() : 0; }
    private boolean home() { return from.equals("home"); }
    private int homeLvl() { return d.get("home").getAsInt(); }
    private List<Gates.Gate> list() { return Gates.ALL.stream().filter(g -> g.floor() == floor).toList(); }
    private boolean floorOpen(int f) { return home() ? f <= homeLvl() && (f == 1 || d.get("lift").getAsBoolean()) : Gates.byId(from) != null && Gates.byId(from).floor() == f; }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        // вкладки: мир → этажи
        g.drawString(font, "Мир 1", left + 14, top + 20, Skin.GOLD_L, false);
        for (int f = 1; f <= 2; f++) {
            int tx = left + 52 + (f - 1) * 64;
            boolean on = floor == f, ok = floorOpen(f);
            Skin.tab(g, font, tx, top + 17, 60, "Этаж " + f, on, mx, my);
            if (!ok) Skin.icon(g, "lock", tx + 48, top + 20);
        }
        // список
        List<Gates.Gate> l = list();
        int lx = left + 12, ly = top + 36, lw = 150, lh = 172;
        Skin.inset(g, lx, ly, lw, lh);
        ls.view(lh - 4).content(l.size() * 24 + 4);
        Scroll.begin(g, lx, ly + 2, lw, lh - 4);
        for (int i = 0; i < l.size(); i++) {
            Gates.Gate gt = l.get(i);
            int y = ly + 3 + i * 24 - ls.off, s = st(gt.id());
            boolean hv = in(mx, my, 12, 39 + i * 24 - ls.off, lw - 6, 22) && my >= ly && my < ly + lh;
            if (sel == i) g.fill(lx + 2, y, lx + lw - 5, y + 22, 0x40D9A441);
            else if (hv) g.fill(lx + 2, y, lx + lw - 5, y + 22, 0x18FFFFFF);
            Skin.icon(g, s == 2 ? "spark" : s == 1 ? "arrow" : "lock", lx + 5, y + 6);
            g.drawString(font, Skin.fit(font, s == 0 ? "???" : gt.name(), lw - 30), lx + 20, y + 3, s == 2 ? Skin.TEXT : s == 1 ? 0xFFB0B8C8 : Skin.FAINT, false);
            g.drawString(font, s == 2 ? "работают" : s == 1 ? "сломаны" : "не найдены", lx + 20, y + 12, s == 2 ? Skin.GOOD : s == 1 ? 0xFFE0A050 : Skin.FAINT, false);
        }
        Scroll.end(g);
        ls.bar(g, lx + lw - 4, ly + 2, lh - 4);
        // карточка
        int cx = left + 170, cw = 178;
        Skin.inset(g, cx, ly, cw, lh);
        if (sel < 0 || sel >= l.size()) {
            if (home()) {
                g.drawString(font, "Домашние врата • ур. " + homeLvl(), cx + 6, ly + 6, Skin.GOLD_L, false);
                Skin.para(g, font, "Ведут на любые починенные врата этажей до " + homeLvl() + "-го. С врат по миру — только по своему этажу и домой. Перенос бесплатный.", cx + 6, ly + 20, cw - 12, 10, 5, Skin.TEXT, false);
                int next = homeLvl() + 1;
                if (next < Gates.HOME_UP.length) {
                    g.drawString(font, "Улучшить до этажа " + next + ":", cx + 6, ly + 80, Skin.DIM, false);
                    Skin.para(g, font, Gates.costText(Gates.HOME_UP[next], Gates.HOME_UP_MONEY[next]), cx + 6, ly + 92, cw - 12, 10, 4, Skin.TEXT, false);
                    Skin.button(g, font, cx + 6, ly + lh - 22, cw - 12, "Улучшить", mx, my, true, true);
                } else g.drawString(font, "Новые этажи — в следующих обновлениях.", cx + 6, ly + 80, Skin.FAINT, false);
            } else Skin.para(g, font, "Выбери врата слева. Починенные — переносят бесплатно, сломанные чинятся ресурсами своей локации прямо у врат.", cx + 6, ly + 8, cw - 12, 10, 6, Skin.DIM, false);
        } else {
            Gates.Gate gt = l.get(sel);
            int s = st(gt.id());
            // место под картинку
            g.fill(cx + 6, ly + 6, cx + cw - 6, ly + 62, 0xFF1A1622);
            g.drawCenteredString(font, s == 0 ? "?" : "картинка скоро", cx + cw / 2, ly + 30, Skin.FAINT);
            g.drawString(font, Skin.fit(font, s == 0 ? "Неизвестные врата" : gt.name(), cw - 12), cx + 6, ly + 68, Skin.GOLD_L, false);
            int y = ly + 80;
            y += Skin.para(g, font, s == 0 ? "Найди их в мире — подойди ближе." : gt.desc(), cx + 6, y, cw - 12, 10, 4, Skin.TEXT, false) + 4;
            if (s == 1) y += Skin.para(g, font, "Починка у самих врат: " + Gates.costText(gt.costs(), gt.money()), cx + 6, y, cw - 12, 10, 4, 0xFFE0A050, false);
            boolean can = s == 2 && floorOpen(gt.floor()) && !gt.id().equals(from);
            int cd = d.get("cd").getAsInt();
            String b = gt.id().equals(from) ? "Ты здесь" : s != 2 ? "Не работают" : !floorOpen(gt.floor()) ? "Не достают" : cd > 0 ? "Остывают " + cd + " с" : "Перенестись";
            Skin.button(g, font, cx + 6, ly + lh - 22, cw - 12, b, mx, my, can && cd <= 0, true);
        }
        if (!home()) Skin.button(g, font, left + 186, top + 17, 162, "Домой (в квартиру)", mx, my, d.get("cd").getAsInt() <= 0, false);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) { return ls.wheel(sy) || super.mouseScrolled(mx, my, sx, sy); }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int f = 1; f <= 2; f++) if (in(mx, my, 52 + (f - 1) * 64, 17, 60, 14)) { floor = f; sel = -1; ls.off = 0; return true; }
        if (!home() && in(mx, my, 186, 17, 162, 16)) { cmd("gate:" + from + ":home"); onClose(); return true; }
        List<Gates.Gate> l = list();
        if (my >= top + 36 && my < top + 208)
            for (int i = 0; i < l.size(); i++) if (in(mx, my, 12, 39 + i * 24 - ls.off, 144, 22)) { sel = i; return true; }
        if (in(mx, my, 170 + 6, 36 + 172 - 22, 166, 16)) {
            if (sel < 0 && home()) { cmd("gate_up"); return true; }
            if (sel >= 0 && sel < l.size()) { cmd("gate:" + from + ":" + l.get(sel).id()); onClose(); return true; }
        }
        return super.mouseClicked(mx, my, b);
    }

    private static void cmd(String c) { PacketDistributor.sendToServer(new Network.PhoneCmd(c)); }

    @SuppressWarnings("unused") private static final int L = ClientState.level;
}
