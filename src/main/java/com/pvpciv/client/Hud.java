package com.pvpciv.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import com.pvpciv.client.screen.Ornate;

public final class Hud {
    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null || ClientState.zone.isEmpty()) return;
        if (mc.getDebugOverlay().showDebugScreen() || mc.screen instanceof DialogueScreen || mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) return;
        Font font = mc.font;
        long now = mc.level.getGameTime();
        int w = g.guiWidth(), h = g.guiHeight();

        // PvP-рамка
        if (ClientState.zoneType == 2) {
            int a = 0x50 + (int) (0x30 * Math.sin(now * 0.15));
            int c = (a << 24) | 0xFF2020;
            g.fill(0, 0, w, 3, c); g.fill(0, h - 3, w, h, c); g.fill(0, 0, 3, h, c); g.fill(w - 3, 0, w, h, c);
        }

        // панель игрока
        int x = 6, y = 6, pw = 172;
        g.fill(x, y, x + pw, y + 46, 0xA00B0E1A);
        g.fill(x, y, x + 2, y + 46, 0xFFFFB020);
        g.drawString(font, "Ур. " + ClientState.level, x + 8, y + 6, 0xFFD166, true);
        String money = "✦ " + ClientState.money;
        int mc2 = now < ClientState.moneyFlashUntil ? 0xFFFFFF : 0xFFC94A;
        g.drawString(font, money, x + pw - 6 - font.width(money), y + 6, mc2, true);
        int bx = x + 8, by = y + 18, bw = pw - 16;
        g.fill(bx, by, bx + bw, by + 5, 0xFF1C2235);
        float f = ClientState.xpNext > 0 ? Math.min(1f, ClientState.xp / (float) ClientState.xpNext) : 0;
        g.fill(bx, by, bx + (int) (bw * f), by + 5, 0xFF7FDBFF);
        String xpt = ClientState.xp + " / " + ClientState.xpNext + " опыта";
        g.drawString(font, xpt, bx, by + 8, 0x8FA3B8, false);
        int zc = switch (ClientState.zoneType) { case 2 -> 0xFF6060; case 1 -> 0xE8C07A; default -> 0x8FE3A8; };
        String zt = switch (ClientState.zoneType) { case 2 -> " • PvP"; case 1 -> " • PvE"; default -> " • безопасно"; };
        g.drawString(font, ClientState.zone + zt, x + 8, y + 33, zc, false);

        int ny = y + 50;
        if (ClientState.bossSeconds >= 0) {
            int s = ClientState.bossSeconds;
            String bt = s == 0 ? "☠ Громила ждёт гостей в Логове" : String.format("☠ Громила проснётся через %d:%02d", s / 60, s % 60);
            g.fill(x, ny, x + pw, ny + 13, 0x900B0E1A);
            g.drawString(font, bt, x + 6, ny + 3, s <= 60 ? 0xFF6060 : 0xC0A0A0, false);
            ny += 16;
        } else {
            g.fill(x, ny, x + pw, ny + 13, 0xB0400000);
            g.drawString(font, "☠ ГРОМИЛА ПРОСНУЛСЯ!", x + 6, ny + 3, 0xFF5050, true);
            ny += 16;
        }

        // подсказка
        if (ClientState.stage == 0 || now < ClientState.objectiveFlashUntil + 400) {
            List<FormattedCharSequence> lines = font.split(Component.literal(ClientState.objective), pw - 12);
            g.fill(x, ny, x + pw, ny + 6 + lines.size() * 10, 0x800B0E1A);
            int ly = ny + 4;
            for (FormattedCharSequence l : lines) { g.drawString(font, l, x + 6, ly, 0xD8DCE6, false); ly += 10; }
        }

        if (!mc.player.isSpectator()) {
            int cx = w / 2;
            // хотбар
            int sw = 20, gap = 22, hw = gap * 9 + 4;
            int hx = cx - hw / 2, hy = h - 27;
            Ornate.plate(g, hx, hy, hw, 25);
            int sel = mc.player.getInventory().selected;
            for (int i = 0; i < 9; i++) {
                int sx = hx + 3 + i * gap, sy = hy + 3;
                Ornate.slot(g, sx, sy, sw, i == sel);
                net.minecraft.world.item.ItemStack st = mc.player.getInventory().items.get(i);
                if (!st.isEmpty()) {
                    if (st.getItem() instanceof com.pvpciv.item.CivWeaponItem cw) {
                        com.pvpciv.loot.Rarity r = com.pvpciv.loot.Gear.rarity(st).ordinal() < cw.minRarity.ordinal() ? cw.minRarity : com.pvpciv.loot.Gear.rarity(st);
                        g.fillGradient(sx + 1, sy + 12, sx + sw - 1, sy + sw - 1, 0x00000000, (0x70 << 24) | r.color);
                    }
                    g.renderItem(st, sx + 2, sy + 2);
                    g.renderItemDecorations(font, st, sx + 2, sy + 2);
                    int stars = com.pvpciv.loot.Gear.stars(st);
                    if (stars > 0 && st.getItem() instanceof com.pvpciv.item.CivWeaponItem) {
                        g.pose().pushPose(); g.pose().translate(0, 0, 200);
                        g.drawString(font, "+" + stars, sx + 1, sy + 1, 0xFF000000 | com.pvpciv.loot.Gear.starColor(stars), true);
                        g.pose().popPose();
                    }
                }
                g.pose().pushPose(); g.pose().translate(0, 0, 200);
                g.drawString(font, String.valueOf(i + 1), sx + 2, sy + sw - 9, i == sel ? 0xFFFFE08A : 0x60FFFFFF, false);
                g.pose().popPose();
            }
            net.minecraft.world.item.ItemStack off = mc.player.getOffhandItem();
            if (!off.isEmpty()) {
                Ornate.plate(g, hx - 30, hy, 25, 25);
                Ornate.slot(g, hx - 28, hy + 3, sw, false);
                g.renderItem(off, hx - 26, hy + 5);
                g.renderItemDecorations(font, off, hx - 26, hy + 5);
            }
            if (!mc.player.isCreative()) {
                // полоски
                int barW = 92, barY = hy - 14;
                float hp = mc.player.getHealth(), max = mc.player.getMaxHealth();
                Ornate.bar(g, hx + 5, barY, barW, 9, hp / max, hp / max < 0.3f ? 0xD01818 : 0xB82432, 0xFF3048);
                if (mc.player.getAbsorptionAmount() > 0) g.fill(hx + 5, barY + 6, hx + 5 + (int) (barW * Math.min(1, mc.player.getAbsorptionAmount() / max)), barY + 8, 0xFFFFD040);
                g.drawCenteredString(font, (int) Math.ceil(hp) + " / " + (int) max, hx + 5 + barW / 2, barY + 1, 0xFFFFFFFF);
                int food = mc.player.getFoodData().getFoodLevel();
                Ornate.bar(g, hx + hw - 5 - barW, barY, barW, 9, food / 20f, food <= 6 ? 0x9A4A10 : 0xC8841C, 0xFFB030);
                g.drawCenteredString(font, food + " / 20", hx + hw - 5 - barW / 2, barY + 1, 0xFFFFFFFF);
                g.drawString(font, "❤", hx - 6, barY - 10, 0xFFFF5060, true);
                g.drawString(font, "Сытость", hx + hw - font.width("Сытость") + 4, barY - 10, 0xFFE8B060, true);
                int armor = mc.player.getArmorValue();
                if (armor > 0) g.drawCenteredString(font, "Броня " + armor, cx, barY - 10, 0xFFA8C0E0);
            }
            // слот навыка R
            int kx = hx + hw + 6, ky = hy;
            Ornate.plate(g, kx, ky, 25, 25);
            Ornate.slot(g, kx + 2, ky + 3, sw, false);
            g.drawCenteredString(font, "»", kx + 12, ky + 8, 0xFF7FDBFF);
            g.drawString(font, "R", kx + 4, ky + 15, 0xFFFFE08A, true);
            long left = ClientState.dashReadyAt - now;
            if (left > 0) {
                float cf = left / (float) com.pvpciv.game.DashSkill.COOLDOWN_TICKS;
                g.fill(kx + 3, ky + 4 + (int) (18 * (1 - cf)), kx + 21, ky + 22, 0xB0000000);
                g.drawCenteredString(font, String.valueOf((left + 19) / 20), kx + 12, ky + 9, 0xFFFFFFFF);
            }
        }
    }
}
