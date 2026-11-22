package com.pvpciv.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pvpciv.client.ui.Scroll;
import com.pvpciv.client.ui.Skin;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** 1.1: «Заметки» — карточки открытых систем, прокрутка колесом. */
public class NotesScreen extends PlainScreen {
    private final JsonArray cards;
    private static final int PAD = 10, TOP = 22;

    public NotesScreen(JsonObject j) { super("Заметки", 300, 220, false); cards = j.getAsJsonArray("cards"); }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = left + PAD, y0 = top + TOP, vw = w - PAD * 2 - 6, vh = h - TOP - 24;
        int tw = vw - 10;
        int total = 0;
        for (var e : cards) total += 16 + Skin.lines(font, e.getAsJsonObject().get("x").getAsString(), tw) * 10 + 6;
        scroll.view(vh).content(total);
        Scroll.begin(g, x, y0, vw, vh);
        int y = y0 - scroll.off;
        for (var e : cards) {
            JsonObject o = e.getAsJsonObject();
            int c = o.get("c").getAsInt();
            List<String> ls = Skin.wrap(font, o.get("x").getAsString(), tw);
            int ch = 16 + ls.size() * 10;
            if (y + ch > y0 && y < y0 + vh) {
                g.fill(x, y, x + vw, y + ch, 0x30FFFFFF);
                g.fill(x, y, x + 2, y + ch, c);
                g.drawString(font, o.get("t").getAsString(), x + 6, y + 4, c, false);
                for (int i = 0; i < ls.size(); i++) g.drawString(font, ls.get(i), x + 6, y + 15 + i * 10, Skin.TEXT, false);
            }
            y += ch + 6;
        }
        Scroll.end(g);
        scroll.bar(g, x + vw + 2, y0, vh);
        g.drawCenteredString(font, "новое открыл — новая карточка · колесо ↕", left + w / 2, top + h - 17, Skin.FAINT);
    }
}
