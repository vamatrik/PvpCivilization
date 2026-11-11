package com.pvpciv.client.screen;

import com.pvpciv.client.ui.Skin;
import com.pvpciv.menu.HomeChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Домашний сундук: 45 личных слотов + сумка. */
public class HomeChestScreen extends CivScreen<HomeChestMenu> {
    public HomeChestScreen(HomeChestMenu m, Inventory inv, Component t) { super(m, inv, "Домашний сундук", 357, 206); }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        g.drawString(font, "Хранилище (только твоё)", x + 10, y + 26, Skin.GOLD_L, false);
        g.drawString(font, "Shift+клик — перенести", x + 10, y + 132, Skin.FAINT, false);
        g.drawString(font, "Вещи здесь не пропадают", x + 10, y + 144, Skin.FAINT, false);
        g.drawString(font, "Сумка", x + 185, y + 26, Skin.DIM, false);
        drawMoney(g, x + 347, y + 26);
        g.drawString(font, "Быстрые слоты", x + 185, y + 170, Skin.DIM, false);
    }
}
