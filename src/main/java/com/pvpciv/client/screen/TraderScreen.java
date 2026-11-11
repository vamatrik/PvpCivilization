package com.pvpciv.client.screen;

import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Skin;
import com.pvpciv.game.Shop;
import com.pvpciv.menu.TraderMenu;
import com.pvpciv.net.Network;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Лавка Сороки: витрина слева, сумка справа, ПКМ по предмету — продать. */
public class TraderScreen extends CivScreen<TraderMenu> {
    private static final int ROW = 19, ROWS = 8;

    public TraderScreen(TraderMenu m, Inventory inv, Component t) { super(m, inv, t.getString().equals("Контрабандист") ? "Контрабандист Шнырь" : t.getString().equals("Элеонора") ? "Лавка охотницы Элеоноры" : "Лавка Сороки", 352, 206); }

    private static boolean tokens() { return ClientState.shopTokens >= 0; }
    private static long balance() { return tokens() ? ClientState.shopTokens : ClientState.money; }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        Skin.inset(g, x + 10, y + 24, 164, 157);
        List<Network.OfferView> offers = ClientState.offers;
        for (int i = 0; i < offers.size() && i < ROWS; i++) {
            Network.OfferView o = offers.get(i);
            int oy = y + 27 + i * ROW;
            boolean sold = o.left() <= 0, afford = balance() >= o.price();
            boolean hv = in(mx, my, 12, 27 + i * ROW, 160, ROW - 1);
            if (hv && !sold && afford) { g.fill(x + 12, oy, x + 172, oy + 18, 0x40D9A441); g.fill(x + 12, oy, x + 14, oy + 18, Skin.GOLD); }
            else if (i % 2 == 0) g.fill(x + 12, oy, x + 172, oy + 18, 0x18FFFFFF);
            g.renderItem(o.stack(), x + 16, oy + 1);
            g.renderItemDecorations(font, o.stack(), x + 16, oy + 1);
            // строка 1 — название целиком (132 px), строка 2 — остаток и цена
            g.drawString(font, Skin.fit(font, o.stack().getHoverName().getString(), 132), x + 36, oy + 1, sold ? Skin.FAINT : Skin.TEXT, false);
            if (sold) g.drawString(font, "распродано", x + 170 - font.width("распродано"), oy + 10, Skin.FAINT, false);
            else {
                if (tokens()) { String pr = o.price() + " жет."; g.drawString(font, pr, x + 169 - font.width(pr), oy + 10, afford ? 0xFFD8E4F0 : 0xFFB06A5A, true); }
                else Skin.money(g, font, x + 169, oy + 10, o.price(), afford ? Skin.GOLD_L : 0xFFB06A5A);
                String lim = o.left() >= 999 ? "без лимита" : "осталось " + o.left();
                g.drawString(font, lim, x + 36, oy + 10, Skin.DIM, false);
            }
        }
        if (offers.isEmpty()) g.drawCenteredString(font, "Товар раскладывают...", x + 92, y + 90, Skin.DIM);
        long ms = Math.max(0, ClientState.shopRotationAt - System.currentTimeMillis());
        if (tokens()) {
            long hl = Math.max(0, ClientState.shopRotationAt - System.currentTimeMillis());
            Skin.icon(g, "clock", x + 12, y + 187);
            g.drawString(font, String.format("завоз через %d:%02d", hl / 3600000, (hl / 60000) % 60), x + 25, y + 188, Skin.DIM, false);
            g.drawString(font, "Сумка", x + 181, y + 26, Skin.DIM, false);
            String b = "Жетоны: " + ClientState.shopTokens;
            g.drawString(font, b, x + 342 - font.width(b), y + 26, 0xFFD8E4F0, true);
        } else {
            Skin.button(g, font, x + 10, y + 184, 84, "Продать хлам", mx, my, true, false);
            Skin.icon(g, "clock", x + 100, y + 187);
            g.drawString(font, String.format("завоз %d:%02d", ms / 3600000, (ms / 60000) % 60), x + 113, y + 188, Skin.DIM, false);
            g.drawString(font, "Сумка · ПКМ — продать", x + 181, y + 26, Skin.DIM, false);
            drawMoney(g, x + 342, y + 26);
        }
        g.drawString(font, "Быстрые слоты", x + 181, y + 170, Skin.DIM, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        List<Network.OfferView> offers = ClientState.offers;
        for (int i = 0; i < offers.size() && i < ROWS; i++) {
            if (in(mx, my, 12, 27 + i * ROW, 160, ROW - 1)) {
                Network.OfferView o = offers.get(i);
                List<Component> tip = new ArrayList<>(getTooltipFromContainerItem(o.stack()));
                tip.add(o.left() <= 0 ? Component.literal(tokens() ? "Раскуплено до завоза (полночь)" : "Распродано до следующего завоза").withStyle(ChatFormatting.DARK_GRAY)
                        : Component.literal("ЛКМ — купить за " + o.price() + (tokens() ? " жет." : " ✦")).withStyle(ChatFormatting.GOLD));
                g.renderTooltip(font, tip, o.stack().getTooltipImage(), o.stack(), mx, my);
                return;
            }
        }
        if (hoveredSlot != null && hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            ItemStack st = hoveredSlot.getItem();
            List<Component> tip = new ArrayList<>(getTooltipFromContainerItem(st));
            int price = tokens() ? 0 : Shop.sellPrice(st);
            tip.add(price > 0 ? Component.literal("ПКМ — продать за " + (long) price * st.getCount() + " ✦").withStyle(ChatFormatting.GOLD)
                    : Component.literal("Это здесь не купят").withStyle(ChatFormatting.DARK_GRAY));
            g.renderTooltip(font, tip, st.getTooltipImage(), st, mx, my);
            return;
        }
        super.renderTooltip(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < ClientState.offers.size() && i < ROWS; i++)
            if (in(mx, my, 12, 27 + i * ROW, 160, ROW - 1)) { click(TraderMenu.BTN_BUY0 + i); return true; }
        if (!tokens() && in(mx, my, 10, 184, 84, 16)) { click(TraderMenu.BTN_SELL_JUNK); return true; }
        return super.mouseClicked(mx, my, b);
    }
}
