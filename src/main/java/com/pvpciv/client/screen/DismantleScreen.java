package com.pvpciv.client.screen;

import com.pvpciv.client.ClientState;
import com.pvpciv.menu.DismantleMenu;
import com.pvpciv.menu.Stations;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class DismantleScreen extends CivScreen<DismantleMenu> {
    public DismantleScreen(DismantleMenu m, Inventory inv, Component t) { super(m, inv, Component.literal("Разборочный стол"), 320, 194); }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        CivUi.inset(g, x + 8, y + 24, 136, 104);
        g.drawString(font, "Положи обломки", x + 54, y + 34, CivUi.TEXT, false);
        g.drawString(font, "или ненужное", x + 54, y + 46, CivUi.TEXT, false);
        g.drawString(font, "оружие", x + 54, y + 58, CivUi.TEXT, false);
        g.drawString(font, "→ пыль, Искры,", x + 54, y + 72, CivUi.DIM, false);
        g.drawString(font, "шанс звёзд", x + 54, y + 82, CivUi.DIM, false);
        ItemStack in = menu.box.getItem(0);
        CivUi.button(g, font, x + 54, y + 98, 86, 16, "Разобрать", mx, my, !in.isEmpty(), 0x8A4A2A);
        CivUi.inset(g, x + 8, y + 132, 136, 54);
        int lvl = menu.stationLevel();
        g.drawString(font, "Стол ур. " + lvl + "/" + Stations.MAX, x + 14, y + 137, CivUi.GOLD, false);
        g.drawString(font, "Выход материалов: +" + (lvl - 1) * 15 + "%", x + 14, y + 149, CivUi.DIM, false);
        if (lvl < Stations.MAX) {
            long cost = Stations.moneyCost(Stations.DISMANTLE, lvl);
            CivUi.button(g, font, x + 14, y + 162, 124, 16, "Улучшить " + cost + "✦ +" + Stations.dustCost(lvl) + " пыли", mx, my, ClientState.money >= cost, 0x2A6A8A);
        } else g.drawString(font, "Максимальный уровень", x + 14, y + 166, 0xFF7CFC9A, false);
        g.drawString(font, "Сумка", x + 150, y + 21, CivUi.DIM, false);
        g.drawString(font, "✦ " + ClientState.money, x + 270, y + 21, 0xFFFFC94A, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (in(mx, my, 54, 98, 86, 16)) { click(DismantleMenu.BTN_GO); return true; }
        if (in(mx, my, 14, 162, 124, 16)) { click(DismantleMenu.BTN_UPGRADE); return true; }
        return super.mouseClicked(mx, my, b);
    }
}
