package com.pvpciv.client.screen;

import com.pvpciv.client.ui.Skin;
import com.pvpciv.game.ArmorUp;
import com.pvpciv.item.ArmorCoreItem;
import com.pvpciv.item.CivArmorItem;
import com.pvpciv.menu.ArmorBenchMenu;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** Стол брони: полоска опыта, требования следующего уровня (ядро, свиток, звёзды для +11). */
public class ArmorBenchScreen extends CivScreen<ArmorBenchMenu> {
    public ArmorBenchScreen(ArmorBenchMenu m, Inventory inv, Component t) { super(m, inv, "Стол брони", 340, 206); }

    @Override protected String hintFor(Slot s) {
        int i = menu.slots.indexOf(s);
        return i == 0 ? "chest" : i == 1 ? "artifact" : null;
    }

    private int count(Predicate<ItemStack> p) {
        int n = 0;
        for (int i = 3; i < menu.slots.size(); i++) { ItemStack st = menu.slots.get(i).getItem(); if (p.test(st)) n += st.getCount(); }
        return n;
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        ItemStack armor = menu.box.getItem(0), junk = menu.box.getItem(1);
        Skin.inset(g, x + 10, y + 22, 152, 128);
        for (String[] l : new String[][]{{"броня", "29"}, {"ядро", "63"}, {"хлам", "97"}})
            g.drawString(font, l[0], x + 30 - font.width(l[0]) / 2, y + Integer.parseInt(l[1]), Skin.FAINT, false);
        boolean can = false;
        if (!(armor.getItem() instanceof CivArmorItem)) {
            g.drawString(font, "Положи броню.", x + 52, y + 34, Skin.GOLD_L, false);
            g.drawString(font, "Хлам → опыт брони", x + 52, y + 50, Skin.DIM, false);
            g.drawString(font, "Полоска + ядро → +1", x + 52, y + 61, Skin.DIM, false);
            g.drawString(font, "Свиток: на +5 и +10", x + 52, y + 72, Skin.DIM, false);
            g.drawString(font, "До +10 — без риска", x + 52, y + 83, Skin.GOOD, false);
        } else {
            int lv = ArmorUp.level(armor);
            g.drawString(font, "Уровень +" + lv + (lv >= ArmorUp.MAX ? " (макс.)" : ""), x + 46, y + 28, Skin.GOLD_L, false);
            if (lv < ArmorUp.MAX) {
                int need = ArmorUp.need(Math.min(lv, 10)), xp = ArmorUp.xp(armor);
                Skin.bar(g, x + 46, y + 40, 110, xp / (float) need, 0xFF8FE3FF, 0xFF2A7FB8, "blue");
                String t = xp + " / " + need;
                g.drawString(font, t, x + 101 - font.width(t) / 2, y + 54, Skin.TEXT, false);
                int target = lv + 1, ly = y + 66;
                boolean full = xp >= need;
                if (lv < 10) {
                    int cn = ArmorUp.coreNeed(target);
                    ItemStack core = menu.box.getItem(2);
                    boolean ok = core.is(ModRegistry.ARMOR_CORE.get()) && ArmorCoreItem.level(core) >= cn;
                    g.renderItem(ArmorCoreItem.of(cn), x + 44, ly);
                    g.drawString(font, "Ядро +" + cn + "+", x + 62, ly + 4, Skin.TEXT, false);
                    String cs = core.isEmpty() ? "в слот" : ok ? "✓" : "слабое";
                    g.drawString(font, cs, x + 160 - font.width(cs), ly + 4, ok ? Skin.GOOD : 0xFFFF7060, false);
                    ly += 17;
                    if (ArmorUp.scrollNeed(target)) {
                        int sh = count(st -> st.is(ModRegistry.TEMPER_SCROLL.get()));
                        ly = req(g, x + 44, ly, ModRegistry.TEMPER_SCROLL.get(), "Свиток закалки", sh, 1);
                        ok &= sh > 0;
                    }
                    can = full && ok;
                } else {
                    int bl = count(st -> st.is(ModRegistry.BLESSED_STAR_W1.get())), bd = count(st -> st.is(ModRegistry.BLOOD_STAR_W1.get()));
                    ly = req(g, x + 44, ly, ModRegistry.BLESSED_STAR_W1.get(), "Благослов.", bl, ArmorUp.BLESSED_11);
                    ly = req(g, x + 44, ly, ModRegistry.BLOOD_STAR_W1.get(), "Кровавые", bd, ArmorUp.BLOOD_11);
                    g.drawString(font, "50%: провал — до +5", x + 44, ly + 2, 0xFFFF7060, false);
                    can = full && bl >= ArmorUp.BLESSED_11 && bd >= ArmorUp.BLOOD_11;
                }
                int v = ArmorUp.junkFor(armor, junk);
                if (v > 0) g.drawString(font, "+" + v + "/шт" + (ArmorUp.native_(armor, junk) ? " · родной ×1.5" : ""), x + 44, y + 138, Skin.GOOD, false);
            }
        }
        boolean feed = armor.getItem() instanceof CivArmorItem && ArmorUp.junk(junk) > 0 && !ArmorUp.full(armor);
        Skin.button(g, font, x + 10, y + 154, 74, "Вложить", mx, my, feed, false);
        Skin.button(g, font, x + 88, y + 154, 74, ArmorUp.level(armor) == 10 ? "Гамба +11" : "Улучшить", mx, my, can, true);
        g.drawString(font, "Ядра — алтарь в подвале", x + 12, y + 178, Skin.FAINT, false);
        g.drawString(font, "Сумка", x + 169, y + 26, Skin.DIM, false);
        drawMoney(g, x + 330, y + 26);
        g.drawString(font, "Быстрые слоты", x + 169, y + 170, Skin.DIM, false);
    }

    private int req(GuiGraphics g, int x, int y, Item it, String name, int have, int need) {
        g.renderItem(new ItemStack(it), x, y);
        g.drawString(font, name, x + 18, y + 4, Skin.TEXT, false);
        String t = have + "/" + need;
        g.drawString(font, t, x + 116 - font.width(t), y + 4, have >= need ? Skin.GOOD : 0xFFFF7060, false);
        return y + 17;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (in(mx, my, 10, 154, 74, 16)) { fx.burst(leftPos + 30, topPos + 48, 16, 0x8FE3FF, 50); click(ArmorBenchMenu.BTN_FEED); return true; }
        if (in(mx, my, 88, 154, 74, 16)) { fx.burst(leftPos + 30, topPos + 48, 36, 0xFFE08A, 90); click(ArmorBenchMenu.BTN_UP); return true; }
        return super.mouseClicked(mx, my, b);
    }

    /** 0.9.2: подсказки на пустых слотах стола. */
    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        if (hoveredSlot != null && !hoveredSlot.hasItem() && menu.getCarried().isEmpty() && hoveredSlot.index < 3) {
            java.util.List<net.minecraft.network.chat.Component> t = new java.util.ArrayList<>();
            java.util.function.BiConsumer<String, net.minecraft.ChatFormatting> l = (x, c) -> t.add(net.minecraft.network.chat.Component.literal(x).withStyle(c));
            switch (hoveredSlot.index) {
                case 0 -> { l.accept("Броня", net.minecraft.ChatFormatting.GOLD); l.accept("Положи сюда шлем, нагрудник, поножи или ботинки.", net.minecraft.ChatFormatting.GRAY); }
                case 1 -> {
                    l.accept("Хлам — опыт брони", net.minecraft.ChatFormatting.GOLD);
                    l.accept("Хлам с мобов (то, что продаётся за искры)", net.minecraft.ChatFormatting.GRAY);
                    l.accept("и лишняя броня +0. Редкие материалы — нет.", net.minecraft.ChatFormatting.GRAY);
                    l.accept("Родной хлам комплекта — ×1,5:", net.minecraft.ChatFormatting.GRAY);
                    l.accept(" Инеевый — инеевый осколок", net.minecraft.ChatFormatting.DARK_AQUA);
                    l.accept(" Ликвидатора — обрезки резины", net.minecraft.ChatFormatting.DARK_AQUA);
                    l.accept(" Самурайский — обрезки шёлка", net.minecraft.ChatFormatting.DARK_AQUA);
                    l.accept(" Лисий — лисий мех", net.minecraft.ChatFormatting.DARK_AQUA);
                    l.accept("Полоска опыта заполнена → нужно ядро для +1.", net.minecraft.ChatFormatting.DARK_GRAY);
                }
                default -> {
                    ItemStack ar = menu.box.getItem(0);
                    if (ar.getItem() instanceof com.pvpciv.item.CivArmorItem ca) {   // 0.9.3: рецепт именно под эту броню
                        int lv = ArmorUp.level(ar), tg = lv + 1;
                        if (lv >= 10) {
                            l.accept("Ядро не нужно", net.minecraft.ChatFormatting.GOLD);
                            l.accept("+11 — благословенные и кровавые звёзды, 50%.", net.minecraft.ChatFormatting.GRAY);
                        } else {
                            String center = ca.tier < 2 ? "Медальон инея" : ca.tier < 3 ? "Фильтр противогаза" : "Нефритовая печать";
                            l.accept("Нужно ядро +" + tg + " (или сильнее)", net.minecraft.ChatFormatting.GOLD);
                            l.accept("для: " + ca.set + " +" + lv + " → +" + tg, net.minecraft.ChatFormatting.GRAY);
                            l.accept("Алтарь инфузии (подвал Кармиллы):", net.minecraft.ChatFormatting.GRAY);
                            l.accept(" центр — " + center, net.minecraft.ChatFormatting.DARK_AQUA);
                            l.accept(tg <= 8 ? " по кругу — " + tg + " звёзд заточки"
                                    : " по кругу — " + (16 - tg) + " обычных + " + (tg - 8) + " благословенных зв.", net.minecraft.ChatFormatting.DARK_AQUA);
                            l.accept("(центр можно любой из трёх — этот родной для тира " + (int) Math.floor(ca.tier) + ")", net.minecraft.ChatFormatting.DARK_GRAY);
                        }
                        g.renderComponentTooltip(font, t, mx, my);
                        return;
                    }
                    l.accept("Ядро брони", net.minecraft.ChatFormatting.GOLD);
                    l.accept("Нужно ядро уровня цели (+N) или выше — тратится.", net.minecraft.ChatFormatting.GRAY);
                    l.accept("Крафт — алтарь инфузии в подвале Кармиллы:", net.minecraft.ChatFormatting.GRAY);
                    l.accept(" центр: медальон инея / фильтр противогаза /", net.minecraft.ChatFormatting.DARK_AQUA);
                    l.accept("        нефритовая печать (любой подходит)", net.minecraft.ChatFormatting.DARK_AQUA);
                    l.accept(" по кругу: звёзды заточки — ядро +N из N звёзд;", net.minecraft.ChatFormatting.DARK_AQUA);
                    l.accept(" для +9…+10 часть звёзд — благословенные.", net.minecraft.ChatFormatting.DARK_AQUA);
                    l.accept("Подробно — кнопка «Гайд» у алтаря.", net.minecraft.ChatFormatting.DARK_GRAY);
                }
            }
            g.renderComponentTooltip(font, t, mx, my);
            return;
        }
        super.renderTooltip(g, mx, my);
    }
}
