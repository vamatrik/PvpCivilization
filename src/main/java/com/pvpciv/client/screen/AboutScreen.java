package com.pvpciv.client.screen;

import com.google.gson.JsonObject;
import com.pvpciv.client.ui.Skin;
import net.minecraft.client.gui.GuiGraphics;

/** 1.2: «О себе» — статистика героя (две колонки, разделы золотом). */
public class AboutScreen extends PlainScreen {
    private final JsonObject j;

    public AboutScreen(JsonObject j) { super("О себе · " + j.get("name").getAsString(), 340, 214, true); this.j = j; }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        var rows = j.getAsJsonArray("rows");
        int col = 0, y = top + 20, x0 = left + 12, cw = 154;
        for (int i = 0; i < rows.size(); i++) {
            var o = rows.get(i).getAsJsonObject();
            if (!o.has("v") && o.get("k").getAsString().equals("Мир")) { col = 1; y = top + 20; }
            int x = x0 + col * (cw + 8);
            if (!o.has("v")) {
                if (y > top + 20) y += 4;
                g.drawString(font, o.get("k").getAsString(), x, y, Skin.GOLD_L, false);
                g.fill(x, y + 10, x + cw, y + 11, 0x40D9A441);
                y += 14;
                continue;
            }
            String v = o.get("v").getAsString();
            g.drawString(font, o.get("k").getAsString(), x + 4, y, Skin.DIM, false);
            g.drawString(font, v, x + cw - font.width(v), y, Skin.TEXT, false);
            y += 11;
        }
    }
}
