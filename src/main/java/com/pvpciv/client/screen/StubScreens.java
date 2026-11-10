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

    public static class Titles extends PlainScreen {
        private static final String[][] T = {
                {"Чёрная звезда", "заточи оружие до +11", "чёрная аура"},
                {"Ломатель", "сломай 10 оружий", "+20% материалов с разбора"},
                {"Ледяной гонщик", "рекорд на сложном пути", "+8% скорости лодки"},
                {"Охотник за головами", "50 убийств в PvP", "+5% Искр с игроков"},
                {"Король барахолки", "продай на 100 000 ✦", "+5% к цене продажи"},
                {"Слито Луне ×100", "проиграй Луне 100 раз", "до +10% урона"},
        };
        public Titles() { super("Титулы", 300, 196, true); }
        @Override protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(font, "Активен один титул: он виден над ником", left + 14, top + 20, Skin.DIM, false);
            for (int i = 0; i < T.length; i++) {
                int y = top + 34 + i * 22;
                Skin.inset(g, left + 12, y, 276, 20);
                Skin.icon(g, "lock", left + 17, y + 5);
                g.drawString(font, T[i][0], left + 32, y + 2, Skin.GOLD_L, false);
                g.drawString(font, T[i][1], left + 32, y + 11, Skin.FAINT, false);
                g.drawString(font, T[i][2], left + 284 - font.width(T[i][2]), y + 6, Skin.DIM, false);
            }
            soon(g, font, left + w / 2, top + 170);
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
