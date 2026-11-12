package com.pvpciv.client.screen;

import com.pvpciv.client.ui.Fx;
import com.pvpciv.client.ui.Skin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Основа окон без предметов (титулы, трофеи, справка): панель, лента-заголовок, эффекты. */
public abstract class PlainScreen extends Screen {
    protected final int w, h;
    protected int left, top;
    protected final Fx fx = new Fx();
    protected final boolean backToInventory;
    /** Прокрутка списка экрана (если список длиннее окна). */
    protected final com.pvpciv.client.ui.Scroll scroll = new com.pvpciv.client.ui.Scroll();
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { return scroll.wheel(sy) || super.mouseScrolled(mx, my, sx, sy); }
    /** Строка списка видна (для кликов)? y — экранная координата строки, rh — её высота, [vy, vy+vh) — окно списка. */
    protected static boolean visible(int y, int rh, int vy, int vh) { return y + rh > vy && y < vy + vh; }

    protected PlainScreen(String title, int w, int h, boolean backToInventory) {
        super(Component.literal(title));
        this.w = w; this.h = h; this.backToInventory = backToInventory;
    }

    @Override protected void init() { left = (width - w) / 2; top = (height - h) / 2; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { g.fillGradient(0, 0, width, height, 0xB0050408, 0xD00A0612); }

    protected abstract void drawContent(GuiGraphics g, int mx, int my, float pt);

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        Skin.panel(g, left, top, w, h);
        Skin.header(g, font, left + w / 2, top - 8, title.getString());
        drawContent(g, mx, my, pt);
        fx.render(g, font, width, height);
    }

    protected boolean in(double mx, double my, int x, int y, int bw, int bh) { return mx >= left + x && mx < left + x + bw && my >= top + y && my < top + y + bh; }
    @Override public void onClose() {
        minecraft.setScreen(null);
        if (backToInventory) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pvpciv.net.Network.OpenInventory());
    }
}
