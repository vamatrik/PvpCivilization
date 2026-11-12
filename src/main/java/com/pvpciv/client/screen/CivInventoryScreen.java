package com.pvpciv.client.screen;

import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Skin;
import com.pvpciv.inv.Bag;
import com.pvpciv.inv.BagSlot;
import com.pvpciv.menu.Categories;
import com.pvpciv.menu.CivInventoryMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Инвентарь: вкладки «Сумка», «Броня», «Герой». */
public class CivInventoryScreen extends CivScreen<CivInventoryMenu> {
    private int filter = -1;
    private static final String[] TABS = {"Сумка", "Броня", "Герой"};
    private static final String[] ARMOR_HINTS = {"helmet", "chest", "legs", "boots", "shield"};
    private static final int[] ARTIFACT_LEVELS = {1, 12, 22, 30};

    public CivInventoryScreen(CivInventoryMenu m, Inventory inv, Component t) { super(m, inv, "Инвентарь", 278, 206); }

    @Override
    protected String hintFor(Slot s) {
        int i = menu.slots.indexOf(s) - menu.armorStart;
        return i >= 0 && i < 5 ? ARMOR_HINTS[i] : null;
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        for (int i = 0; i < 3; i++) Skin.tab(g, font, x + 12 + i * 60, y + 18, 58, TABS[i], menu.tab() == i, mx, my);
        g.fill(x + 10, y + 32, x + imageWidth - 10, y + 33, Skin.GOLD_D);
        drawMoney(g, x + imageWidth - 14, y + 21);
        g.drawString(font, "Быстрые слоты", x + 13, y + 170, Skin.DIM, false);
        switch (menu.tab()) {
            case CivInventoryMenu.TAB_BAG -> bagTab(g, x, y, mx, my);
            case CivInventoryMenu.TAB_EQUIP -> equipTab(g, x, y, mx, my);
            default -> heroTab(g, x, y, mx, my);
        }
    }

    private void bagTab(GuiGraphics g, int x, int y, int mx, int my) {
        Skin.inset(g, x + 180, y + 38, 88, 126);
        g.drawString(font, "Фильтр", x + 186, y + 43, Skin.GOLD_L, false);
        String[] f = {"Всё", Categories.NAMES[0], Categories.NAMES[1], Categories.NAMES[2], Categories.NAMES[3], Categories.NAMES[4]};
        for (int i = 0; i < f.length; i++) Skin.row(g, font, x + 183, y + 55 + i * 13, 82, f[i], filter == i - 1, mx, my);
        Skin.button(g, font, x + 184, y + 144, 80, "Сортировать", mx, my, true, false);
        g.drawString(font, "Слоты " + menu.unlockedSlots() + "/" + Bag.MAX, x + 182, y + 170, Skin.DIM, false);
        int next = 0;
        for (int m : Bag.LEVEL_MILESTONES) if (ClientState.level < m) { next = m; break; }
        if (next > 0) g.drawString(font, "+9 на ур. " + next, x + 182, y + 184, Skin.FAINT, false);
    }

    private void equipTab(GuiGraphics g, int x, int y, int mx, int my) {
        Skin.inset(g, x + 12, y + 38, 92, 126);
        Skin.spr(g, "podium", x + 22, y + 140, 72, 18, 0x90FFB060);
        Skin.glow(g, x + 58, y + 96, 50, 0x30FFC070);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 14, y + 40, x + 102, y + 158, 48, 0.0625f, mx, my, minecraft.player);
        for (int i = 0; i < 4; i++) {
            int ax = x + 137, ay = y + 45 + i * 22;
            Skin.slotLocked(g, ax, ay);
        }
        Skin.inset(g, x + 160, y + 38, 108, 126);
        statBlock(g, x + 166, y + 44, 96);
    }

    private void heroTab(GuiGraphics g, int x, int y, int mx, int my) {
        Skin.inset(g, x + 12, y + 38, 122, 126);
        statBlock(g, x + 18, y + 44, 110);
        Skin.inset(g, x + 138, y + 38, 130, 126);
        g.drawString(font, "Навыки", x + 144, y + 43, Skin.GOLD_L, false);
        skill(g, x + 144, y + 55, key(0), "Рывок", 1);
        skill(g, x + 144, y + 76, key(1), "Второй навык", 8);
        skill(g, x + 144, y + 97, key(2), "Третий навык", 18);
        Skin.button(g, font, x + 142, y + 122, 41, "Титулы", mx, my, true, false);
        Skin.button(g, font, x + 184, y + 122, 41, "Трофеи", mx, my, true, false);
        Skin.button(g, font, x + 226, y + 122, 41, "Лодки", mx, my, true, false);
        Skin.button(g, font, x + 142, y + 142, 41, "Боссы", mx, my, true, false);
        Skin.button(g, font, x + 184, y + 142, 41, "Броня+", mx, my, true, false);
        Skin.button(g, font, x + 226, y + 142, 41, "Опции", mx, my, true, false);
    }

    private static String key(int i) { return com.pvpciv.client.ClientInit.SKILL_KEYS[i].getTranslatedKeyMessage().getString().toUpperCase(); }

    private void skill(GuiGraphics g, int x, int y, String key, String name, int lvl) {
        boolean open = ClientState.level >= lvl;
        if (open) Skin.slot(g, x + 1, y + 1); else Skin.slotLocked(g, x + 1, y + 1);
        if (open) g.drawCenteredString(font, key, x + 9, y + 5, Skin.GOLD_L);
        g.drawString(font, open ? name : "с уровня " + lvl, x + 23, y + 1, open ? Skin.TEXT : Skin.FAINT, false);
        g.drawString(font, "клавиша " + key, x + 23, y + 10, Skin.FAINT, false);
    }

    private void statBlock(GuiGraphics g, int x, int y, int w) {
        var p = minecraft.player;
        g.drawString(font, "Уровень " + ClientState.level, x, y, Skin.GOLD_L, true);
        Skin.thinBar(g, x, y + 12, w, 4, ClientState.xpNext > 0 ? ClientState.xp / (float) ClientState.xpNext : 0, 0xFF8FE3FF, 0xFF2A7FB8);
        g.drawString(font, ClientState.xp + " / " + ClientState.xpNext, x, y + 20, Skin.DIM, false);
        Skin.divider(g, x + w / 2, y + 31);
        int ly = y + 42;
        ly = stat(g, x, ly, w, "heart", "Здоровье", (int) Math.ceil(p.getHealth()) + "/" + (int) p.getMaxHealth());
        ly = stat(g, x, ly, w, "arrow", "Урон", String.format("%.1f", p.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        ly = stat(g, x, ly, w, "shield", "Броня", String.valueOf((int) p.getAttributeValue(Attributes.ARMOR)));
        ly = stat(g, x, ly, w, "food", "Сытость", p.getFoodData().getFoodLevel() + "/20");
        ly = stat(g, x, ly, w, "spark", "Искры", String.valueOf(ClientState.money));
        g.drawString(font, String.format("Бонус: +%.1f%% урона", 1.5 * (ClientState.level - 1)), x, ly + 2, Skin.FAINT, false);
    }

    private int stat(GuiGraphics g, int x, int y, int w, String icon, String name, String val) {
        Skin.icon(g, icon, x, y - 1);
        g.drawString(font, name, x + 13, y, Skin.DIM, false);
        g.drawString(font, val, x + w - font.width(val), y, Skin.TEXT, false);
        return y + 13;
    }

    @Override
    protected void drawOverlay(GuiGraphics g, int mx, int my) {
        if (menu.tab() != CivInventoryMenu.TAB_BAG || filter < 0) return;
        for (Slot s : menu.slots) {
            if (!(s instanceof BagSlot) || !s.isActive() || !s.hasItem()) continue;
            if (Categories.of(s.getItem()) != filter) g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0xD00B0911);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        if (menu.tab() == CivInventoryMenu.TAB_EQUIP) for (int i = 0; i < 4; i++)
            if (in(mx, my, 136, 44 + i * 22, 18, 18)) {
                g.renderComponentTooltip(font, List.of(Component.literal("Слот артефакта " + (i + 1)).withStyle(ChatFormatting.GOLD),
                        Component.literal("Откроется на уровне " + ARTIFACT_LEVELS[i]).withStyle(ChatFormatting.GRAY),
                        Component.literal("Артефакты появятся в следующих версиях").withStyle(ChatFormatting.DARK_GRAY)), mx, my);
                return;
            }
        super.renderTooltip(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < 3; i++) if (in(mx, my, 12 + i * 60, 18, 58, 14)) { click(i); return true; }
        if (menu.tab() == CivInventoryMenu.TAB_BAG) {
            for (int i = 0; i < 6; i++) if (in(mx, my, 183, 55 + i * 13, 82, 13)) { filter = i - 1; return true; }
            if (in(mx, my, 184, 144, 80, 16)) { click(CivInventoryMenu.BTN_SORT); return true; }
        }
        if (menu.tab() == CivInventoryMenu.TAB_STATS) {
            if (in(mx, my, 142, 122, 41, 16)) { minecraft.setScreen(new StubScreens.Titles()); return true; }
            if (in(mx, my, 184, 122, 41, 16)) { minecraft.setScreen(new StubScreens.Trophies()); return true; }
            if (in(mx, my, 226, 122, 41, 16)) { minecraft.setScreen(new StubScreens.Boats()); return true; }
            if (in(mx, my, 142, 142, 41, 16)) { minecraft.setScreen(new StubScreens.Bosses()); return true; }
            if (in(mx, my, 184, 142, 41, 16)) { minecraft.setScreen(new StubScreens.ArmorBench()); return true; }
            if (in(mx, my, 226, 142, 41, 16)) { minecraft.setScreen(new StubScreens.Settings()); return true; }
        }
        return super.mouseClicked(mx, my, b);
    }
}
