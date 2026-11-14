package com.pvpciv.client.screen;

import com.google.gson.JsonObject;
import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Skin;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** 0.10.5: навыки — три слота (у каждого свой тип), в слоте выбирается один навык из его списка. */
public class SkillsScreen extends PlainScreen {
    private static final String[] TYPE = {"Перемещение", "Боевые", "Особые"};
    private static final int[] LVL = {1, 10, 35};
    private static final int COL = 120, CARD = 46;

    public SkillsScreen() { super("Навыки", 380, 216, true); }

    private List<JsonObject> slot(int s) {
        List<JsonObject> l = new ArrayList<>();
        for (var el : ClientState.skillsAll) { JsonObject o = el.getAsJsonObject(); if (o.get("slot").getAsInt() == s) l.add(o); }
        return l;
    }

    private static String key(int i) { return com.pvpciv.client.ClientInit.SKILL_KEYS[i].getTranslatedKeyMessage().getString().toUpperCase(); }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        g.drawCenteredString(font, "Щёлкни по навыку, чтобы поставить его в слот", left + w / 2, top + 18, Skin.DIM);
        for (int i = 0; i < 3; i++) {
            int x = left + 10 + i * (COL + 3), y = top + 32;
            boolean open = ClientState.level >= LVL[i];
            Skin.inset(g, x, y, COL, h - 44);
            g.drawString(font, "Слот " + (i + 1) + " · " + key(i), x + 5, y + 4, open ? Skin.GOLD_L : Skin.FAINT, false);
            g.drawString(font, TYPE[i] + (open ? "" : " · с ур. " + LVL[i]), x + 5, y + 14, Skin.DIM, false);
            List<JsonObject> l = slot(i + 1);
            if (l.isEmpty()) { Skin.para(g, font, "Навыки этого слота появятся на следующих этажах.", x + 5, y + 30, COL - 10, 10, 4, Skin.FAINT, false); continue; }
            for (int k = 0; k < l.size(); k++) {
                JsonObject o = l.get(k);
                int cy = y + 28 + k * (CARD + 3);
                boolean have = o.get("open").getAsBoolean() && open, sel = o.get("id").getAsString().equals(ClientState.skillSel[i]);
                boolean hv = have && Skin.hover(mx, my, x + 3, cy, COL - 6, CARD);
                g.fill(x + 3, cy, x + COL - 3, cy + CARD, sel ? 0xFFD9A441 : hv ? 0xFF6A5A80 : 0xFF2A2236);
                g.fill(x + 4, cy + 1, x + COL - 4, cy + CARD - 1, sel ? 0xFF2A2014 : 0xFF14101C);
                int col = i == 0 ? Skin.ICE : i == 1 ? 0xFFE0405A : Skin.GOLD_L;
                g.drawCenteredString(font, o.get("icon").getAsString(), x + 13, cy + 5, have ? col : Skin.FAINT);
                String cdt = o.get("cd").getAsInt() + " с";
                g.drawString(font, Skin.fit(font, o.get("name").getAsString(), COL - 36 - font.width(cdt)), x + 24, cy + 4, have ? (sel ? Skin.GOLD_L : Skin.TEXT) : Skin.FAINT, false);
                g.drawString(font, cdt, x + COL - 8 - font.width(cdt), cy + 4, Skin.DIM, false);
                String body = have ? o.get("desc").getAsString() : "🔒 " + o.get("how").getAsString();
                g.pose().pushPose();
                g.pose().translate(x + 7, cy + 15, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                Skin.para(g, font, body, 0, 0, (int) ((COL - 14) / 0.75f), 10, 3, have ? Skin.DIM : Skin.FAINT, false);
                g.pose().popPose();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < 3; i++) {
            if (ClientState.level < LVL[i]) continue;
            List<JsonObject> l = slot(i + 1);
            for (int k = 0; k < l.size(); k++) {
                JsonObject o = l.get(k);
                if (!o.get("open").getAsBoolean()) continue;
                if (in(mx, my, 10 + i * (COL + 3) + 3, 32 + 28 + k * (CARD + 3), COL - 6, CARD)) {
                    ClientState.skillSel[i] = o.get("id").getAsString();
                    PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("setskill:" + (i + 1) + ":" + o.get("id").getAsString()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, b);
    }
}
