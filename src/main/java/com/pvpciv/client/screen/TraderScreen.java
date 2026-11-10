package com.pvpciv.client.screen;

import com.pvpciv.client.ClientState;
import com.pvpciv.game.Shop;
import com.pvpciv.menu.TraderMenu;
import com.pvpciv.net.Network;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TraderScreen extends CivScreen<TraderMenu> {
    public TraderScreen(TraderMenu m, Inventory inv, Component t) { super(m, inv, Component.literal("Сорока • лавка"), 346, 194); }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        CivUi.inset(g, x + 8, y + 24, 162, 146);
        List<Network.OfferView> offers = ClientState.offers;
        for (int i = 0; i < offers.size() && i < 8; i++) {
            Network.OfferView o = offers.get(i);
            int oy = y + 28 + i * 17;
            boolean hover = in(mx, my, 10, 28 + i * 17, 158, 16);
            boolean can = o.left() > 0 && ClientState.money >= o.price();
            g.fill(x + 10, oy, x + 168, oy + 16, hover && can ? 0xFF2A3658 : 0xFF161C2E);
            g.renderItem(o.stack(), x + 11, oy);
            g.renderItemDecorations(font, o.stack(), x + 11, oy);
            String name = o.stack().getHoverName().getString();
            if (font.width(name) > 80) name = font.plainSubstrByWidth(name, 76) + "…";
            g.drawString(font, name, x + 30, oy + 4, o.left() > 0 ? CivUi.TEXT : 0xFF5A6478, false);
            String price = o.price() + "✦";
            g.drawString(font, price, x + 166 - font.width(price), oy + 4, can ? 0xFFFFC94A : 0xFF8A6A4A, false);
            String left = o.left() >= 999 ? "∞" : "×" + o.left();
            g.drawString(font, left, x + 118, oy + 4, CivUi.DIM, false);
        }
        long ms = Math.max(0, ClientState.shopRotationAt - System.currentTimeMillis());
        g.drawString(font, String.format("Новый завоз через %d:%02d", ms / 3600000, (ms / 60000) % 60), x + 10, y + 174, CivUi.DIM, false);
        CivUi.button(g, font, x + 176, y + 2, 92, 14, "Продать хлам", mx, my, true, 0x6A5A2A);
        g.drawString(font, "ПКМ по предмету — продать", x + 176, y + 21, CivUi.DIM, false);
        g.drawString(font, "✦ " + ClientState.money, x + 290, y + 5, 0xFFFFC94A, true);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        List<Network.OfferView> offers = ClientState.offers;
        for (int i = 0; i < offers.size() && i < 8; i++) {
            if (in(mx, my, 10, 28 + i * 17, 158, 16)) {
                List<Component> tip = new ArrayList<>(getTooltipFromContainerItem(offers.get(i).stack()));
                tip.add(Component.literal("ЛКМ — купить за " + offers.get(i).price() + " ✦").withStyle(ChatFormatting.GOLD));
                g.renderComponentTooltip(font, tip, mx, my);
                return;
            }
        }
        if (hoveredSlot != null && hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            ItemStack st = hoveredSlot.getItem();
            List<Component> tip = new ArrayList<>(getTooltipFromContainerItem(st));
            int price = Shop.sellPrice(st);
            if (price > 0) tip.add(Component.literal("ПКМ — продать за " + (long) price * st.getCount() + " ✦").withStyle(ChatFormatting.GOLD));
            g.renderComponentTooltip(font, tip, mx, my);
            return;
        }
        super.renderTooltip(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < ClientState.offers.size() && i < 8; i++)
            if (in(mx, my, 10, 28 + i * 17, 158, 16)) { click(TraderMenu.BTN_BUY0 + i); return true; }
        if (in(mx, my, 176, 2, 92, 14)) { click(TraderMenu.BTN_SELL_JUNK); return true; }
        return super.mouseClicked(mx, my, b);
    }

    @SuppressWarnings("unused") private static final Class<?> S = Slot.class;
}
