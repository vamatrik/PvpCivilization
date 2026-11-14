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
    /** Гайд всех крафтов (поверх сумки справа), с прокруткой. */
    private boolean guide;
    private final com.pvpciv.client.ui.Scroll gs = new com.pvpciv.client.ui.Scroll();

    private static final String[][] GUIDE = {
            {"#", "Алтарь инфузии"},
            {"Благословенная звезда", "В центр — оружие +4…+9 (сгорает), на 4 пьедестала — обычные звёзды (остальные пустые). Шанс 35% на +4, +15% за каждый плюс, с +9 — 100%."},
            {"Ядро брони +1…+8", "В центр — материал брони: Медальон инея (броня этажа 1), Фильтр противогаза (этажа 2) или Нефритовая печать (этажа 3). На пьедесталы — столько обычных звёзд, до какого плюса нужно ядро: +1 — одна … +8 — восемь. Ядро подходит к броне своего этажа и ниже."},
            {"Ядро брони +9 и +10", "Все 8 пьедесталов, но часть звёзд — благословенные: +9 — 7 обычных и 1 благословенная, +10 — 6 и 2."},
            {"Как класть", "ПКМ предметом по пьедесталу или алтарю. Забрать — ПКМ пустой рукой (алтарь — с Shift)."},
            {"#", "Стол брони (в квартире)"},
            {"Опыт брони", "Вкладывай хлам: монеты, шестерни, реликвии, осколки, ненужную броню +0. «Родной» хлам — с той же локации, где падает броня (иней для инеевой, резина для ликвидатора), — даёт ×1.5."},
            {"Улучшение +1", "Полная полоска опыта + ядро нужного плюса или сильнее (стол берёт самое слабое из подходящих)."},
            {"Свиток закалки", "Дополнительно на +5 и на +10."},
            {"+11", "6 благословенных и 2 кровавые звезды, шанс 50%. Провал — броня откатывается до +5."},
            {"Бонус", "Каждый плюс: +8% брони и здоровья, на +11 — ×2.3."},
            {"#", "Наковальня (заточка оружия)"},
            {"Обычная звезда", "Может сломать оружие при неудаче."},
            {"Благословенная", "При неудаче — заточка −1, оружие цело."},
            {"Звезда крови", "Шанс как у обычной; при неудаче — подвал Кармиллы на 10 минут, оружие цело."},
    };

    public InfusionScreen(InfusionMenu m, Inventory inv, Component t) { super(m, inv, "Алтарь инфузии", 340, 206); }

    private ItemStack center() { return InfusionMenu.at(minecraft.level, menu.altar); }
    private java.util.List<ItemStack> edges() { return menu.edgeItems(minecraft.level); }
    private int ex(int i) { return InfusionMenu.CX + (int) Math.round(Math.cos(i * Math.PI / 4) * InfusionMenu.R); }
    private int ey(int i) { return InfusionMenu.CY + (int) Math.round(Math.sin(i * Math.PI / 4) * InfusionMenu.R); }

    @Override
    protected void drawOverlay(GuiGraphics g, int mx, int my) {
        if (guide) { drawGuide(g); return; }
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
            line = "Ядро брони +" + InfusionMenu.coreLevel(edges) + " • 100%";
            col = 0xFF8FE3FF;
        } else if (c.isEmpty()) line = "Положи дар на алтарь";
        else line = "Звёзды — на пьедесталы";
        g.drawCenteredString(font, line, x + 86, ty, col);
        Skin.button(g, font, x + 10, y + 154, 152, "Начать ритуал", mx, my, k != InfusionMenu.Kind.NONE, true);
        Skin.inset(g, x + 10, y + 173, 152, 30);
        g.drawString(font, "Оружие +4…+9 и 4 звезды → ✦", x + 15, y + 177, Skin.FAINT, false);
        g.drawCenteredString(font, "Забрать — ПКМ рукой", x + 86, y + 139, Skin.FAINT);
        g.drawString(font, "Медальон + N★ → ядро +N", x + 15, y + 189, Skin.FAINT, false);
        Skin.button(g, font, x + 112, y + 25, 48, guide ? "Закрыть" : "Гайд", mx, my, true, guide);
        g.drawString(font, "Сумка", x + 169, y + 26, Skin.DIM, false);
        drawMoney(g, x + 330, y + 26);
        g.drawString(font, "Быстрые слоты", x + 169, y + 170, Skin.DIM, false);
    }

    private void drawGuide(GuiGraphics g) {
        int x = leftPos + 164, y = topPos + 22, w = 170, h = 180;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);   // поверх слотов сумки
        g.fill(x, y, x + w, y + h, 0xF0120C18);
        g.fill(x, y, x + w, y + 1, 0xFFD9A441);
        int tw = w - 12, cy = 4;
        // высота содержимого
        int total = 4;
        for (String[] r : GUIDE) total += r[0].equals("#") ? 16 : 11 + Skin.lines(font, r[1], tw) * 9 + 4;
        gs.view(h - 6).content(total);
        com.pvpciv.client.ui.Scroll.begin(g, x, y + 2, w, h - 4);
        for (String[] r : GUIDE) {
            int ry = y + cy - gs.off;
            if (r[0].equals("#")) {
                g.drawString(font, r[1], x + 6, ry + 4, Skin.GOLD_L, true);
                g.fill(x + 6, ry + 14, x + w - 10, ry + 15, 0x50D9A441);
                cy += 16;
            } else {
                g.drawString(font, r[0], x + 6, ry, 0xFF8FE3FF, false);
                cy += 11 + Skin.para(g, font, r[1], x + 6, ry + 11, tw, 9, 20, Skin.TEXT, false) + 4;
            }
        }
        com.pvpciv.client.ui.Scroll.end(g);
        gs.bar(g, x + w - 5, y + 3, h - 6);
        g.pose().popPose();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (guide && gs.wheel(sy)) return true;
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (in(mx, my, 112, 25, 48, 16)) { guide = !guide; gs.off = 0; return true; }
        if (guide && in(mx, my, 164, 22, 170, 180)) return true;
        if (in(mx, my, 10, 154, 152, 16) && InfusionMenu.kind(center(), edges()) != InfusionMenu.Kind.NONE) {
            int cx = leftPos + InfusionMenu.CX + 8, cy = topPos + InfusionMenu.CY + 8;
            fx.burst(cx, cy, 40, 0xE0306A, 90); fx.burst(cx, cy, 20, 0xFFE08A, 60);
            click(InfusionMenu.BTN_GO);
            return true;
        }
        return super.mouseClicked(mx, my, b);
    }
}
