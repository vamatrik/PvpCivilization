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

    // ------------------------------------------------------------------ 1.1.1: поиск по сумке
    private net.minecraft.client.gui.components.EditBox search;
    private String query = "";

    @Override
    protected void init() {
        super.init();
        // 1.2: поиск в стиле мода — тёмная плашка с золотой рамкой и лупой
        search = new net.minecraft.client.gui.components.EditBox(font, leftPos + 196, topPos + 43, 66, 9, Component.literal("Поиск")) {
            @Override
            public void renderWidget(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
                int x0 = getX() - 12, y0 = getY() - 2, x1 = getX() + width + 2, y1 = getY() + height + 1;
                boolean f = isFocused(), h = isHovered();
                g.fill(x0, y0, x1, y1, f ? 0xFFE8C060 : h ? 0xFFB08A3A : 0xFF7A5E2A);
                g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFF141019);
                g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0x30FFFFFF);
                int c = f ? 0xFFFFE08A : 0xFFB8A070, ix = x0 + 3, iy = getY();   // лупа
                g.fill(ix + 1, iy, ix + 4, iy + 1, c); g.fill(ix + 1, iy + 4, ix + 4, iy + 5, c);
                g.fill(ix, iy + 1, ix + 1, iy + 4, c); g.fill(ix + 4, iy + 1, ix + 5, iy + 4, c);
                g.fill(ix + 4, iy + 5, ix + 5, iy + 6, c); g.fill(ix + 5, iy + 6, ix + 6, iy + 7, c); g.fill(ix + 6, iy + 7, ix + 7, iy + 8, c);
                super.renderWidget(g, mx, my, pt);
            }
        };
        search.setBordered(false);
        search.setTextColor(0xFFF0E6D0);
        search.setMaxLength(24);
        search.setHint(Component.literal("поиск…").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(query);
        search.setResponder(v -> { query = v.trim().toLowerCase(java.util.Locale.ROOT); refreshSearch(); });
        addRenderableWidget(search);
        search.visible = menu.tab() == CivInventoryMenu.TAB_BAG;
    }

    private void refreshSearch() {
        if (query.isEmpty() && filter < 0) {
            menu.setFilter(null);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("bagmap:"));
            return;
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("bagls"));
    }

    /** Ответ сервера: содержимое сумки → индексы под запрос и фильтр. */
    public void onBagList(com.google.gson.JsonObject j) {
        if (query.isEmpty() && filter < 0) return;
        StringBuilder sb = new StringBuilder();
        java.util.List<Integer> idx = new java.util.ArrayList<>();
        for (var e : j.getAsJsonArray("it")) {
            var a = e.getAsJsonArray();
            var it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(a.get(1).getAsString()));
            var st = new net.minecraft.world.item.ItemStack(it);
            if (filter >= 0 && Categories.of(st) != filter) continue;
            if (!query.isEmpty() && !st.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains(query)) continue;
            idx.add(a.get(0).getAsInt());
        }
        for (int i : idx) sb.append(sb.length() > 0 ? "," : "").append(i);
        menu.setFilter(idx.stream().mapToInt(Integer::intValue).toArray());
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("bagmap:" + (idx.isEmpty() ? "-1" : sb)));
    }

    @Override
    public boolean mouseReleased(double mx, double my, int b) {
        boolean r = super.mouseReleased(mx, my, b);
        if (menu.filterMap != null) refreshSearch();   // предмет переложили — выдача поиска обновится
        return r;
    }

    @Override
    public boolean keyPressed(int key, int sc, int mods) {
        if (search != null && search.isFocused()) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { search.setFocused(false); return true; }
            return search.keyPressed(key, sc, mods) || true;   // не закрывать инвентарь буквой E и не жать хоткеи
        }
        return super.keyPressed(key, sc, mods);
    }

    @Override
    protected String hintFor(Slot s) {
        int i = menu.slots.indexOf(s) - menu.armorStart;
        return i >= 0 && i < 5 ? ARMOR_HINTS[i] : null;
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        if (search != null) search.visible = menu.tab() == CivInventoryMenu.TAB_BAG;
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
        if (menu.filterMap != null) g.drawString(font, "найдено: " + menu.filterMap.length, x + 182, y + 184, Skin.GOLD_L, false);
        String[] f = {"Всё", Categories.NAMES[0], Categories.NAMES[1], Categories.NAMES[2], Categories.NAMES[3], Categories.NAMES[4]};
        for (int i = 0; i < f.length; i++) Skin.row(g, font, x + 183, y + 55 + i * 13, 82, f[i], filter == i - 1, mx, my);
        Skin.button(g, font, x + 184, y + 144, 80, "Сортировать", mx, my, true, false);
        g.drawString(font, "Слоты " + menu.unlockedSlots() + (menu.maxScroll() > 0 ? " · колесо ↕" : ""), x + 182, y + 170, Skin.DIM, false);
        int next = 0;
        for (int m : Bag.LEVEL_MILESTONES) if (ClientState.level < m) { next = m; break; }
        if (next > 0 && menu.filterMap == null) g.drawString(font, "+9 на ур. " + next, x + 182, y + 184, Skin.FAINT, false);
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
        // 1.2: «глазки» показа брони
        int mask = minecraft.player == null ? 0 : ClientState.armorHide.getOrDefault(minecraft.player.getId(), 0);
        for (int i = 0; i < 4; i++) {
            int ex = x + 105, ey = y + 50 + i * 22;
            boolean hid = (mask & (1 << i)) != 0, hv = in(mx, my, 105, 50 + i * 22, 7, 8);
            g.fill(ex, ey, ex + 7, ey + 8, hv ? 0xFF8A70B0 : 0xFF2A2236);
            g.fill(ex + 1, ey + 2, ex + 6, ey + 6, hid ? 0xFF3A3048 : 0xFFE8ECF4);
            g.fill(ex + 3, ey + 3, ex + 4, ey + 5, hid ? 0xFF3A3048 : 0xFF202030);
            if (hid) for (int k = 0; k < 7; k++) g.fill(ex + k, ey + 7 - k, ex + k + 1, ey + 8 - k, 0xFFFF6060);
        }
        Skin.inset(g, x + 160, y + 38, 108, 126);
        statBlock(g, x + 166, y + 44, 96);
    }

    private void heroTab(GuiGraphics g, int x, int y, int mx, int my) {
        Skin.inset(g, x + 12, y + 38, 122, 126);
        statBlock(g, x + 18, y + 44, 110);
        Skin.inset(g, x + 138, y + 38, 130, 126);
        g.drawString(font, "Навыки", x + 144, y + 41, Skin.GOLD_L, false);
        skill(g, x + 144, y + 51, key(0), skillName(0), 1);
        skill(g, x + 144, y + 68, key(1), skillName(1), 10);
        skill(g, x + 144, y + 85, key(2), skillName(2), 1);
        skill(g, x + 144, y + 102, key(3), skillName(3), 1);
        if (in(mx, my, 142, 50, 126, 70)) g.drawString(font, "выбрать →", x + 268 - font.width("выбрать →") - 4, y + 41, Skin.GOLD_L, false);
        // 1.2: кнопки листаются (колесо мыши или точки снизу)
        for (int k = 0; k < 6; k++) {
            int idx = btnPage * 6 + k;
            if (idx >= HERO_BTN.length) break;
            Skin.button(g, font, x + 142 + (k % 3) * 42, y + 122 + (k / 3) * 20, 41, HERO_BTN[idx], mx, my, true, false);
        }
        int pages = (HERO_BTN.length + 5) / 6;
        for (int pg = 0; pg < pages; pg++) g.fill(x + 199 + pg * 8, y + 160, x + 204 + pg * 8, y + 162, pg == btnPage ? Skin.GOLD_L : 0xFF4A4058);
    }

    private static final String[] HERO_BTN = {"Титулы", "Трофеи", "Лодки", "Журнал", "Альманах", "О себе", "Настройки"};
    private int btnPage;

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (menu.tab() == CivInventoryMenu.TAB_STATS && in(mx, my, 138, 118, 130, 46)) {
            int pages = (HERO_BTN.length + 5) / 6;
            btnPage = Math.floorMod(btnPage - (int) Math.signum(dy), pages);
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    private void heroButton(String name) {
        switch (name) {
            case "Титулы" -> minecraft.setScreen(new StubScreens.Titles());
            case "Трофеи" -> minecraft.setScreen(new StubScreens.Trophies());
            case "Лодки" -> minecraft.setScreen(new StubScreens.Boats());
            case "Журнал" -> minecraft.setScreen(new StubScreens.Bosses());
            case "Альманах" -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("almanac"));
            case "О себе" -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("about"));
            case "Настройки" -> minecraft.setScreen(new StubScreens.Settings());
            default -> {}
        }
    }

    /** 0.10.5: название навыка в слоте (выбирается в меню «Навыки»). */
    private static String skillName(int i) {
        String id = ClientState.skillSel[i];
        for (var el : ClientState.skillsAll) { var o = el.getAsJsonObject(); if (o.get("id").getAsString().equals(id)) return o.get("name").getAsString(); }
        return "— пусто —";
    }

    private static String key(int i) { return com.pvpciv.client.ClientInit.SKILL_KEYS[i].getTranslatedKeyMessage().getString().toUpperCase(); }

    private void skill(GuiGraphics g, int x, int y, String key, String name, int lvl) {
        boolean open = ClientState.level >= lvl;
        if (open) Skin.slot(g, x + 1, y + 1); else Skin.slotLocked(g, x + 1, y + 1);
        if (open) g.drawCenteredString(font, key, x + 9, y + 5, Skin.GOLD_L);
        g.drawString(font, Skin.fit(font, open ? name : "с уровня " + lvl, 100), x + 23, y + 5, open ? Skin.TEXT : Skin.FAINT, false);   // 1.2: 4 слота — одна строка
    }

    private void statBlock(GuiGraphics g, int x, int y, int w) {
        var p = minecraft.player;
        g.drawString(font, "Уровень " + ClientState.level, x, y, Skin.GOLD_L, true);
        Skin.thinBar(g, x, y + 12, w, 4, ClientState.xpNext > 0 ? ClientState.xp / (float) ClientState.xpNext : 0, 0xFF8FE3FF, 0xFF2A7FB8);
        g.drawString(font, ClientState.xp + " / " + ClientState.xpNext, x, y + 20, Skin.DIM, false);
        Skin.divider(g, x + w / 2, y + 31);
        int ly = y + 42;
        ly = stat(g, x, ly, w, "heart", "Здоровье", com.pvpciv.combat.Balance.fmt(Math.ceil(p.getHealth())) + "/" + com.pvpciv.combat.Balance.fmt(p.getMaxHealth()));
        ly = stat(g, x, ly, w, "arrow", "Урон", com.pvpciv.combat.Balance.fmt(p.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        double def = com.pvpciv.combat.Balance.armor(p, ClientState.level), prot = com.pvpciv.combat.Balance.protection(p);
        ly = stat(g, x, ly, w, "shield", "Броня", com.pvpciv.combat.Balance.fmt(def) + (prot > 0 ? " · " + Math.round(prot * 100) + "%" : ""));
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
        if (menu.tab() != CivInventoryMenu.TAB_BAG || filter < 0 || menu.filterMap != null) return;
        for (Slot s : menu.slots) {
            if (!(s instanceof BagSlot) || !s.isActive() || !s.hasItem()) continue;
            if (Categories.of(s.getItem()) != filter) g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0xD00B0911);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        if (menu.tab() == CivInventoryMenu.TAB_EQUIP) for (int i = 0; i < 4; i++)
            if (in(mx, my, 105, 50 + i * 22, 7, 8)) {
                g.renderComponentTooltip(font, List.of(Component.literal("Показ брони").withStyle(ChatFormatting.GOLD),
                        Component.literal("Скрыть / показать эту часть (только вид — защита остаётся)").withStyle(ChatFormatting.GRAY)), mx, my);
                return;
            }
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
        if (menu.tab() == CivInventoryMenu.TAB_EQUIP && minecraft.player != null) for (int i = 0; i < 4; i++)
            if (in(mx, my, 105, 50 + i * 22, 7, 8)) {   // 1.2: показ брони
                int m = ClientState.armorHide.getOrDefault(minecraft.player.getId(), 0) ^ (1 << i);
                ClientState.armorHide.put(minecraft.player.getId(), m);
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("armvis:" + m));
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
                return true;
            }
        if (menu.tab() == CivInventoryMenu.TAB_BAG) {
            for (int i = 0; i < 6; i++) if (in(mx, my, 183, 55 + i * 13, 82, 13)) { filter = i - 1; refreshSearch(); return true; }
            if (search != null && in(mx, my, 184, 41, 80, 11)) { search.setFocused(true); setFocused(search); return search.mouseClicked(mx, my, b) || true; }
            if (search != null) search.setFocused(false);
            if (in(mx, my, 184, 144, 80, 16)) { click(CivInventoryMenu.BTN_SORT); if (menu.filterMap != null) refreshSearch(); return true; }
        }
        if (menu.tab() == CivInventoryMenu.TAB_STATS) {
            if (in(mx, my, 142, 50, 126, 70)) { minecraft.setScreen(new SkillsScreen()); return true; }   // 0.10.5: меню навыков
            for (int k = 0; k < 6; k++) {
                int idx = btnPage * 6 + k;
                if (idx < HERO_BTN.length && in(mx, my, 142 + (k % 3) * 42, 122 + (k / 3) * 20, 41, 16)) { heroButton(HERO_BTN[idx]); return true; }
            }
            int pages = (HERO_BTN.length + 5) / 6;
            for (int pg = 0; pg < pages; pg++) if (in(mx, my, 197 + pg * 8, 157, 9, 7)) { btnPage = pg; return true; }
        }
        return super.mouseClicked(mx, my, b);
    }
}
