package com.pvpciv.client.screen;

import com.pvpciv.client.ClientState;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.loot.Rarity;
import com.pvpciv.menu.ForgeMenu;
import com.pvpciv.menu.Stations;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ForgeScreen extends CivScreen<ForgeMenu> {
    public ForgeScreen(ForgeMenu m, Inventory inv, Component t) { super(m, inv, Component.literal("Кузница"), 320, 194); }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        CivUi.inset(g, x + 8, y + 24, 136, 104);
        g.drawString(font, "Оружие", x + 14, y + 36, CivUi.DIM, false);
        g.drawString(font, "Звезда", x + 14, y + 80, CivUi.DIM, false);
        ItemStack w = menu.box.getItem(0), star = menu.box.getItem(1);
        int tx = x + 54;
        if (w.getItem() instanceof CivWeaponItem cw) {
            int s = Gear.stars(w);
            String name = w.getHoverName().getString();
            if (font.width(name) > 86) name = font.plainSubstrByWidth(name, 82) + "…";
            g.drawString(font, name, tx, y + 30, 0xFF000000 | Gear.rarity(w).color, false);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) sb.append(i < s ? "★" : "☆");
            g.drawString(font, sb.toString(), tx, y + 42, 0xFF000000 | Gear.starColor(Math.max(1, s)), false);
            if (s < Gear.MAX_STARS) {
                Rarity r = Gear.rarity(w).ordinal() < cw.minRarity.ordinal() ? cw.minRarity : Gear.rarity(w);
                double now = CivWeaponItem.damage(cw.tier, cw.cls, r, s), next = CivWeaponItem.damage(cw.tier, cw.cls, r, s + 1);
                g.drawString(font, String.format("Урон %.1f → %.1f", now, next), tx, y + 54, CivUi.TEXT, false);
                int pc = (int) Math.round(menu.chance() * 100);
                g.drawString(font, "Шанс: " + pc + "%", tx, y + 66, pc >= 70 ? 0xFF7CFC9A : pc >= 40 ? 0xFFFFD166 : 0xFFFF6B6B, false);
                boolean blessed = star.is(ModRegistry.BLESSED_STAR_W1.get());
                g.drawString(font, star.isEmpty() ? "Положи звезду" : blessed ? "Провал: −1 заточка" : "Провал: ПОЛОМКА", tx, y + 78,
                        star.isEmpty() ? CivUi.DIM : blessed ? 0xFFC0C8D8 : 0xFFFF5050, false);
            } else g.drawString(font, "Максимум +10", tx, y + 54, 0xFFFF5050, false);
        } else {
            g.drawString(font, "Положи оружие", tx, y + 30, CivUi.DIM, false);
            g.drawString(font, "и звезду", tx, y + 42, CivUi.DIM, false);
        }
        boolean can = menu.progress() == 0 && w.getItem() instanceof CivWeaponItem && !star.isEmpty() && Gear.stars(w) < Gear.MAX_STARS;
        CivUi.button(g, font, tx, y + 94, 86, 16, menu.progress() > 0 ? "Заточка..." : "Заточить", mx, my, can, 0xB0602A);
        if (menu.progress() > 0) CivUi.bar(g, tx, y + 113, 86, 6, menu.progress() / (float) ForgeMenu.RITUAL, 0xE8B04A);
        else {
            String res = switch (menu.result()) { case 1 -> "УСПЕХ!"; case 2 -> "Неудача: −1"; case 3 -> "СЛОМАНО"; default -> ""; };
            int rc = switch (menu.result()) { case 1 -> 0xFF7CFC9A; case 3 -> 0xFFFF5050; default -> 0xFFC0C8D8; };
            g.drawCenteredString(font, res, tx + 43, y + 114, rc);
        }
        // станция
        CivUi.inset(g, x + 8, y + 132, 136, 54);
        int lvl = menu.stationLevel();
        g.drawString(font, "Кузница ур. " + lvl + "/" + Stations.MAX, x + 14, y + 137, CivUi.GOLD, false);
        g.drawString(font, "Бонус к шансу: +" + (lvl - 1) + "%", x + 14, y + 149, CivUi.DIM, false);
        if (lvl < Stations.MAX) {
            long cost = Stations.moneyCost(Stations.FORGE, lvl);
            boolean ok = ClientState.money >= cost;
            CivUi.button(g, font, x + 14, y + 162, 124, 16, "Улучшить " + cost + "✦ +" + Stations.dustCost(lvl) + " пыли", mx, my, ok, 0x2A6A8A);
        } else g.drawString(font, "Максимальный уровень", x + 14, y + 166, 0xFF7CFC9A, false);
        g.drawString(font, "Сумка", x + 150, y + 21, CivUi.DIM, false);
        g.drawString(font, "✦ " + ClientState.money, x + 270, y + 21, 0xFFFFC94A, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (in(mx, my, 54, 94, 86, 16)) { click(ForgeMenu.BTN_ENHANCE); return true; }
        if (in(mx, my, 14, 162, 124, 16)) { click(ForgeMenu.BTN_UPGRADE); return true; }
        return super.mouseClicked(mx, my, b);
    }
}
