package com.pvpciv.client.screen;

import com.pvpciv.client.ui.Skin;
import com.pvpciv.game.Milestones;
import com.pvpciv.net.Network;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * «Дневник подвигов» Тимофея: слева — разделы, справа — график «stonks», который растёт до твоего текущего
 * значения; пунктиром — высоты вех с наградами. Шкала логарифмическая (100 и 25 000 ударов на одном графике).
 */
public class MilestonesScreen extends PlainScreen {
    private final long[] values; private final int[] claimed;
    private int sel = 0;
    private long opened = System.currentTimeMillis();

    public MilestonesScreen(long[] values, int[] claimed) {
        super("Дневник подвигов · Тимофей", 360, 214, false);
        this.values = values; this.claimed = claimed;
        for (int i = 0; i < values.length; i++) if (ready(i) > 0) { sel = i; break; }
    }

    private int ready(int i) {
        Milestones.Track t = Milestones.Track.values()[i];
        int n = 0;
        for (int k = claimed[i]; k < t.steps.length && values[i] >= t.steps[k]; k++) n++;
        return n;
    }

    private static double lg(double v) { return Math.log10(Math.max(1, v)); }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        Milestones.Track[] tr = Milestones.Track.values();
        // разделы
        for (int i = 0; i < tr.length; i++) {
            int x = left + 10, y = top + 22 + i * 30;
            boolean on = i == sel, hov = in(mx, my, 10, 22 + i * 30, 100, 26);
            Skin.inset(g, x, y, 100, 26);
            if (on) g.fill(x + 1, y + 1, x + 99, y + 25, 0x40FFD166); else if (hov) g.fill(x + 1, y + 1, x + 99, y + 25, 0x18FFFFFF);
            g.drawString(font, Skin.fit(font, tr[i].title, 92), x + 5, y + 4, on ? Skin.GOLD_L : Skin.TEXT, false);
            String sub = claimed[i] + "/" + tr[i].steps.length + " вех";
            g.drawString(font, sub, x + 5, y + 15, Skin.FAINT, false);
            int r = ready(i);
            if (r > 0) { g.fill(x + 84, y + 4, x + 96, y + 15, 0xFF2E8B57); g.drawCenteredString(font, "+" + r, x + 90, y + 6, 0xFFFFFFFF); }
        }
        // график
        Milestones.Track t = tr[sel];
        int gx = left + 122, gy = top + 24, gw = 226, gh = 150;
        Skin.inset(g, gx, gy, gw, gh);
        long top_ = t.steps[t.steps.length - 1];
        double max = lg(top_ * 1.15), min = 0;
        java.util.function.DoubleUnaryOperator Y = v -> gy + gh - 6 - (lg(v) - min) / (max - min) * (gh - 14);
        // пунктир вех
        for (int k = 0; k < t.steps.length; k++) {
            int yy = (int) Y.applyAsDouble(t.steps[k]);
            boolean got = k < claimed[sel], can = !got && values[sel] >= t.steps[k];
            int col = got ? 0xFFFFD166 : can ? 0xFF6CE08A : 0xFF6A6A78;
            for (int x = gx + 4; x < gx + gw - 4; x += 6) g.fill(x, yy, x + 3, yy + 1, col);
            String lbl = fmt(t.steps[k]) + " · " + t.bonus + (k == t.steps.length - 1 && t.titleId != null ? " + титул" : "");
            g.drawString(font, lbl, gx + gw - 6 - font.width(lbl), yy - 9, col, false);
        }
        // линия «stonks»: растёт при открытии
        float grow = Math.min(1f, (System.currentTimeMillis() - opened) / 900f);
        double cur = values[sel];
        int n = 40, lastX = gx + 6, lastY = (int) Y.applyAsDouble(1);
        for (int i = 1; i <= n; i++) {
            double f = i / (double) n;
            if (f > grow) break;
            double jitter = Math.sin(i * 1.7 + sel) * 0.08 + Math.sin(i * 0.53) * 0.05;
            double v = Math.pow(10, lg(Math.max(1, cur)) * Math.min(1, Math.max(0, f + (i < n ? jitter : 0))));
            int x = gx + 6 + (int) ((gw - 30) * f), y = (int) Y.applyAsDouble(v);
            line(g, lastX, lastY, x, y, 0xFF3CD070);
            for (int yy = y + 1; yy < gy + gh - 2; yy += 1) g.fill(x - 3, yy, x + 3, yy + 1, 0x183CD070);
            lastX = x; lastY = y;
        }
        if (grow >= 1) {
            g.drawString(font, "↗", lastX + 3, lastY - 6, 0xFF3CD070, true);
            String now = fmt(values[sel]);
            g.drawString(font, now, Math.min(gx + gw - 6 - font.width(now), lastX + 12), lastY - 4, 0xFFFFFFFF, true);
        }
        g.drawString(font, t.title + ": " + fmt(values[sel]), gx, gy + gh + 6, Skin.GOLD_L, false);
        int r = ready(sel);
        Skin.button(g, font, gx + gw - 100, gy + gh + 2, 100, r > 0 ? "Забрать (" + r + ")" : "Нечего забирать", mx, my, r > 0, true);
    }

    private static String fmt(long v) { return v >= 10000 ? (v / 1000) + "K" : String.valueOf(v); }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / Math.max(1, steps), y = y0 + (y1 - y0) * i / Math.max(1, steps);
            g.fill(x, y, x + 2, y + 2, col);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < values.length; i++) if (in(mx, my, 10, 22 + i * 30, 100, 26)) { sel = i; opened = System.currentTimeMillis(); return true; }
        if (in(mx, my, 122 + 226 - 100, 24 + 150 + 2, 100, 16) && ready(sel) > 0) {
            PacketDistributor.sendToServer(new Network.ClaimMilestones(sel));
            Milestones.Track t = Milestones.Track.values()[sel];
            while (claimed[sel] < t.steps.length && values[sel] >= t.steps[claimed[sel]]) claimed[sel]++;
            return true;
        }
        return super.mouseClicked(mx, my, b);
    }
}
