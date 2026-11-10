package com.pvpciv.client.screen;

import com.pvpciv.client.ui.Skin;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.menu.DismantleMenu;
import com.pvpciv.menu.Stations;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Разборочный стол: вход, предпросмотр выхода, улучшение станции. */
public class DismantleScreen extends CivScreen<DismantleMenu> {
    private boolean hadItem;

    public DismantleScreen(DismantleMenu m, Inventory inv, Component t) { super(m, inv, "Разборочный стол", 340, 206); }

    @Override protected String hintFor(Slot s) { return menu.slots.indexOf(s) == 0 ? "weapon" : null; }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        ItemStack in = menu.box.getItem(0);
        Skin.inset(g, x + 10, y + 24, 152, 70);
        Skin.glow(g, x + 85, y + 41, 20, in.isEmpty() ? 0x20FFFFFF : 0x60FF8C42);
        g.drawCenteredString(font, "обломки или оружие", x + 86, y + 58, Skin.DIM);
        Skin.divider(g, x + 86, y + 71);
        g.drawCenteredString(font, "кроме меча новичка", x + 86, y + 81, Skin.FAINT);
        Skin.inset(g, x + 10, y + 98, 152, 54);
        if (in.isEmpty()) {
            g.drawString(font, "Что получишь:", x + 16, y + 103, Skin.GOLD_L, false);
            g.drawString(font, "пепельную пыль, Искры", x + 16, y + 116, Skin.DIM, false);
            g.drawString(font, "и, если повезёт, звёзды", x + 16, y + 127, Skin.DIM, false);
        } else {
            double m = menu.mult();
            int ly = y + 101;
            if (in.is(ModRegistry.WEAPON_SHARDS.get())) {
                int n = in.getCount();
                ly = out(g, x + 14, ly, ModRegistry.ASH_DUST.get(), "Пыль", "×" + Math.round(n * m) + "–" + Math.round(2 * n * m));
                ly = out(g, x + 14, ly, ModRegistry.SPARKS.get(), "Искры", "×" + Math.round(2 * n * m) + "–" + Math.round(4 * n * m));
                out(g, x + 14, ly, ModRegistry.STAR_W1.get(), "Звезда", "шанс " + pct(1 - Math.pow(1 - Math.min(1, 0.04 * m), n)));
            } else if (in.getItem() instanceof CivWeaponItem w) {
                int rr = Gear.rarity(in).ordinal(), s = Gear.stars(in);
                ly = out(g, x + 14, ly, ModRegistry.ASH_DUST.get(), "Пыль", "×" + Math.round((2 + rr * 2 + w.tier) * m));
                ly = out(g, x + 14, ly, ModRegistry.SPARKS.get(), "Искры", "×" + Math.round(10 * (rr + 1) * w.tier * m));
                String st = "шанс " + pct(Math.min(1, 0.10 * (rr + 1) * m)) + (s > 0 ? " +" + s + "×" + pct(Math.min(1, 0.3 * m)) : "");
                if (rr >= 3) st += ", благ. " + pct(Math.min(1, 0.10 * (rr - 2) * m));
                out(g, x + 14, ly, ModRegistry.STAR_W1.get(), "Звезда", st);
            }
        }
        Skin.button(g, font, x + 10, y + 154, 152, "Разобрать", mx, my, !in.isEmpty(), true);
        int lvl = menu.stationLevel();
        stationBlock(g, x + 10, y + 173, 152, "Стол", lvl, Stations.MAX, lvl > 1 ? "+" + (lvl - 1) * 15 + "% выход" : "без бонуса", Stations.moneyCost(Stations.DISMANTLE, lvl), Stations.dustCost(lvl), mx, my);
        g.drawString(font, "Сумка", x + 169, y + 26, Skin.DIM, false);
        drawMoney(g, x + 330, y + 26);
        g.drawString(font, "Быстрые слоты", x + 169, y + 170, Skin.DIM, false);
    }

    private static String pct(double v) { return Math.round(v * 100) + "%"; }

    private int out(GuiGraphics g, int x, int y, Item item, String name, String text) {
        g.renderItem(new ItemStack(item), x, y);
        g.drawString(font, name, x + 19, y + 4, Skin.TEXT, false);
        g.drawString(font, text, x + 144 - font.width(text), y + 4, Skin.GOLD_L, false);
        return y + 17;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean has = !menu.box.getItem(0).isEmpty();
        hadItem = has;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (in(mx, my, 10, 154, 152, 16)) {
            if (hadItem) { fx.burst(leftPos + 85, topPos + 41, 30, 0xC0C8D8, 80); fx.burst(leftPos + 85, topPos + 41, 14, 0xFFB040, 60); }
            click(DismantleMenu.BTN_GO); return true;
        }
        int lvl = menu.stationLevel();
        if (lvl < Stations.MAX && stationClick(mx, my, 10, 173, 152)) { click(DismantleMenu.BTN_UPGRADE); return true; }
        return super.mouseClicked(mx, my, b);
    }
}
