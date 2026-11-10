package com.pvpciv.client.screen;

import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Fx;
import com.pvpciv.client.ui.Skin;
import com.pvpciv.inv.Bag;
import com.pvpciv.inv.BagSlot;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.menu.CivMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Основа всех окон с предметами. Наследник задаёт размер, рисует содержимое в drawContent
 * (координаты — абсолютные, используй leftPos/topPos) и при желании подсказки пустых слотов в hintFor.
 * Раскладка слотов задаётся в меню (menu/*.java) и должна совпадать с рисунком.
 */
public abstract class CivScreen<M extends CivMenu> extends AbstractContainerScreen<M> {
    protected final Fx fx = new Fx();
    private long lastMoney = Long.MIN_VALUE;
    private int moneyX, moneyY;

    protected CivScreen(M menu, Inventory inv, String title, int w, int h) {
        super(menu, inv, Component.literal(title));
        this.imageWidth = w;
        this.imageHeight = h;
    }

    protected void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            menu.clickMenuButton(minecraft.player, id);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    protected abstract void drawContent(GuiGraphics g, int mx, int my, float pt);
    /** Поверх предметов (z=300): затемнения, плашки, эффекты. */
    protected void drawOverlay(GuiGraphics g, int mx, int my) {}
    /** Силуэт для пустого слота: weapon/star/helmet/chest/legs/boots/shield/artifact или null. */
    protected String hintFor(Slot s) { return null; }

    /** Искры игрока в углу окна; при изменении всплывает «+N». */
    protected void drawMoney(GuiGraphics g, int xRight, int y) {
        moneyX = xRight - 14; moneyY = y;
        Skin.money(g, font, xRight, y, ClientState.money, Skin.GOLD_L);
    }

    @Override
    protected void containerTick() {
        if (lastMoney != Long.MIN_VALUE && ClientState.money != lastMoney) {
            long d = ClientState.money - lastMoney;
            fx.pop((d > 0 ? "+" : "") + d, moneyX, moneyY + 10, d > 0 ? 0xFFE08A : 0xFF7A6A);
        }
        lastMoney = ClientState.money;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, 0xB0050408, 0xD00A0612);
        renderBg(g, pt, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        Skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        Skin.header(g, font, leftPos + imageWidth / 2, topPos - 8, title.getString());
        drawContent(g, mx, my, pt);
        for (Slot s : menu.slots) {
            if (!s.isActive()) continue;
            int x = leftPos + s.x, y = topPos + s.y;
            if (s instanceof BagSlot b && b.locked()) { Skin.slotLocked(g, x, y); continue; }
            Skin.slot(g, x, y);
            ItemStack st = s.getItem();
            if (st.isEmpty()) { String h = hintFor(s); if (h != null) Skin.hint(g, h, x, y); }
            else if (st.getItem() instanceof CivWeaponItem w)
                Skin.rarity(g, x, y, (Gear.rarity(st).ordinal() < w.minRarity.ordinal() ? w.minRarity : Gear.rarity(st)).color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int sx = fx.shakeX();
        g.pose().pushPose();
        g.pose().translate(sx, 0, 0);
        super.render(g, mx, my, pt);
        g.pose().translate(0, 0, 300);
        for (Slot s : menu.slots) {
            if (!s.isActive()) continue;
            ItemStack st = s.getItem();
            int stars = Gear.stars(st);
            if (st.getItem() instanceof CivWeaponItem && stars > 0)
                g.drawString(font, "+" + stars, leftPos + s.x, topPos + s.y, 0xFF000000 | Gear.starColor(stars), true);
        }
        drawOverlay(g, mx, my);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        fx.render(g, font, width, height);
        g.pose().popPose();
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        if (hoveredSlot instanceof BagSlot b && b.locked() && menu.getCarried().isEmpty()) {
            int have = 0;
            for (int m : Bag.LEVEL_MILESTONES) if (ClientState.level >= m) have++;
            int need = (b.bagIndex - menu.unlockedSlots()) / 9 + 1;
            String how = have + need <= Bag.LEVEL_MILESTONES.length ? "Откроется на уровне " + Bag.LEVEL_MILESTONES[have + need - 1] : "Нужна Пряжка рюкзака";
            g.renderComponentTooltip(font, List.of(Component.literal("Слот закрыт").withStyle(ChatFormatting.GOLD),
                    Component.literal(how).withStyle(ChatFormatting.GRAY),
                    Component.literal("Пряжка рюкзака: +3 слота сразу").withStyle(ChatFormatting.DARK_GRAY)), mx, my);
            return;
        }
        super.renderTooltip(g, mx, my);
    }

    protected boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h;
    }

    /** Блок «станция ур. N» с кнопкой улучшения (общий для всех станций). Высота 30. */
    protected void stationBlock(GuiGraphics g, int x, int y, int w, String name, int lvl, int max, String bonus, long cost, int dust, int mx, int my) {
        Skin.inset(g, x, y, w, 30);
        g.drawString(font, name + " ур. " + lvl + "/" + max, x + 5, y + 4, Skin.GOLD_L, false);
        g.drawString(font, bonus, x + w - 5 - font.width(bonus), y + 4, Skin.GOOD, false);
        if (lvl >= max) { g.drawString(font, "Максимальный уровень", x + 5, y + 17, Skin.DIM, false); return; }
        boolean ok = ClientState.money >= cost;
        g.drawString(font, cost + "✦ +" + dust + " пыли", x + 5, y + 17, ok ? Skin.TEXT : 0xFFB06A5A, false);
        Skin.button(g, font, x + w - 61, y + 12, 58, "Улучшить", mx, my, ok, false);
    }

    /** Попадание по кнопке улучшения из stationBlock. */
    protected boolean stationClick(double mx, double my, int x, int y, int w) { return in(mx, my, x + w - 61, y + 12, 58, 16); }
}
