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

    /** Боссы: таймеры всех боссов; закреплённые показываются на HUD. */
    public static class Bosses extends PlainScreen {
        private record B(String id, String name, String where, String lvl, boolean ready) {}
        private static final B[] LIST = {
                new B("brute", "Гнилой Громила", "Пепельная пустошь • Логово (PvP)", "ур. 5", true),
                new B("keeper", "Хранительница Склепа", "Инеевый Кряж • Ледяной Склеп (PvP)", "ур. 8", true),
                new B("don", "Дон Витторио", "Этаж 2 • Клуб «Серра» (PvP)", "ур. 10", true),
                new B("rat_king", "Крысиный король", "Этаж 2 • Тронный сток (PvP)", "ур. 13", true)};
        public Bosses() { super("Боссы", 300, 192, true); }
        @Override protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(font, "Закреплённые боссы видны на экране", left + 14, top + 20, Skin.DIM, false);
            for (int i = 0; i < LIST.length; i++) {
                B b = LIST[i];
                int y = top + 34 + i * 30;
                Skin.inset(g, left + 12, y, 276, 28);
                Skin.icon(g, "skull", left + 17, y + 6);
                g.drawString(font, b.name() + "  " + b.lvl(), left + 33, y + 4, b.ready() ? 0xFFFF7060 : Skin.GOLD_D, false);
                g.drawString(font, b.where(), left + 33, y + 15, Skin.FAINT, false);
                String t;
                if (!b.ready()) t = "скоро";
                else {
                    int s = ClientState.bossSeconds(b.id());
                    t = s < 0 ? "ПРОСНУЛСЯ!" : s == 0 ? "ждёт гостей" : String.format("%d:%02d", s / 60, s % 60);
                }
                g.drawString(font, t, left + 230 - font.width(t), y + 4, b.ready() && ClientState.bossSeconds(b.id()) < 0 ? 0xFFFF6060 : Skin.TEXT, false);
                if (b.ready()) {
                    boolean pin = ClientState.pinned(b.id());
                    Skin.button(g, font, left + 236, y + 6, 48, pin ? "Убрать" : "На экран", mx, my, true, pin);
                }
            }
        }
        @Override public boolean mouseClicked(double mx, double my, int btn) {
            for (int i = 0; i < LIST.length; i++)
                if (LIST[i].ready() && in(mx, my, 236, 40 + i * 30, 48, 16)) { ClientState.togglePin(LIST[i].id()); return true; }
            return super.mouseClicked(mx, my, btn);
        }
    }

    public static class Trophies extends PlainScreen {
        private static final String[] B = {"Гнилой Громила", "Хранительница Склепа", "Дон Витторио", "Крысиный король", "Кузнечный Дух", "Садовница теней", "Комендант", "Хранитель свитков", "Чемпион Колизея", "Каэль"};
        public Trophies() { super("Витрина трофеев", 300, 180, true); }
        @Override protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(font, "Трофеи боссов мира 1", left + 14, top + 20, Skin.DIM, false);
            for (int i = 0; i < 10; i++) {
                int x = left + 16 + (i % 5) * 54, y = top + 36 + (i / 5) * 56;
                Skin.inset(g, x, y, 50, 50);
                Skin.spr(g, "podium", x + 5, y + 30, 40, 10, 0x60FFB060);
                boolean got = (ClientState.trophies & (1 << i)) != 0;
                net.minecraft.world.item.Item it = com.pvpciv.block.ShowcaseBlockEntity.trophy(i);
                if (got && it != null) g.renderItem(new net.minecraft.world.item.ItemStack(it), x + 17, y + 12);
                else Skin.spr(g, "icon_skull", x + 17, y + 12, 16, 16, 0x50FFFFFF);
                g.drawCenteredString(font, Skin.fit(font, B[i], 50), x + 25, y + 39, got ? Skin.GOLD_L : Skin.FAINT);
            }
            g.drawCenteredString(font, "Трофей даётся за первую победу над боссом", left + w / 2, top + 152, Skin.FAINT);
        }
    }

    /** Гараж лодок: все открытые лодки, при себе — выбранная. */
    /** Настройки героя: всё, что не влезает в отдельные вкладки. Пока — PvP. */
    public static class Settings extends PlainScreen {
        private static final String[] TITLE = {"Защита от сильных"};
        private static final String[] DESC = {"Не драться с игроками на " + com.pvpciv.combat.CombatEvents.PVP_GAP + "+ уровней выше: ни они тебя, ни ты их."};
        public Settings() {
            super("Настройки", 340, 200, true);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("opt:get"));
        }
        private boolean val(int i) { return ClientState.pvpShield; }
        @Override protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(font, "PvP", left + 14, top + 20, Skin.GOLD_L, false);
            for (int i = 0; i < TITLE.length; i++) {
                int y = top + 32 + i * 42;
                boolean hv = in(mx, my, 12, 32 + i * 42, 316, 40);
                Skin.inset(g, left + 12, y, 316, 40);
                if (hv) g.fill(left + 13, y + 1, left + 327, y + 39, 0x18FFFFFF);
                g.drawString(font, TITLE[i], left + 18, y + 5, Skin.TEXT, false);
                Skin.para(g, font, DESC[i], left + 18, y + 17, 260, 10, 2, Skin.FAINT, false);
                boolean on = val(i);
                int sx = left + 292, sy = y + 14;
                g.fill(sx, sy, sx + 28, sy + 12, on ? 0xFF2E9E5A : 0xFF3A3A48);
                g.fill(on ? sx + 16 : sx + 2, sy + 2, on ? sx + 26 : sx + 12, sy + 10, 0xFFE8ECF4);
            }
            Skin.para(g, font, "Обычные PvP-зоны остаются PvP-зонами — защита действует только по уровню.", left + 14, top + 80, 312, 10, 2, Skin.DIM, false);
        }
        @Override public boolean mouseClicked(double mx, double my, int b) {
            if (in(mx, my, 12, 32, 316, 40)) {
                ClientState.pvpShield = !ClientState.pvpShield;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("opt:pvpShield:" + (ClientState.pvpShield ? 1 : 0)));
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
                return true;
            }
            return super.mouseClicked(mx, my, b);
        }
    }

    public static class Boats extends PlainScreen {
        private final java.util.List<com.pvpciv.item.BoatSummonItem> all = new java.util.ArrayList<>();
        public Boats() {
            super("Гараж лодок", 280, 170, true);
            for (var it : net.minecraft.core.registries.BuiltInRegistries.ITEM) if (it instanceof com.pvpciv.item.BoatSummonItem b) all.add(b);
            all.sort(java.util.Comparator.comparingInt(b -> b.tier));
        }
        private static String id(net.minecraft.world.item.Item it) { return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it).toString(); }
        @Override protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(font, "Новая лучшая лодка выбирается сама", left + 14, top + 20, Skin.DIM, false);
            for (int i = 0; i < all.size(); i++) {
                var b = all.get(i);
                int y = top + 34 + i * 26;
                String id = id(b);
                boolean open = ClientState.boats.contains(id), act = id.equals(ClientState.activeBoat);
                Skin.inset(g, left + 12, y, 256, 24);
                if (act) g.fill(left + 13, y + 1, left + 267, y + 23, 0x308FE3FF);
                else if (open && in(mx, my, 12, 34 + i * 26, 256, 24)) g.fill(left + 13, y + 1, left + 267, y + 23, 0x18FFFFFF);
                if (open) g.renderItem(new net.minecraft.world.item.ItemStack(b), left + 17, y + 4); else Skin.icon(g, "lock", left + 20, y + 7);
                g.drawString(font, open ? b.title + " лодка" : "???", left + 38, y + 3, open ? Skin.TEXT : Skin.GOLD_D, false);
                String sp = b.tier > 1 ? "Тир " + b.tier + " • +" + Math.round(com.pvpciv.item.BoatSummonItem.speedBonus(b.tier) * 100) + "% на льду" : "Тир 1 • обычная";
                g.drawString(font, sp, left + 38, y + 13, Skin.FAINT, false);
                String st = act ? "выбрана" : open ? "выбрать" : "не найдена";
                g.drawString(font, st, left + 262 - font.width(st), y + 8, act ? Skin.GOOD : open ? Skin.GOLD_L : Skin.FAINT, false);
            }
        }
        @Override public boolean mouseClicked(double mx, double my, int b) {
            for (int i = 0; i < all.size(); i++) {
                String id = id(all.get(i));
                if (ClientState.boats.contains(id) && !id.equals(ClientState.activeBoat) && in(mx, my, 12, 34 + i * 26, 256, 24)) {
                    ClientState.activeBoat = id;
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.SelectBoat(id));
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
                    return true;
                }
            }
            return super.mouseClicked(mx, my, b);
        }
    }

    /** Справка «Броня+»: как улучшать броню (сам стол — в квартире). */
    public static class ArmorBench extends PlainScreen {
        public ArmorBench() { super("Улучшение брони", 280, 196, true); }
        @Override protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
            String[][] rows = {
                    {"Где", "Стол брони — в твоей квартире, у двери"},
                    {"Опыт", "Клади хлам: монеты, шестерни, реликвии,"},
                    {"", "осколки инея, ненужную броню +0"},
                    {"+1", "Полная полоска + Ядро брони"},
                    {"Ядро", "Алтарь в подвале: Медальон инея"},
                    {"", "в центре и 8 звёзд по краям"},
                    {"✦ в ядре", "+7 и +8 — 1, +9 — 2, +10 — 3"},
                    {"Свиток", "Свиток закалки на +5 и на +10"},
                    {"Риск", "До +10 — никакого"},
                    {"+11", "6 благословенных + 2 кровавые,"},
                    {"", "50%; провал — броня падает до +5"},
                    {"Бонус", "каждый плюс: +8% брони и здоровья"}};
            for (int i = 0; i < rows.length; i++) {
                int y = top + 22 + i * 13;
                g.drawString(font, rows[i][0], left + 14, y, Skin.GOLD_L, false);
                g.drawString(font, rows[i][1], left + 70, y, Skin.TEXT, false);
            }
        }
    }

    @SuppressWarnings("unused") private static final int L = ClientState.level;
}
