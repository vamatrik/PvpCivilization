package com.pvpciv.client.screen;

import com.pvpciv.client.ClientState;
import com.pvpciv.inv.Bag;
import com.pvpciv.inv.BagSlot;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.menu.Categories;
import com.pvpciv.menu.CivInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CivInventoryScreen extends CivScreen<CivInventoryMenu> {
    private int filter = -1;
    private static final String[] TABS = {"Сумка", "Броня", "Герой"};

    public CivInventoryScreen(CivInventoryMenu m, Inventory inv, Component t) { super(m, inv, Component.literal("Инвентарь"), 258, 194); }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        for (int i = 0; i < 3; i++) CivUi.tab(g, font, x + 92 + i * 55, y + 2, 53, TABS[i], menu.tab() == i, mx, my);
        g.drawString(font, "Быстрые слоты", x + 10, y + 157, CivUi.DIM, false);
        int tab = menu.tab();
        if (tab == CivInventoryMenu.TAB_BAG) {
            CivUi.inset(g, x + 178, y + 26, 72, 128);
            g.drawString(font, "Фильтр", x + 184, y + 30, CivUi.GOLD, false);
            String[] f = {"Всё", Categories.NAMES[0], Categories.NAMES[1], Categories.NAMES[2], Categories.NAMES[3], Categories.NAMES[4]};
            for (int i = 0; i < f.length; i++) CivUi.tab(g, font, x + 182, y + 42 + i * 15, 64, f[i], filter == i - 1, mx, my);
            CivUi.button(g, font, x + 182, y + 134, 64, 14, "Сортировать", mx, my, true, 0x2A6A8A);
            String cnt = "Слоты: " + menu.unlockedSlots() + "/" + Bag.MAX;
            g.drawString(font, cnt, x + 178, y + 168, CivUi.DIM, false);
            int next = 0;
            for (int m : Bag.LEVEL_MILESTONES) if (ClientState.level < m) { next = m; break; }
            if (next > 0) g.drawString(font, "+9 на ур. " + next, x + 178, y + 178, 0xFF5A6478, false);
        } else if (tab == CivInventoryMenu.TAB_EQUIP) {
            CivUi.inset(g, x + 10, y + 26, 78, 128);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 12, y + 28, x + 86, y + 152, 50, 0.0625f, mx, my, minecraft.player);
            g.drawString(font, "Броня", x + 96, y + 24, CivUi.DIM, false);
            g.drawString(font, "Рука", x + 96, y + 110, CivUi.DIM, false);
            g.drawString(font, "Артеф.", x + 128, y + 24, CivUi.DIM, false);
            for (int i = 0; i < 4; i++) {
                boolean open = i == 0 ? ClientState.level >= 1 : ClientState.level >= new int[]{1, 12, 22, 30}[i];
                CivUi.lockedSlot(g, x + 130, y + 34 + i * 20);
                if (!open) continue;
            }
            g.drawString(font, "скоро", x + 152, y + 40, 0xFF5A6478, false);
            statBlock(g, x + 160, y + 60);
        } else {
            statBlock(g, x + 16, y + 30);
            int sx = x + 140, sy = y + 30;
            g.drawString(font, "Навыки", sx, sy, CivUi.GOLD, false);
            skill(g, sx, sy + 12, "R", "Рывок", true);
            skill(g, sx, sy + 34, "F", "откроется на ур. 8", ClientState.level >= 8);
            skill(g, sx, sy + 56, "V", "откроется на ур. 18", ClientState.level >= 18);
            g.drawString(font, "Станции квартиры", sx, sy + 84, CivUi.GOLD, false);
            g.drawString(font, "Кузница, разборочный", sx, sy + 96, CivUi.DIM, false);
            g.drawString(font, "стол — улучшай в них", sx, sy + 106, CivUi.DIM, false);
            g.drawString(font, "Титулы: скоро", sx, sy + 122, 0xFF5A6478, false);
        }
    }

    private void skill(GuiGraphics g, int x, int y, String key, String name, boolean open) {
        g.fill(x, y, x + 18, y + 18, open ? 0xFF7FDBFF : 0xFF2A3040);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF101828);
        g.drawCenteredString(font, key, x + 9, y + 5, open ? 0xFFFFFFFF : 0xFF5A6478);
        g.drawString(font, name, x + 24, y + 5, open ? CivUi.TEXT : 0xFF5A6478, false);
    }

    private void statBlock(GuiGraphics g, int x, int y) {
        var p = minecraft.player;
        g.drawString(font, "Уровень " + ClientState.level, x, y, CivUi.GOLD, true);
        CivUi.bar(g, x, y + 12, 100, 6, ClientState.xpNext > 0 ? ClientState.xp / (float) ClientState.xpNext : 0, 0x3FA8D8);
        g.drawString(font, ClientState.xp + " / " + ClientState.xpNext, x, y + 21, CivUi.DIM, false);
        int ly = y + 36;
        String[] lines = {
                "❤ Здоровье: " + (int) Math.ceil(p.getHealth()) + " / " + (int) p.getMaxHealth(),
                "Урон: " + String.format("%.1f", p.getAttributeValue(Attributes.ATTACK_DAMAGE)),
                "Броня: " + (int) p.getAttributeValue(Attributes.ARMOR),
                "✦ Искры: " + ClientState.money,
                "Бонус урона: +" + (ClientState.level - 1) + "%",
        };
        for (String l : lines) { g.drawString(font, l, x, ly, CivUi.TEXT, false); ly += 12; }
    }

    @Override
    protected void drawOverlay(GuiGraphics g, int mx, int my) {
        if (menu.tab() != CivInventoryMenu.TAB_BAG || filter < 0) return;
        for (Slot s : menu.slots) {
            if (!(s instanceof BagSlot) || !s.isActive() || !s.hasItem()) continue;
            if (Categories.of(s.getItem()) != filter) g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0xC0080A12);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < 3; i++) if (in(mx, my, 92 + i * 55, 2, 53, 14)) { click(i); return true; }
        if (menu.tab() == CivInventoryMenu.TAB_BAG) {
            for (int i = 0; i < 6; i++) if (in(mx, my, 182, 42 + i * 15, 64, 14)) { filter = i - 1; return true; }
            if (in(mx, my, 182, 134, 64, 14)) { click(CivInventoryMenu.BTN_SORT); return true; }
        }
        return super.mouseClicked(mx, my, b);
    }

    @SuppressWarnings("unused") private static final Class<?> W = CivWeaponItem.class;
    @SuppressWarnings("unused") private static final ItemStack E = ItemStack.EMPTY;
}
