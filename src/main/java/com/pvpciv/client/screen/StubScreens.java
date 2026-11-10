package com.pvpciv.client.screen;

import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Skin;
import net.minecraft.client.gui.GuiGraphics;

/** Заготовки будущих экранов в общем стиле: титулы, трофеи, верстак брони. Логика появится позже. */
public final class StubScreens {
    private static void soon(GuiGraphics g, net.minecraft.client.gui.Font f, int cx, int y) {
        Skin.divider(g, cx, y);
        g.drawCenteredString(f, "Появится в следующих версиях", cx, y + 10, Skin.FAINT);
    }

    /** Титулы: список из progress.Titles.DEFS. Открытый — щелчок делает активным (виден над ником, даёт бонус). */
    public static class Titles extends PlainScreen {
        private final java.util.List<com.pvpciv.progress.Titles.Def> list = new java.util.ArrayList<>(com.pvpciv.progress.Titles.DEFS.values());
        public Titles() { super("Титулы", 300, 196, true); }
        @Override protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(font, "Активен один титул: он виден над ником и даёт бонус", left + 14, top + 20, Skin.DIM, false);
            for (int i = 0; i < Math.min(6, list.size()); i++) {
                var d = list.get(i);
                int y = top + 34 + i * 22;
                boolean open = ClientState.titles.contains(d.id()), act = d.id().equals(ClientState.activeTitle);
                boolean hover = open && in(mx, my, 12, 34 + i * 22, 276, 20);
                Skin.inset(g, left + 12, y, 276, 20);
                if (act) g.fill(left + 13, y + 1, left + 287, y + 19, 0x30FFD166);
                else if (hover) g.fill(left + 13, y + 1, left + 287, y + 19, 0x18FFFFFF);
                Skin.icon(g, open ? (act ? "spark" : "arrow") : "lock", left + 17, y + 5);
                g.drawString(font, d.name(), left + 32, y + 2, open ? (0xFF000000 | d.color()) : Skin.GOLD_D, false);
                g.drawString(font, Skin.fit(font, d.how(), 150), left + 32, y + 11, Skin.FAINT, false);
                String b = act ? "активен • " + d.bonusText() : d.bonusText();
                g.drawString(font, Skin.fit(font, b, 110), left + 284 - Math.min(110, font.width(b)), y + 6, act ? Skin.GOOD : Skin.DIM, false);
            }
            Skin.divider(g, left + w / 2, top + 170);
            g.drawCenteredString(font, "Щёлкни по открытому титулу, чтобы надеть или снять", left + w / 2, top + 180, Skin.FAINT);
        }
        @Override public boolean mouseClicked(double mx, double my, int b) {
            for (int i = 0; i < Math.min(6, list.size()); i++) {
                var d = list.get(i);
                if (ClientState.titles.contains(d.id()) && in(mx, my, 12, 34 + i * 22, 276, 20)) {
                    String id = d.id().equals(ClientState.activeTitle) ? "" : d.id();
                    ClientState.activeTitle = id;
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.SelectTitle(id));
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
                    return true;
                }
            }
            return super.mouseClicked(mx, my, b);
        }
    }

    public static class Trophies extends PlainScreen {
        private static final String[] B = {"Гнилой Громила", "Ледяной Кит", "Ржавый Голем", "Капитан Сосулька", "Кузнечный Дух", "Садовница теней", "Комендант", "Хранитель свитков", "Чемпион Колизея", "Каэль"};
        public Trophies() { super("Витрина трофеев", 300, 180, true); }
        @Override protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(font, "Трофеи боссов мира 1", left + 14, top + 20, Skin.DIM, false);
            for (int i = 0; i < 10; i++) {
                int x = left + 16 + (i % 5) * 54, y = top + 36 + (i / 5) * 56;
                Skin.inset(g, x, y, 50, 50);
                Skin.spr(g, "podium", x + 5, y + 30, 40, 10, 0x60FFB060);
                Skin.spr(g, "icon_skull", x + 17, y + 12, 16, 16, 0x50FFFFFF);
                g.drawCenteredString(font, Skin.fit(font, B[i], 50), x + 25, y + 39, Skin.FAINT);
            }
            soon(g, font, left + w / 2, top + 152);
        }
    }

    public static class ArmorBench extends PlainScreen {
        public ArmorBench() { super("Верстак брони", 260, 170, true); }
        @Override protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
            Skin.inset(g, left + 12, top + 22, 236, 96);
            Skin.slot(g, left + 30, top + 40); Skin.hint(g, "chest", left + 30, top + 40);
            Skin.slot(g, left + 30, top + 80); Skin.hint(g, "artifact", left + 30, top + 80);
            g.drawString(font, "броня", left + 24, top + 60, Skin.FAINT, false);
            g.drawString(font, "материал", left + 16, top + 100, Skin.FAINT, false);
            g.drawString(font, "Уровень брони 0 / 20", left + 62, top + 34, Skin.GOLD_L, false);
            Skin.bar(g, left + 66, top + 46, 172, 0.35f, 0xFF8FE3FF, 0xFF2A7FB8, "blue");
            g.drawString(font, "Материалы дают опыт броне:", left + 62, top + 64, Skin.DIM, false);
            g.drawString(font, "тир 1 — 10, тир 2 — 40, тир 3 — 160", left + 62, top + 75, Skin.DIM, false);
            g.drawString(font, "Свитки закалки снимают потолок", left + 62, top + 90, Skin.DIM, false);
            g.drawString(font, "на уровнях 5, 10 и 15", left + 62, top + 101, Skin.DIM, false);
            Skin.button(g, font, left + 12, top + 124, 236, "Улучшить броню", mx, my, false, true);
            soon(g, font, left + w / 2, top + 144);
        }
    }

    @SuppressWarnings("unused") private static final int L = ClientState.level;
}
