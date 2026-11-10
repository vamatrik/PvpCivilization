package com.pvpciv.client.screen;

import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Skin;
import com.pvpciv.net.Network;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Экран смерти: что потеряно и кнопка «Проснуться». */
public class CivDeathScreen extends Screen {
    private int ticks;

    public CivDeathScreen() { super(Component.literal("Ты пал")); }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        ticks++;
        if (minecraft.player != null && minecraft.player.isAlive() && minecraft.player.getHealth() > 0) minecraft.setScreen(null);
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float in = Math.min(1, (ticks + pt) / 25f);
        g.fillGradient(0, 0, width, height, Skin.alpha(0xD0300408, in), Skin.alpha(0xF0050208, in));
        int cx = width / 2, cy = height / 2;
        Skin.glow(g, cx, cy - 46, 110, Skin.alpha(0x50FF2020, in));
        g.pose().pushPose();
        g.pose().translate(cx, cy - 62, 0);
        g.pose().scale(3, 3, 1);
        g.drawCenteredString(font, "ТЫ ПАЛ", 0, 0, Skin.alpha(0xFFFF5A4A, Math.max(0.05f, in)));
        g.pose().popPose();
        Network.DeathInfo d = ClientState.death;
        String sub = d != null && d.pvp() ? "Тебя одолел " + d.killer() : d != null && !d.killer().isEmpty() ? "Тебя убил: " + d.killer() : "Бездна забрала своё";
        g.drawCenteredString(font, sub, cx, cy - 30, Skin.alpha(0xFFD8C8C8, Math.max(0.05f, in)));
        if (ticks < 12) return;
        int pw = 236, px = cx - pw / 2, py = cy - 14;
        Skin.panel(g, px, py, pw, 62);
        g.drawCenteredString(font, "Ты проснёшься в своей кровати, но...", cx, py + 9, Skin.DIM);
        Skin.divider(g, cx, py + 20);
        long money = d == null ? 0 : d.lostMoney(), xp = d == null ? 0 : d.lostXp();
        String m = d == null ? "—" : "−" + money + (d != null && d.pvp() ? " (выпали)" : " (сгорели)");
        Skin.icon(g, "spark", px + 22, py + 31);
        g.drawString(font, "Искры", px + 35, py + 32, Skin.TEXT, false);
        g.drawString(font, m, px + pw - 20 - font.width(m), py + 32, money > 0 ? Skin.BAD : Skin.DIM, false);
        Skin.gem(g, "blue", px + 22, py + 43);
        g.drawString(font, "Опыт до уровня", px + 35, py + 44, Skin.TEXT, false);
        String x = d == null ? "—" : "−" + xp;
        g.drawString(font, x, px + pw - 20 - font.width(x), py + 44, xp > 0 ? Skin.BAD : Skin.DIM, false);
        Skin.button(g, font, cx - 60, py + 70, 120, "Проснуться", mx, my, ticks > 20, true);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        int cx = width / 2, py = height / 2 - 14;
        if (ticks > 20 && Skin.hover((int) mx, (int) my, cx - 60, py + 70, 120, 16) && minecraft.player != null) {
            minecraft.player.respawn();
            return true;
        }
        return super.mouseClicked(mx, my, b);
    }
}
