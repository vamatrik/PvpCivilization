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
        for (int i = 2; i < menu.slots.size(); i++) { ItemStack st = menu.slots.get(i).getItem(); if (p.test(st)) n += st.getCount(); }
        return n;
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        ItemStack armor = menu.box.getItem(0), junk = menu.box.getItem(1);
        Skin.inset(g, x + 10, y + 22, 152, 128);
        g.drawString(font, "броня", x + 18, y + 33, Skin.FAINT, false);
        g.drawString(font, "хлам", x + 19, y + 89, Skin.FAINT, false);
        boolean can = false;
        if (!(armor.getItem() instanceof CivArmorItem)) {
            g.drawString(font, "Положи броню.", x + 46, y + 34, Skin.GOLD_L, false);
            g.drawString(font, "Хлам → опыт брони", x + 46, y + 50, Skin.DIM, false);
            g.drawString(font, "Полоска + ядро → +1", x + 46, y + 61, Skin.DIM, false);
            g.drawString(font, "Свиток: на +5 и +10", x + 46, y + 72, Skin.DIM, false);
            g.drawString(font, "До +10 — без риска", x + 46, y + 83, Skin.GOOD, false);
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
                    int have = count(st -> st.is(ModRegistry.ARMOR_CORE.get()) && ArmorCoreItem.level(st) >= cn);
                    ly = req(g, x + 44, ly, ModRegistry.ARMOR_CORE.get(), "Ядро +" + cn + " или выше", have, 1);
                    boolean ok = have > 0;
                    if (ArmorUp.scrollNeed(target)) {
                        int sh = count(st -> st.is(ModRegistry.TEMPER_SCROLL.get()));
                        ly = req(g, x + 44, ly, ModRegistry.TEMPER_SCROLL.get(), "Свиток закалки", sh, 1);
                        ok &= sh > 0;
                    }
                    can = full && ok;
                } else {
                    int bl = count(st -> st.is(ModRegistry.BLESSED_STAR_W1.get())), bd = count(st -> st.is(ModRegistry.BLOOD_STAR_W1.get()));
                    ly = req(g, x + 44, ly, ModRegistry.BLESSED_STAR_W1.get(), "Благословенные", bl, ArmorUp.BLESSED_11);
                    ly = req(g, x + 44, ly, ModRegistry.BLOOD_STAR_W1.get(), "Кровавые", bd, ArmorUp.BLOOD_11);
                    g.drawString(font, "50%! Провал — откат до +5", x + 44, ly + 2, 0xFFFF7060, false);
                    can = full && bl >= ArmorUp.BLESSED_11 && bd >= ArmorUp.BLOOD_11;
                }
                int v = ArmorUp.junkFor(armor, junk);
                if (v > 0) g.drawString(font, "+" + v + " за шт." + (ArmorUp.native_(armor, junk) ? " (родной ×1.5)" : ""), x + 44, y + 138, Skin.GOOD, false);
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
        g.drawString(font, t, x + 114 - font.width(t), y + 4, have >= need ? Skin.GOOD : 0xFFFF7060, false);
        return y + 17;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (in(mx, my, 10, 154, 74, 16)) { fx.burst(leftPos + 30, topPos + 52, 16, 0x8FE3FF, 50); click(ArmorBenchMenu.BTN_FEED); return true; }
        if (in(mx, my, 88, 154, 74, 16)) { fx.burst(leftPos + 30, topPos + 52, 36, 0xFFE08A, 90); click(ArmorBenchMenu.BTN_UP); return true; }
        return super.mouseClicked(mx, my, b);
    }
}
