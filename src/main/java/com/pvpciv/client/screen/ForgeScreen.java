package com.pvpciv.client.screen;

import com.pvpciv.client.ui.Skin;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.loot.Rarity;
import com.pvpciv.menu.ForgeMenu;
import com.pvpciv.menu.Stations;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Кузница: наковальня, слоты оружия и звезды, шанс, ритуал заточки, улучшение станции. */
public class ForgeScreen extends CivScreen<ForgeMenu> {
    private int lastProgress, tick;
    private long resultAt = -1;
    private int shownResult;

    public ForgeScreen(ForgeMenu m, Inventory inv, Component t) { super(m, inv, "Кузница", 340, 206); }

    @Override
    protected String hintFor(Slot s) {
        int i = menu.slots.indexOf(s);
        return i == 0 ? "weapon" : i == 1 ? "star" : null;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tick++;
        int p = menu.progress();
        int ax = leftPos + 85, ay = topPos + 58;
        if (p > 0 && tick % 3 == 0) fx.burst(ax, ay, 3, 0xFFB040, 40);
        if (p > 0 && p % 12 == 1 && p != lastProgress) fx.burst(ax, ay, 14, 0xFFE08A, 70);
        if (lastProgress > 0 && p == 0) {
            shownResult = menu.result();
            resultAt = System.currentTimeMillis();
            if (shownResult == 1) { fx.flash(0xFFE08A, 450); fx.burst(ax, ay - 16, 60, Gear.starColor(Gear.stars(menu.box.getItem(0))), 110); }
            else if (shownResult == 3) { fx.flash(0xFF2020, 500); fx.shake(450); fx.burst(ax, ay - 16, 40, 0x9098A8, 90); }
            else if (shownResult == 2) fx.burst(ax, ay - 16, 16, 0xA0A0B0, 50);
        }
        if (p > 0) resultAt = -1;
        lastProgress = p;
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        ItemStack w = menu.box.getItem(0), star = menu.box.getItem(1);
        boolean weapon = w.getItem() instanceof CivWeaponItem;
        int s = Gear.stars(w);
        boolean maxed = weapon && s >= Gear.MAX_STARS;
        boolean ritual = menu.progress() > 0;
        // сцена
        Skin.inset(g, x + 10, y + 24, 152, 70);
        if (weapon) {
            float pulse = ritual ? 0.75f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 90.0) : 0.7f;
            Skin.glow(g, x + 85, y + 41, ritual ? 26 : 20, Skin.alpha(0xFF000000 | Gear.starColor(Math.max(1, s + (ritual ? 1 : 0))), pulse));
        }
        Skin.spr(g, "anvil", x + 57, y + 55, 56, 34);
        g.drawCenteredString(font, "звезда", x + 33, y + 71, Skin.FAINT);
        if (weapon && !maxed) {
            int pc = (int) Math.round(menu.chance() * 100);
            int col = pc >= 70 ? Skin.GOOD : pc >= 40 ? Skin.WARN : Skin.BAD;
            g.pose().pushPose();
            g.pose().translate(x + 135, y + 38, 0);
            g.pose().scale(1.5f, 1.5f, 1);
            g.drawCenteredString(font, pc + "%", 0, 0, col);
            g.pose().popPose();
            g.drawCenteredString(font, "шанс", x + 135, y + 54, Skin.DIM);
        }
        // сведения
        Skin.inset(g, x + 10, y + 98, 152, 52);
        if (weapon) {
            CivWeaponItem cw = (CivWeaponItem) w.getItem();
            Rarity r = Gear.rarity(w).ordinal() < cw.minRarity.ordinal() ? cw.minRarity : Gear.rarity(w);
            g.drawString(font, Skin.fit(font, w.getHoverName().getString(), 140), x + 16, y + 103, 0xFF000000 | r.color, true);
            Skin.stars(g, font, x + 16, y + 114, s, 0xFF000000 | Gear.starColor(Math.max(1, s)));
            if (maxed) g.drawString(font, "Предел: +10", x + 16, y + 126, Skin.BAD, false);
            else {
                double now = CivWeaponItem.damage(cw.tierValue(), cw.cls, r, s), next = CivWeaponItem.damage(cw.tierValue(), cw.cls, r, s + 1);
                String a = String.format("Урон %.1f", now);
                g.drawString(font, a, x + 16, y + 126, Skin.TEXT, false);
                Skin.icon(g, "arrow", x + 18 + font.width(a), y + 125);
                g.drawString(font, String.format("%.1f", next), x + 31 + font.width(a), y + 126, Skin.GOOD, false);
                boolean blessed = star.is(ModRegistry.BLESSED_STAR_W1.get());
                if (star.isEmpty()) g.drawString(font, "Положи звезду заточки", x + 16, y + 138, Skin.DIM, false);
                else if (blessed) g.drawString(font, "При неудаче: заточка −1", x + 16, y + 138, 0xFFC8D0E0, false);
                else {
                    g.fill(x + 13, y + 136, x + 159, y + 148, 0x70C01818);
                    g.drawString(font, "При неудаче: ПОЛОМКА", x + 16, y + 138, 0xFFFF8080, false);
                }
            }
        } else {
            g.drawString(font, "Оружие — на наковальню,", x + 16, y + 106, Skin.DIM, false);
            g.drawString(font, "звезду — в слот слева.", x + 16, y + 117, Skin.DIM, false);
            g.drawString(font, "Обычная звезда может", x + 16, y + 128, Skin.FAINT, false);
            g.drawString(font, "сломать оружие!", x + 16, y + 138, Skin.FAINT, false);
        }
        // действие
        if (ritual) {
            Skin.bar(g, x + 16, y + 156, 140, menu.progress() / (float) ForgeMenu.RITUAL, 0xFFFFE08A, 0xFFE07A20, "amber");
        } else {
            boolean can = weapon && !star.isEmpty() && !maxed;
            Skin.button(g, font, x + 10, y + 154, 152, "Заточить", mx, my, can, true);
        }
        int lvl = menu.stationLevel();
        stationBlock(g, x + 10, y + 173, 152, "Кузница", lvl, Stations.MAX, lvl > 1 ? "+" + (lvl - 1) + "% шанс" : "без бонуса", Stations.moneyCost(Stations.FORGE, lvl), Stations.dustCost(lvl), mx, my);
        g.drawString(font, "Сумка", x + 169, y + 26, Skin.DIM, false);
        drawMoney(g, x + 330, y + 26);
        g.drawString(font, "Быстрые слоты", x + 169, y + 170, Skin.DIM, false);
    }

    @Override
    protected void drawOverlay(GuiGraphics g, int mx, int my) {
        if (resultAt < 0) return;
        long d = System.currentTimeMillis() - resultAt;
        if (d > 2200) return;
        String res = switch (shownResult) { case 1 -> "УСПЕХ!"; case 2 -> "Неудача: −1"; case 3 -> "СЛОМАНО"; default -> ""; };
        int rc = switch (shownResult) { case 1 -> Skin.GOOD; case 3 -> Skin.BAD; default -> 0xFFC8D0E0; };
        float a = d < 1700 ? 1 : 1 - (d - 1700) / 500f;
        float sc = d < 150 ? 1.6f + (150 - d) / 150f : 1.6f;
        if (shownResult == 3) Skin.spr(g, "crack", leftPos + 64, topPos + 64, 44, 13, Skin.alpha(0xFFFFFFFF, a));
        g.pose().pushPose();
        g.pose().translate(leftPos + 86, topPos + 62, 0);
        g.pose().scale(sc, sc, 1);
        g.drawCenteredString(font, res, 0, 0, Skin.alpha(rc, Math.max(0.05f, a)));
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (in(mx, my, 10, 154, 152, 16) && menu.progress() == 0) { click(ForgeMenu.BTN_ENHANCE); return true; }
        int lvl = menu.stationLevel();
        if (lvl < Stations.MAX && stationClick(mx, my, 10, 173, 152)) { click(ForgeMenu.BTN_UPGRADE); return true; }
        return super.mouseClicked(mx, my, b);
    }
}
