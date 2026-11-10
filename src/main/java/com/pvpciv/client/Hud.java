package com.pvpciv.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

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

        // свои полоски здоровья и сытости
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            int cx = w / 2, barY = h - 32;
            float hp = mc.player.getHealth(), max = mc.player.getMaxHealth();
            com.pvpciv.client.screen.CivUi.bar(g, cx - 91, barY, 88, 8, hp / max, hp / max < 0.3f ? 0xC02828 : 0xB8303C);
            if (mc.player.getAbsorptionAmount() > 0) g.fill(cx - 90, barY + 1, cx - 90 + (int) (86 * Math.min(1, mc.player.getAbsorptionAmount() / max)), barY + 3, 0xFFFFD040);
            String hs = "❤ " + (int) Math.ceil(hp) + " / " + (int) max;
            g.drawCenteredString(font, hs, cx - 47, barY - 9, 0xFFFF8A8A);
            int food = mc.player.getFoodData().getFoodLevel();
            com.pvpciv.client.screen.CivUi.bar(g, cx + 3, barY, 88, 8, food / 20f, food <= 6 ? 0x8A5A1A : 0xC88A2A);
            g.drawCenteredString(font, "Сытость " + food + " / 20", cx + 47, barY - 9, food <= 6 ? 0xFFFF8040 : 0xFFE8C07A);
            int armor = mc.player.getArmorValue();
            if (armor > 0) g.drawString(font, "Броня " + armor, cx - 91, barY - 19, 0xFFA8C0E0, true);
        }

        // слот навыка R
        int sx = w / 2 + 100, sy = h - 24;
        g.fill(sx - 1, sy - 1, sx + 23, sy + 23, 0xFF7FDBFF);
        g.fill(sx, sy, sx + 22, sy + 22, 0xE0101828);
        g.drawString(font, "»", sx + 8, sy + 4, 0x7FDBFF, true);
        g.drawString(font, "R", sx + 2, sy + 13, 0xFFFFFF, true);
        long left = ClientState.dashReadyAt - now;
        if (left > 0) {
            float cf = left / (float) com.pvpciv.game.DashSkill.COOLDOWN_TICKS;
            g.fill(sx, sy + (int) (22 * (1 - cf)), sx + 22, sy + 22, 0xB0000000);
            g.drawCenteredString(font, String.valueOf((left + 19) / 20), sx + 11, sy + 7, 0xFFFFFF);
        }
        g.drawString(font, "Рывок", sx + 26, sy + 7, 0xAFC6D8, true);
    }
}
