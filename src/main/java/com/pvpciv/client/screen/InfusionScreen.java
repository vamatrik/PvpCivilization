package com.pvpciv.client.screen;

import com.pvpciv.client.ui.Skin;
import com.pvpciv.loot.Gear;
import com.pvpciv.menu.InfusionMenu;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Алтарь инфузии: круг из 8 звёзд вокруг центрального слота, подсказка рецепта и шанс. */
public class InfusionScreen extends CivScreen<InfusionMenu> {
    private float spin;

    public InfusionScreen(InfusionMenu m, Inventory inv, Component t) { super(m, inv, "Алтарь инфузии", 340, 206); }

    private ItemStack center() { return InfusionMenu.at(minecraft.level, menu.altar); }
    private java.util.List<ItemStack> edges() { return menu.edgeItems(minecraft.level); }
    private int ex(int i) { return InfusionMenu.CX + (int) Math.round(Math.cos(i * Math.PI / 4) * InfusionMenu.R); }
    private int ey(int i) { return InfusionMenu.CY + (int) Math.round(Math.sin(i * Math.PI / 4) * InfusionMenu.R); }

    @Override
    protected void drawOverlay(GuiGraphics g, int mx, int my) {
        ItemStack c = center();
        java.util.List<ItemStack> e = edges();
        int x = leftPos, y = topPos;
        if (in(mx, my, InfusionMenu.CX, InfusionMenu.CY, 16, 16) && !c.isEmpty()) g.renderTooltip(font, c, mx, my);
        for (int i = 0; i < 8; i++) if (in(mx, my, ex(i), ey(i), 16, 16) && !e.get(i).isEmpty()) g.renderTooltip(font, e.get(i), mx, my);
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        int x = leftPos, y = topPos;
        spin += pt * 0.02f;
        ItemStack c = center();
        java.util.List<ItemStack> edges = edges();
        InfusionMenu.Kind k = InfusionMenu.kind(c, edges);
        Skin.inset(g, x + 10, y + 22, 152, 128);
        int cx = x + InfusionMenu.CX + 8, cy = y + InfusionMenu.CY + 8;
        Skin.glow(g, cx, cy, 52, k == InfusionMenu.Kind.NONE ? 0x18C0305A : 0x50E0306A);
        // линии от краёв к центру
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            boolean on = !edges.get(i).isEmpty();
            for (int r = 12; r < InfusionMenu.R - 8; r += 3) {
                int px = cx + (int) Math.round(Math.cos(a) * r), py = cy + (int) Math.round(Math.sin(a) * r);
                g.fill(px, py, px + 2, py + 2, on ? 0xC0E0306A : 0x40806070);
            }
        }
        Skin.slot(g, x + InfusionMenu.CX, y + InfusionMenu.CY);
        if (c.isEmpty()) Skin.hint(g, "weapon", x + InfusionMenu.CX, y + InfusionMenu.CY); else g.renderItem(c, x + InfusionMenu.CX, y + InfusionMenu.CY);
        for (int i = 0; i < 8; i++) {
            Skin.slot(g, x + ex(i), y + ey(i));
            if (edges.get(i).isEmpty()) Skin.hint(g, "star", x + ex(i), y + ey(i)); else g.renderItem(edges.get(i), x + ex(i), y + ey(i));
        }
        int ty = y + 127;
        String line;
        int col = Skin.DIM;
        if (k == InfusionMenu.Kind.BLESSED) {
            int s = Gear.stars(c);
            line = "Благословенная звезда • шанс " + Math.round(InfusionMenu.chance(s) * 100) + "%";
            col = Skin.GOLD_L;
        } else if (k == InfusionMenu.Kind.CORE) {
            int b = InfusionMenu.edges(edges, ModRegistry.BLESSED_STAR_W1.get());
            line = "Ядро брони" + (b > 0 ? " " + "✦".repeat(b) : "") + " • 100%";
            col = 0xFF8FE3FF;
        } else if (c.isEmpty()) line = "Положи дар на алтарь";
        else line = "Звёзды — на 8 пьедесталов";
        g.drawCenteredString(font, line, x + 86, ty, col);
        Skin.button(g, font, x + 10, y + 154, 152, "Начать ритуал", mx, my, k != InfusionMenu.Kind.NONE, true);
        Skin.inset(g, x + 10, y + 173, 152, 30);
        g.drawString(font, "Оружие +5…+9 и 8 звёзд → ✦", x + 15, y + 177, Skin.FAINT, false);
        g.drawCenteredString(font, "Забрать — ПКМ рукой", x + 86, y + 139, Skin.FAINT);
        g.drawString(font, "Медальон и 8 звёзд → ядро", x + 15, y + 189, Skin.FAINT, false);
        g.drawString(font, "Сумка", x + 169, y + 26, Skin.DIM, false);
        drawMoney(g, x + 330, y + 26);
        g.drawString(font, "Быстрые слоты", x + 169, y + 170, Skin.DIM, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (in(mx, my, 10, 154, 152, 16) && InfusionMenu.kind(center(), edges()) != InfusionMenu.Kind.NONE) {
            int cx = leftPos + InfusionMenu.CX + 8, cy = topPos + InfusionMenu.CY + 8;
            fx.burst(cx, cy, 40, 0xE0306A, 90); fx.burst(cx, cy, 20, 0xFFE08A, 60);
            click(InfusionMenu.BTN_GO);
            return true;
        }
        return super.mouseClicked(mx, my, b);
    }
}
