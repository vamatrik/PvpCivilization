package com.pvpciv.client;

import com.pvpciv.client.ui.Skin;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Игровой HUD: плита игрока, таймер босса, цель, хотбар, полоски, навыки, баннер зоны, PvP-рамка. */
public final class Hud {
    private static final String[] ZONE_GEM = {"green", "amber", "red"};
    private static final String[] ZONE_SUB = {"Безопасная зона", "PvE-зона: монстры", "PvP-зона: игроки могут атаковать!"};
    private static final int[] SKILL_LEVELS = {1, 8, 18};
    private static final String[] SKILL_KEYS = {"R", "F", "V"};

    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) return;
        if (mc.getDebugOverlay().showDebugScreen() || mc.screen instanceof DialogueScreen || mc.screen instanceof AbstractContainerScreen<?>
                || mc.screen instanceof com.pvpciv.client.screen.PlainScreen || mc.screen instanceof com.pvpciv.client.screen.CivDeathScreen) return;
        Font font = mc.font;
        long now = mc.level.getGameTime();
        int w = g.guiWidth(), h = g.guiHeight();

        if (ClientState.zoneType == 2) pvpFrame(g, w, h);
        if (!ClientState.zone.isEmpty()) {
            playerPlate(g, font, now);
            zoneBanner(g, font, w);
        }
        if (!mc.player.isSpectator()) bottom(g, mc, font, now, w, h);
    }

    private static void pvpFrame(GuiGraphics g, int w, int h) {
        int a = 0x38 + (int) (0x20 * Math.sin(System.currentTimeMillis() / 350.0));
        int c = (a << 24) | 0xFF1818;
        g.fillGradient(0, 0, w, 12, c, 0x00FF1818);
        g.fillGradient(0, h - 12, w, h, 0x00FF1818, c);
        for (int i = 0; i < 10; i++) { int ai = (a * (10 - i) / 10) << 24 | 0xFF1818; g.fill(i, 0, i + 1, h, ai); g.fill(w - i - 1, 0, w - i, h, ai); }
    }

    private static void playerPlate(GuiGraphics g, Font font, long now) {
        int x = 5, y = 5, pw = 128;
        Skin.plate(g, x, y, pw, 43);
        g.drawString(font, "Ур. " + ClientState.level, x + 8, y + 6, Skin.GOLD_L, true);
        Skin.money(g, font, x + pw - 8, y + 6, ClientState.money, now < ClientState.moneyFlashUntil ? 0xFFFFFFFF : Skin.GOLD_L);
        float f = ClientState.xpNext > 0 ? ClientState.xp / (float) ClientState.xpNext : 0;
        Skin.thinBar(g, x + 8, y + 18, pw - 16, 3, f, 0xFF8FE3FF, 0xFF2A7FB8);
        String xp = ClientState.xp + "/" + ClientState.xpNext;
        g.pose().pushPose();
        g.pose().translate(x + pw - 8 - font.width(xp) * 0.75f, y + 23.5f, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, xp, 0, 0, Skin.FAINT, false);
        g.pose().popPose();
        Skin.gem(g, ZONE_GEM[Math.min(2, ClientState.zoneType)], x + 5, y + 29);
        g.drawString(font, Skin.fit(font, shortZone(), pw - 26), x + 17, y + 30, ClientState.zoneType == 2 ? 0xFFFF8080 : Skin.TEXT, false);

        int ny = y + 46;
        boolean awake = ClientState.bossSeconds < 0;
        Skin.plate(g, x, ny, pw, 17);
        if (awake) g.fill(x + 2, ny + 2, x + pw - 2, ny + 15, (0x50 + (int) (0x30 * Math.sin(System.currentTimeMillis() / 250.0))) << 24 | 0xC01010);
        Skin.icon(g, "skull", x + 4, ny + 2);
        int s = ClientState.bossSeconds;
        String bt = awake ? "Громила проснулся!" : s == 0 ? "Громила ждёт" : String.format("Громила: %d:%02d", s / 60, s % 60);
        g.drawString(font, bt, x + 19, ny + 5, awake ? 0xFFFF7060 : s <= 60 ? Skin.WARN : Skin.DIM, false);
        ny += 20;

        if (ClientState.stage == 0 || now < ClientState.objectiveFlashUntil + 500) {
            List<FormattedCharSequence> lines = font.split(Component.literal(ClientState.objective), pw - 16);
            Skin.plate(g, x, ny, pw, 10 + lines.size() * 10);
            int ly = ny + 5;
            for (FormattedCharSequence l : lines) { g.drawString(font, l, x + 8, ly, 0xFFD8D2E6, false); ly += 10; }
        }
    }

    private static String shortZone() {
        String z = ClientState.zone;
        return z.contains("· ") ? z.substring(z.lastIndexOf("· ") + 2) : z;
    }

    private static void zoneBanner(GuiGraphics g, Font font, int w) {
        long d = System.currentTimeMillis() - ClientState.zoneChangedAt;
        if (ClientState.zoneChangedAt <= 0 || d > 3200) return;
        float a = d < 300 ? d / 300f : d > 2600 ? 1 - (d - 2600) / 600f : 1;
        if (a < 0.06f) return;
        String name = shortZone();
        int bw = Math.max(120, font.width(name) * 3 / 2 + 60), cx = w / 2, y = 78;
        Skin.spr(g, "header", cx - bw / 2, y, bw, 22, Skin.alpha(0xFFFFFFFF, a));
        g.pose().pushPose();
        g.pose().translate(cx, y + 5, 0);
        g.pose().scale(1.5f, 1.5f, 1);
        g.drawCenteredString(font, name, 0, 0, Skin.alpha(Skin.GOLD_L, a));
        g.pose().popPose();
        int zt = Math.min(2, ClientState.zoneType);
        g.drawCenteredString(font, ZONE_SUB[zt], cx, y + 27, Skin.alpha(zt == 2 ? 0xFFFF7060 : zt == 1 ? 0xFFE8C07A : 0xFF9AE8B0, a));
    }

    private static void bottom(GuiGraphics g, Minecraft mc, Font font, long now, int w, int h) {
        int cx = w / 2, hw = 190, hx = cx - hw / 2, hy = h - 30;
        // хотбар
        Skin.plate(g, hx, hy, hw, 28);
        int sel = mc.player.getInventory().selected;
        for (int i = 0; i < 9; i++) {
            int ix = hx + 7 + i * 20, iy = hy + 6;
            Skin.slot(g, ix, iy);
            ItemStack st = mc.player.getInventory().items.get(i);
            if (st.getItem() instanceof CivWeaponItem cw)
                Skin.rarity(g, ix, iy, (Gear.rarity(st).ordinal() < cw.minRarity.ordinal() ? cw.minRarity : Gear.rarity(st)).color);
            if (i == sel) Skin.spr(g, "slot_selected", ix - 4, iy - 4, 24, 24);
            if (!st.isEmpty()) {
                g.renderItem(st, ix, iy);
                g.renderItemDecorations(font, st, ix, iy);
                int stars = Gear.stars(st);
                if (stars > 0 && st.getItem() instanceof CivWeaponItem) {
                    g.pose().pushPose(); g.pose().translate(0, 0, 200);
                    g.drawString(font, "+" + stars, ix, iy, 0xFF000000 | Gear.starColor(stars), true);
                    g.pose().popPose();
                }
            }
        }
        ItemStack off = mc.player.getOffhandItem();
        if (!off.isEmpty()) {
            Skin.plate(g, hx - 32, hy, 28, 28);
            Skin.slot(g, hx - 26, hy + 6);
            g.renderItem(off, hx - 26, hy + 6);
            g.renderItemDecorations(font, off, hx - 26, hy + 6);
        }
        // навыки
        int kx = hx + hw + 4;
        Skin.plate(g, kx, hy, 70, 28);
        for (int i = 0; i < 3; i++) {
            int ix = kx + 7 + i * 20, iy = hy + 6;
            boolean open = ClientState.level >= SKILL_LEVELS[i];
            if (!open) { Skin.slotLocked(g, ix, iy); continue; }
            Skin.slot(g, ix, iy);
            if (i == 0) {
                g.drawString(font, "»", ix + 5, iy + 1, Skin.ICE, true);
                long left = ClientState.dashReadyAt - now;
                if (left > 0) {
                    float cf = left / (float) com.pvpciv.game.DashSkill.COOLDOWN_TICKS;
                    g.fill(ix, iy + Math.round(16 * (1 - cf)), ix + 16, iy + 16, 0xC0000000);
                    g.drawCenteredString(font, String.valueOf((left + 19) / 20), ix + 8, iy + 4, 0xFFFFFFFF);
                } else Skin.glow(g, ix + 8, iy + 8, 11, 0x307FDBFF);
            }
            g.pose().pushPose();
            g.pose().translate(ix + 11, iy + 10, 200);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, SKILL_KEYS[i], 0, 0, Skin.GOLD_L, true);
            g.pose().popPose();
        }
        if (mc.player.isCreative()) return;
        // полоски
        int bw = 84, by = hy - 15;
        float hp = mc.player.getHealth(), max = mc.player.getMaxHealth();
        boolean low = hp / max < 0.3f;
        if (low) Skin.glow(g, hx + 6 + bw / 2, by + 6, 30, (0x30 + (int) (0x28 * Math.sin(System.currentTimeMillis() / 150.0))) << 24 | 0xFF2020);
        Skin.bar(g, hx + 8, by, bw, hp / max, low ? 0xFFFF5050 : 0xFFE84858, low ? 0xFF8A1010 : 0xFF8A1828, "red");
        float abs = mc.player.getAbsorptionAmount();
        if (abs > 0) g.fill(hx + 11, by + 8, hx + 11 + Math.round((bw - 6) * Math.min(1, abs / max)), by + 10, 0xFFFFD040);
        g.drawCenteredString(font, (int) Math.ceil(hp) + "/" + (int) max, hx + 8 + bw / 2, by + 2, 0xFFFFFFFF);
        int food = mc.player.getFoodData().getFoodLevel();
        Skin.bar(g, hx + hw - 8 - bw, by, bw, food / 20f, food <= 6 ? 0xFFC87020 : 0xFFF0B040, food <= 6 ? 0xFF6A3008 : 0xFF9A5A10, "amber");
        g.drawCenteredString(font, food + "/20", hx + hw - 8 - bw / 2, by + 2, 0xFFFFFFFF);
        int armor = mc.player.getArmorValue();
        if (armor > 0) {
            Skin.icon(g, "shield", hx + 4, by - 11);
            g.drawString(font, String.valueOf(armor), hx + 16, by - 10, 0xFFA8C0E0, true);
        }
        int air = mc.player.getAirSupply(), maxAir = mc.player.getMaxAirSupply();
        if (air < maxAir) Skin.thinBar(g, hx + hw - 8 - bw, by - 6, bw, 2, Math.max(0, air) / (float) maxAir, 0xFFBFEFFF, 0xFF4090D0);
    }

    /** Своя полоса босса (вызывается из события; возвращает занятую высоту). */
    public static int bossBar(GuiGraphics g, LerpingBossEvent ev, int x, int y) {
        Font font = Minecraft.getInstance().font;
        float p = ev.getProgress();
        int bw = 150, bx = x + 91 - bw / 2;
        boolean rage = p < 0.5f;
        if (rage) Skin.glow(g, x + 91, y + 7, 60, (0x20 + (int) (0x18 * Math.sin(System.currentTimeMillis() / 180.0))) << 24 | 0xFF3010);
        g.drawCenteredString(font, ev.getName(), x + 91, y - 9, 0xFFFFFFFF);
        Skin.bar(g, bx, y, bw, p, rage ? 0xFFFF6030 : 0xFFE03040, rage ? 0xFF7A1408 : 0xFF6A0C18, null);
        if (rage) Skin.spr(g, "crack", bx + 12, y, 40, 12, 0xB0FFFFFF);
        if (p < 0.25f) Skin.spr(g, "crack", bx + 92, y, 40, 12, 0xB0FFFFFF);
        Skin.icon(g, "skull", bx - 5, y); Skin.icon(g, "skull", bx + bw - 7, y);
        return 28;
    }
}
