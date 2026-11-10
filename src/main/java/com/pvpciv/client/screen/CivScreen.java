package com.pvpciv.client.screen;

import com.pvpciv.inv.BagSlot;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.menu.CivMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Основа экранов: панель, рамки слотов, редкость и заточка на иконках. */
public abstract class CivScreen<M extends CivMenu> extends AbstractContainerScreen<M> {
    protected CivScreen(M menu, Inventory inv, Component title, int w, int h) {
        super(menu, inv, title);
        this.imageWidth = w;
        this.imageHeight = h;
    }

    protected void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            menu.clickMenuButton(minecraft.player, id);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        CivUi.panel(g, leftPos, topPos, imageWidth, imageHeight);
        CivUi.header(g, font, leftPos, topPos, imageWidth, title.getString());
        drawContent(g, mx, my, pt);
        for (Slot s : menu.slots) {
            if (!s.isActive()) continue;
            int x = leftPos + s.x, y = topPos + s.y;
            if (s instanceof BagSlot b && b.locked()) CivUi.lockedSlot(g, x, y);
            else CivUi.slot(g, x, y);
            ItemStack st = s.getItem();
            if (st.getItem() instanceof CivWeaponItem w) {
                int r = Gear.rarity(st).ordinal() < w.minRarity.ordinal() ? w.minRarity.color : Gear.rarity(st).color;
                CivUi.rarityFrame(g, x, y, r);
            }
        }
    }

    protected abstract void drawContent(GuiGraphics g, int mx, int my, float pt);

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        for (Slot s : menu.slots) {
            if (!s.isActive()) continue;
            ItemStack st = s.getItem();
            int stars = Gear.stars(st);
            if (st.getItem() instanceof CivWeaponItem && stars > 0) {
                g.drawString(font, "+" + stars, leftPos + s.x, topPos + s.y, 0xFF000000 | Gear.starColor(stars), true);
            }
        }
        drawOverlay(g, mx, my);
        g.pose().popPose();
        renderTooltip(g, mx, my);
    }

    protected void drawOverlay(GuiGraphics g, int mx, int my) {}

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, 0xA0000000, 0xC0000008);
        renderBg(g, pt, mx, my);
    }

    protected boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h;
    }
}
