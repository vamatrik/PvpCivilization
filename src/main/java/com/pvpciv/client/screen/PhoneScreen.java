package com.pvpciv.client.screen;

import com.google.gson.JsonObject;
import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Skin;
import com.pvpciv.game.FoodApp;
import com.pvpciv.game.Phone;
import com.pvpciv.game.Taxi;
import com.pvpciv.net.Network;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Телефон «Поко X3 Про». Три приложения: «Выкуп», «Шпиль.Еда» (меню, склад, тарифы доставки) и «Шпиль.Такси»
 * (телефон поворачивается горизонтально — схема этажей, точки посадки и высадки).
 */
public class PhoneScreen extends Screen {
    private static final int HOME = 0, FOOD = 1, TAXI = 2;
    private int app;
    private JsonObject data;
    private long dataAt;
    private int left, top, W, H, sx, sy, sw, sh;
    /** Масштаб, если окно игры ниже телефона (720p, крупный GUI): всё рисуется в «виртуальных» пикселях. */
    private float k = 1;
    // еда
    private final int[] cart = new int[FoodApp.MENU.length];
    private int tariff = 0, dest = 0, gateIdx = 0;
    /** Выбранные врата для доставки (только починенные). */
    private String gate() {
        java.util.List<String> l = com.pvpciv.game.Gates.ALL.stream().map(com.pvpciv.game.Gates.Gate::id).filter(ClientState.gatesFixed::contains).toList();
        return l.isEmpty() ? null : l.get(Math.floorMod(gateIdx, l.size()));
    }
    // такси
    private int tFloor = 1, tFrom = -1, tTo = -1, hover = -1;

    public PhoneScreen(int kind, JsonObject j) {
        super(Component.literal("Телефон"));
        data(kind, j);
    }

    public void data(int kind, JsonObject j) {
        if (kind != app) { java.util.Arrays.fill(cart, 0); }
        app = kind; data = j; dataAt = System.currentTimeMillis();
        if (kind == TAXI) {
            tFloor = j.get("floor").getAsInt();
            tFrom = j.get("here").getAsInt(); tTo = -1;
            if (j.has("pf")) { tFrom = j.get("pf").getAsInt(); tTo = j.get("pt").getAsInt(); }
        }
        if (minecraft != null) init();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        boolean land = app == TAXI;
        W = land ? 336 : 162; H = land ? 200 : 268;
        k = Math.min(1f, Math.min((height - 8f) / H, (width - 8f) / W));
        int vw = Math.round(width / k), vh = Math.round(height / k);
        left = (vw - W) / 2; top = (vh - H) / 2;
        if (land) { sx = left + 20; sy = top + 8; sw = W - 40; sh = H - 16; }
        else { sx = left + 7; sy = top + 18; sw = W - 14; sh = H - 36; }
    }

    private int secLeft(String key) {
        if (!data.has(key)) return 0;
        return (int) Math.max(0, data.get(key).getAsLong() - (System.currentTimeMillis() - dataAt) / 1000);
    }

    private static String mmss(int s) { return String.format("%d:%02d", s / 60, s % 60); }

    // ================================================================== отрисовка
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) { g.fillGradient(0, 0, width, height, 0xA0050408, 0xC00A0612); }

    @Override
    public void render(GuiGraphics g, int rmx, int rmy, float pt) {
        renderBackground(g, rmx, rmy, pt);
        int mx = Math.round(rmx / k), my = Math.round(rmy / k);
        g.pose().pushPose();
        g.pose().scale(k, k, 1);
        // корпус
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF3A3A44);
        g.fill(left, top, left + W, top + H, 0xFF101014);
        g.fill(sx, sy, sx + sw, sy + sh, 0xFF141B30);
        if (app == TAXI) { g.fill(left + 8, top + H / 2 - 3, left + 12, top + H / 2 + 3, 0xFF303040); g.fill(left + W - 6, top + H / 2 - 14, left + W - 4, top + H / 2 + 14, 0xFF505060); }
        else { g.fill(left + W / 2 - 3, top + 7, left + W / 2 + 3, top + 11, 0xFF303040); g.fill(left + W / 2 - 14, top + H - 10, left + W / 2 + 14, top + H - 8, 0xFF505060); }
        // строка состояния
        java.time.LocalTime t = java.time.LocalTime.now();
        String time = String.format("%02d:%02d", t.getHour(), t.getMinute());
        g.drawString(font, app == TAXI ? "Шпиль.Такси" : "Поко X3 Про", sx + 4, sy + 3, 0xFFBFC8E0, false);
        g.drawString(font, time, sx + sw - 4 - font.width(time), sy + 3, 0xFFBFC8E0, false);
        Skin.money(g, font, sx + sw - 34, sy + 3, ClientState.money, Skin.GOLD_L);
        g.fill(sx, sy + 13, sx + sw, sy + 14, 0x30FFFFFF);
        switch (app) {
            case FOOD -> food(g, mx, my);
            case TAXI -> taxi(g, mx, my);
            default -> home(g, mx, my);
        }
        g.pose().popPose();
    }

    private static final String[] APPS = {"Выкуп", "Шпиль.Еда", "Шпиль.Такси"};
    private static final String[] DESC = {"Вытащит из подвала Кармиллы", "Еда к двери. Дорого, зато дома", "Схема этажей: откуда и куда"};
    private static final int[] COL = {0xFFB03040, 0xFFE8B820, 0xFF2E9E5A};

    private void home(GuiGraphics g, int mx, int my) {
        g.fillGradient(sx, sy + 14, sx + sw, sy + 70, 0xFF2A3A70, 0xFF141B30);
        for (int i = 0; i < 3; i++) {
            int ay = sy + 22 + i * 56;
            boolean hov = Skin.hover(mx, my, sx + 4, ay, sw - 8, 52);
            g.fill(sx + 4, ay, sx + sw - 4, ay + 52, hov ? 0x40FFFFFF : 0x20FFFFFF);
            g.fill(sx + 8, ay + 6, sx + 40, ay + 38, COL[i]);
            ItemStack ic = new ItemStack(i == 0 ? Items.IRON_BARS : i == 1 ? ModRegistry.DORM_STEW.get() : ModRegistry.NEON_TAXI_T3.get());
            g.pose().pushPose(); g.pose().translate(sx + 12, ay + 10, 0); g.pose().scale(1.5f, 1.5f, 1); g.renderItem(ic, 0, 0); g.pose().popPose();
            int tx = sx + 46, tw = sw - 54;
            g.drawString(font, APPS[i], tx, ay + 4, 0xFFFFFFFF, false);
            Skin.para(g, font, DESC[i], tx, ay + 15, tw, 9, 2, 0xFF9AA4C0, false);
            String st; int sc = Skin.GOLD_L;
            if (i == 0) {
                int cd = secLeft("bailCd");
                boolean jailed = data.get("jailed").getAsBoolean();
                if (!jailed) { st = Phone.BAIL_PER_MIN + " ✦ / мин срока"; sc = 0xFF8A8A9A; }
                else if (cd > 0) { st = "Элеонора остынет: " + (cd / 60 + 1) + " мин"; sc = 0xFF8A8A9A; }
                else st = "Выкупить: " + data.get("bail").getAsInt() + " ✦";
            } else if (i == 1) {
                int f = data.get("food").getAsInt();
                st = f == 1 ? "Курьер в пути" : f == 2 ? "Курьер у двери!" : "Открыть меню";
                if (f == 2) sc = Skin.GOOD;
            } else st = ClientState.taxiUntil > System.currentTimeMillis() ? "Такси ждёт тебя" : "Открыть карту";
            g.drawString(font, Skin.fit(font, st, tw), tx, ay + 39, sc, false);
        }
        g.drawCenteredString(font, "Esc — убрать телефон", sx + sw / 2, sy + sh - 12, 0xFF5A6480);
    }

    // ------------------------------------------------------------------ еда
    private int cartSum() { int s = 0; for (int i = 0; i < cart.length; i++) s += cart[i] * FoodApp.MENU[i].price(); return s; }
    private int cartCount() { int s = 0; for (int c : cart) s += c; return s; }

    private void food(GuiGraphics g, int mx, int my) {
        g.fill(sx, sy + 14, sx + sw, sy + 30, 0xFFE8B820);
        g.drawString(font, "← Шпиль.Еда", sx + 6, sy + 18, Skin.hover(mx, my, sx, sy + 14, 70, 16) ? 0xFF000000 : 0xFF2A1A00, false);
        String rs = "завоз " + mmss(secLeft("restock"));
        g.drawString(font, rs, sx + sw - 6 - font.width(rs), sy + 18, 0xFF5A4000, false);
        int state = data.get("state").getAsInt();
        if (state != 0) {
            int cy = sy + 60;
            g.pose().pushPose(); g.pose().translate(sx + sw / 2f - 16, cy, 0); g.pose().scale(2, 2, 1);
            g.renderItem(new ItemStack(ModRegistry.DORM_STEW.get()), 0, 0); g.pose().popPose();
            String a = state == 1 ? "Курьер в пути. Примерно " + secLeft("eta") + " с до двери."
                    : "Курьер ждёт " + (data.has("where") ? data.get("where").getAsString() : "у двери") + "! Ещё " + mmss(secLeft("wait")) + ".";
            Skin.paraCentered(g, font, a, sx + sw / 2, cy + 42, sw - 16, 10, 3, 0xFFFFFFFF);
            Skin.paraCentered(g, font, "Забирать заказ нужно самому — у двери квартиры. Опоздание бывает: пробки на лифте.", sx + sw / 2, cy + 80, sw - 16, 10, 4, 0xFF9AA4C0);
            back(g, mx, my);
            return;
        }
        if (data.get("apt").getAsInt() < 0) {
            Skin.paraCentered(g, font, "Нет адреса доставки. Сначала заселись в общежитие.", sx + sw / 2, sy + 80, sw - 16, 10, 3, 0xFFFF9090);
            back(g, mx, my);
            return;
        }
        var stock = data.getAsJsonArray("stock");
        for (int i = 0; i < FoodApp.MENU.length; i++) {
            FoodApp.Dish d = FoodApp.MENU[i];
            int ry = sy + 33 + i * 18, left = stock.get(i).getAsInt() - cart[i];
            if (i % 2 == 0) g.fill(sx + 2, ry, sx + sw - 2, ry + 17, 0x14FFFFFF);
            g.renderItem(new ItemStack(d.item().get(), d.count()), sx + 4, ry + 1);
            boolean out = stock.get(i).getAsInt() <= 0;
            g.drawString(font, Skin.fit(font, d.name(), 80), sx + 22, ry + 1, out ? 0xFF6A6A7A : 0xFFFFFFFF, false);
            g.drawString(font, d.price() + " ✦ · " + (out ? "нет" : "ост. " + left), sx + 22, ry + 9, out ? 0xFF6A6A7A : 0xFF9AA4C0, false);
            int bx = sx + sw - 44;
            qbtn(g, bx, ry + 2, "−", cart[i] > 0, mx, my);
            g.drawCenteredString(font, String.valueOf(cart[i]), bx + 21, ry + 4, cart[i] > 0 ? Skin.GOLD_L : 0xFF6A6A7A);
            qbtn(g, bx + 28, ry + 2, "+", left > 0, mx, my);
        }
        int dy = sy + 33 + FoodApp.MENU.length * 18 + 2;
        for (int k = 0; k < 3; k++) {
            int tx = sx + 4 + k * (sw - 8) / 3, tw = (sw - 8) / 3 - 2;
            boolean on = dest == k, hv = Skin.hover(mx, my, tx, dy, tw, 13);
            g.fill(tx, dy, tx + tw, dy + 13, on ? 0xC0E8B820 : hv ? 0x40FFFFFF : 0x20FFFFFF);
            String lab = k == 0 ? "Дверь" : k == 1 ? "Врата" : "Ко мне";
            g.drawCenteredString(font, lab, tx + tw / 2, dy + 3, on ? 0xFF2A1A00 : 0xFFFFFFFF);
        }
        if (dest == 1) {
            String gn = gate() == null ? "нет починенных врат" : com.pvpciv.game.Gates.byId(gate()).name();
            g.drawString(font, Skin.fit(font, "→ " + gn + " (клик — сменить)", sw - 8), sx + 4, dy + 15, 0xFF9AA4C0, false);
        } else g.drawString(font, dest == 0 ? "к двери квартиры, +0 ✦" : "найдёт тебя, +" + FoodApp.DEST_FEE[2] + " ✦ (не в бою и не в PvP)", sx + 4, dy + 15, 0xFF9AA4C0, false);
        int fy = dy + 26;
        for (int t = 0; t < 2; t++) {
            int tx = sx + 4 + t * (sw - 8) / 2, tw = (sw - 8) / 2 - 2;
            boolean on = tariff == t, hv = Skin.hover(mx, my, tx, fy, tw, 22);
            g.fill(tx, fy, tx + tw, fy + 22, on ? 0xC0E8B820 : hv ? 0x40FFFFFF : 0x20FFFFFF);
            g.drawCenteredString(font, t == 0 ? "Обычная" : "Экспресс", tx + tw / 2, fy + 2, on ? 0xFF2A1A00 : 0xFFFFFFFF);
            g.drawCenteredString(font, FoodApp.FEE[t] + " ✦ · ~" + FoodApp.ETA_S[t] + " с", tx + tw / 2, fy + 12, on ? 0xFF3A2A00 : 0xFF9AA4C0);
        }
        int n = cartCount(), total = cartSum() + (n > 0 ? FoodApp.FEE[tariff] + FoodApp.DEST_FEE[dest] : 0);
        boolean ok = n > 0 && ClientState.money >= total && (dest != 1 || gate() != null);
        int by = fy + 25;
        g.drawString(font, n == 0 ? "Корзина пуста" : "Итого: " + total + " ✦", sx + 6, by + 4, n == 0 ? 0xFF6A6A7A : ok ? Skin.GOLD_L : 0xFFFF8080, false);
        boolean hv = ok && Skin.hover(mx, my, sx + sw - 64, by, 60, 16);
        g.fill(sx + sw - 64, by, sx + sw - 4, by + 16, ok ? (hv ? 0xFFFFD040 : 0xFFE8B820) : 0xFF3A3A48);
        g.drawCenteredString(font, "Заказать", sx + sw - 34, by + 4, ok ? 0xFF2A1A00 : 0xFF6A6A7A);
    }

    private void qbtn(GuiGraphics g, int x, int y, String s, boolean on, int mx, int my) {
        boolean hv = on && Skin.hover(mx, my, x, y, 13, 13);
        g.fill(x, y, x + 13, y + 13, on ? (hv ? 0x80FFFFFF : 0x40FFFFFF) : 0x15FFFFFF);
        g.drawCenteredString(font, s, x + 7, y + 3, on ? 0xFFFFFFFF : 0xFF5A5A6A);
    }

    private void back(GuiGraphics g, int mx, int my) {
        g.drawString(font, "← назад", sx + 6, sy + sh - 11, Skin.hover(mx, my, sx + 4, sy + sh - 13, 50, 12) ? 0xFFFFFFFF : 0xFF7A84A0, false);
    }

    // ------------------------------------------------------------------ такси
    private int mapX() { return sx + 4; }
    private int mapY() { return sy + 32; }
    private static final int MW = 196, MH = 142;

    private int[] pt(Taxi.Pt p) {
        if (p.floor() == 1) return new int[]{mapX() + Math.round(p.mx() * MW), mapY() + Math.round(p.my() * MH)};
        int side = MH, ox = mapX() + (MW - side) / 2;
        return new int[]{ox + Math.round(p.mx() * side), mapY() + Math.round(p.my() * side)};
    }

    private void taxi(GuiGraphics g, int mx, int my) {
        boolean f2ok = data.get("f2").getAsBoolean();
        for (int f = 1; f <= 2; f++) {
            int tx = sx + 4 + (f - 1) * 66;
            boolean on = tFloor == f, ok = f == 1 || f2ok, hv = ok && Skin.hover(mx, my, tx, sy + 16, 62, 13);
            g.fill(tx, sy + 16, tx + 62, sy + 29, on ? 0xC02E9E5A : hv ? 0x40FFFFFF : 0x20FFFFFF);
            g.drawCenteredString(font, "Этаж " + f, tx + 31, sy + 19, ok ? 0xFFFFFFFF : 0xFF5A5A6A);
        }
        int hp = data.has("homePrice") ? data.get("homePrice").getAsInt() : -1;
        if (hp >= 0) btn2(g, sx + 136, sy + 16, 64, "Домой " + hp + " ✦", ClientState.money >= hp && ClientState.taxiUntil <= System.currentTimeMillis(), mx, my);
        // схема
        int x0 = mapX(), y0 = mapY();
        g.fill(x0, y0, x0 + MW, y0 + MH, 0xFF0C1222);
        if (tFloor == 1) floor1(g); else floor2(g);
        hover = -1;
        int here = data.get("here").getAsInt();
        for (int i = 0; i < Taxi.PTS.length; i++) {
            Taxi.Pt p = Taxi.PTS[i];
            if (p.floor() != tFloor) continue;
            int[] c = pt(p);
            boolean hv = Math.abs(mx - c[0]) <= 4 && Math.abs(my - c[1]) <= 4;
            if (hv) hover = i;
            int col = i == tFrom ? 0xFF50E070 : i == tTo ? 0xFFFFC030 : p.deep() >= 3 ? 0xFFE05050 : 0xFFD0D8F0;
            int r = hv || i == tFrom || i == tTo ? 3 : 2;
            g.fill(c[0] - r - 1, c[1] - r - 1, c[0] + r + 2, c[1] + r + 2, 0xFF000000);
            g.fill(c[0] - r, c[1] - r, c[0] + r + 1, c[1] + r + 1, col);
            if (i == here) g.drawCenteredString(font, "ты", c[0], c[1] - 12, 0xFF50E070);
            if (hv || i == tFrom || i == tTo) {
                String n = p.name();
                int lx = Math.max(x0 + 2, Math.min(x0 + MW - 2 - font.width(n), c[0] - font.width(n) / 2));
                int ly = c[1] + 6 > y0 + MH - 10 ? c[1] - 14 : c[1] + 6;
                g.fill(lx - 2, ly - 1, lx + font.width(n) + 2, ly + 9, 0xC0000000);
                g.drawString(font, n, lx, ly, col, false);
            }
        }
        g.drawString(font, "1-й клик — откуда, 2-й — куда", x0, y0 + MH + 3, 0xFF5A6480, false);
        // правая колонка
        int rx = x0 + MW + 8, rw = sx + sw - 4 - rx, ry = sy + 18;
        boolean pending = ClientState.taxiUntil > System.currentTimeMillis();
        g.drawString(font, "Откуда", rx, ry, 0xFF50E070, false);
        ry += 10 + Skin.para(g, font, tFrom < 0 ? "выбери точку" : Taxi.PTS[tFrom].name(), rx, ry + 10, rw, 9, 2, tFrom < 0 ? 0xFF6A6A7A : 0xFFFFFFFF, false);
        ry += 4;
        g.drawString(font, "Куда", rx, ry, 0xFFFFC030, false);
        ry += 10 + Skin.para(g, font, tTo < 0 ? "выбери точку" : Taxi.PTS[tTo].name(), rx, ry + 10, rw, 9, 2, tTo < 0 ? 0xFF6A6A7A : 0xFFFFFFFF, false);
        ry += 6;
        int price = Taxi.price(tFrom, tTo);
        if (pending) {
            int s = (int) ((ClientState.taxiUntil - System.currentTimeMillis()) / 1000);
            ry += Skin.para(g, font, (ClientState.taxiHere ? "Такси на месте — садись!" : "Такси ждёт у точки «" + ClientState.taxiFrom + "»") + " " + mmss(s), rx, ry, rw, 9, 4, 0xFF8FE3FF, false) + 4;
            btn(g, rx, ry, rw, "Отменить", true, false, mx, my);
        } else if (price > 0) {
            Taxi.Pt a = Taxi.PTS[tFrom], b = Taxi.PTS[tTo];
            String det = (a.floor() != b.floor() ? "через лифт (+" + Taxi.LIFT_FEE + ") · " : "") + (b.deep() >= 2 ? "в глушь — дороже" : b.deep() == 0 ? "до трассы — дёшево" : "");
            g.drawString(font, price + " ✦", rx, ry, ClientState.money >= price ? Skin.GOLD_L : 0xFFFF8080, false);
            ry += 11;
            if (!det.isEmpty()) ry += Skin.para(g, font, det, rx, ry, rw, 9, 3, 0xFF7A84A0, false) + 3;
            btn(g, rx, ry, rw, "Заказать", ClientState.money >= price, true, mx, my);
            if (tFrom != here) Skin.para(g, font, "Таксист подождёт на точке 3 мин.", rx, ry + 20, rw, 9, 3, 0xFF5A6480, false);
        }
        g.drawString(font, "← назад", rx, sy + sh - 11, Skin.hover(mx, my, rx - 2, sy + sh - 13, 50, 12) ? 0xFFFFFFFF : 0xFF7A84A0, false);
    }

    private void btn2(GuiGraphics g, int x, int y, int w, String s, boolean on, int mx, int my) {
        boolean hv = on && Skin.hover(mx, my, x, y, w, 13);
        g.fill(x, y, x + w, y + 13, !on ? 0xFF3A3A48 : hv ? 0xFF40C070 : 0xFF2E9E5A);
        g.drawCenteredString(font, s, x + w / 2, y + 3, on ? 0xFFFFFFFF : 0xFF6A6A7A);
    }

    private int lastBtnY = -100;
    private void btn(GuiGraphics g, int x, int y, int w, String s, boolean on, boolean main, int mx, int my) {
        lastBtnY = y;
        boolean hv = on && Skin.hover(mx, my, x, y, w, 16);
        g.fill(x, y, x + w, y + 16, !on ? 0xFF3A3A48 : main ? (hv ? 0xFF40C070 : 0xFF2E9E5A) : (hv ? 0xFF805050 : 0xFF603A3A));
        g.drawCenteredString(font, s, x + w / 2, y + 4, on ? 0xFFFFFFFF : 0xFF6A6A7A);
    }

    private void ring(GuiGraphics g, int cx, int cy, int r, int col, int n) {
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }

    private void floor1(GuiGraphics g) {
        int x0 = mapX(), y0 = mapY();
        java.util.function.IntUnaryOperator X = z -> x0 + Math.round(Taxi.f1x(z) * MW);
        java.util.function.IntUnaryOperator Y = x -> y0 + Math.round((0.5f + x / 240f) * MH);
        // Инеевый Кряж (север) и Пепельная пустошь (юг)
        g.fill(X.applyAsInt(-600), Y.applyAsInt(-77), X.applyAsInt(-381), Y.applyAsInt(83), 0xFF1E3550);
        g.drawString(font, "Кряж", X.applyAsInt(-600) + 1, Y.applyAsInt(-77) - 11, 0xFF6FA8D0, false);
        g.fill(X.applyAsInt(359), Y.applyAsInt(-97), X.applyAsInt(689), Y.applyAsInt(103), 0xFF3A2A22);
        g.drawString(font, "Пустошь", X.applyAsInt(689) - font.width("Пустошь") - 1, Y.applyAsInt(-97) - 11, 0xFFC09070, false);
        int cy = Y.applyAsInt(3);
        g.fill(X.applyAsInt(-381), cy - 1, X.applyAsInt(-35), cy + 1, 0xFF7FDBFF);     // Ледяная трасса
        g.fill(X.applyAsInt(39), cy - 1, X.applyAsInt(359), cy + 1, 0xFFB0C0D0);       // Ледяной тракт
        int dx = X.applyAsInt(0);
        g.fill(dx - 6, cy - 9, dx + 7, cy + 10, 0xFF4A4A60);
        g.drawCenteredString(font, "общага", dx, cy - 22, 0xFF9AA4C0);
    }

    private void floor2(GuiGraphics g) {
        int side = MH, ox = mapX() + (MW - side) / 2, oy = mapY(), cx = ox + side / 2, cy = oy + side / 2;
        for (int r = side / 2 - 1; r > side / 2 - 4; r--) ring(g, cx, cy, r, 0xFF2A2F48, 220);
        ring(g, cx, cy, Math.round(120 / 300f * side), 0xFFE040C0, 160);   // неоновое кольцо
        ring(g, cx, cy, Math.round(26 / 300f * side), 0xFF8FE3FF, 40);     // хаб
        g.drawCenteredString(font, "Неоновый город", cx, oy + 2, 0xFF9AA4C0);
    }

    // ================================================================== клики
    @Override
    public boolean mouseClicked(double mxd, double myd, int b) {
        int mx = (int) Math.round(mxd / k), my = (int) Math.round(myd / k);
        switch (app) {
            case HOME -> {
                for (int i = 0; i < 3; i++) if (Skin.hover(mx, my, sx + 4, sy + 22 + i * 56, sw - 8, 52)) {
                    click();
                    if (i == 0) {
                        if (!data.get("jailed").getAsBoolean() || secLeft("bailCd") > 0) return true;
                        cmd("bail"); onClose();
                    } else cmd(i == 1 ? "food" : "taxi");
                    return true;
                }
            }
            case FOOD -> {
                if (Skin.hover(mx, my, sx + 4, sy + sh - 13, 50, 12) || Skin.hover(mx, my, sx, sy + 14, 70, 16)) { click(); cmd("open"); return true; }
                if (data.get("state").getAsInt() != 0 || data.get("apt").getAsInt() < 0) return true;
                var stock = data.getAsJsonArray("stock");
                for (int i = 0; i < FoodApp.MENU.length; i++) {
                    int ry = sy + 33 + i * 18, bx = sx + sw - 44;
                    if (Skin.hover(mx, my, bx, ry + 2, 13, 13) && cart[i] > 0) { cart[i]--; click(); return true; }
                    if (Skin.hover(mx, my, bx + 28, ry + 2, 13, 13) && cart[i] < stock.get(i).getAsInt()) { cart[i]++; click(); return true; }
                }
                int dy = sy + 33 + FoodApp.MENU.length * 18 + 2;
                for (int k = 0; k < 3; k++) if (Skin.hover(mx, my, sx + 4 + k * (sw - 8) / 3, dy, (sw - 8) / 3 - 2, 13)) { dest = k; click(); return true; }
                if (dest == 1 && Skin.hover(mx, my, sx + 4, dy + 14, sw - 8, 10)) { gateIdx++; click(); return true; }
                int fy = dy + 26;
                for (int t = 0; t < 2; t++) if (Skin.hover(mx, my, sx + 4 + t * (sw - 8) / 2, fy, (sw - 8) / 2 - 2, 22)) { tariff = t; click(); return true; }
                int n = cartCount(), total = cartSum() + FoodApp.FEE[tariff] + FoodApp.DEST_FEE[dest];
                if (n > 0 && ClientState.money >= total && (dest != 1 || gate() != null) && Skin.hover(mx, my, sx + sw - 64, fy + 25, 60, 16)) {
                    StringBuilder c = new StringBuilder();
                    for (int i = 0; i < cart.length; i++) if (cart[i] > 0) c.append(i).append('*').append(cart[i]).append(',');
                    cmd("food_order:" + c + ":" + tariff + ":" + (dest == 0 ? "door" : dest == 2 ? "me" : gate()));
                    click();
                    onClose();
                    return true;
                }
            }
            case TAXI -> {
                int rx = mapX() + MW + 8, rw = sx + sw - 4 - rx;
                if (Skin.hover(mx, my, rx - 2, sy + sh - 13, 50, 12)) { click(); cmd("open"); return true; }
                if (Skin.hover(mx, my, sx + 136, sy + 16, 64, 13) && data.has("homePrice") && data.get("homePrice").getAsInt() >= 0) { cmd("taxi_home"); click(); onClose(); return true; }
                for (int f = 1; f <= 2; f++) if (Skin.hover(mx, my, sx + 4 + (f - 1) * 66, sy + 16, 62, 13) && (f == 1 || data.get("f2").getAsBoolean())) { tFloor = f; click(); return true; }
                boolean pending = ClientState.taxiUntil > System.currentTimeMillis();
                if (Skin.hover(mx, my, rx, lastBtnY, rw, 16)) {
                    if (pending) { cmd("taxi_cancel"); click(); return true; }
                    if (Taxi.price(tFrom, tTo) > 0 && ClientState.money >= Taxi.price(tFrom, tTo)) { cmd("taxi_order:" + tFrom + ":" + tTo); click(); onClose(); return true; }
                }
                if (hover >= 0 && !pending) {
                    click();
                    if (tFrom < 0 || (tFrom >= 0 && tTo >= 0)) { tFrom = hover; tTo = -1; }
                    else if (hover == tFrom) tFrom = -1;
                    else tTo = hover;
                    return true;
                }
            }
        }
        return super.mouseClicked(mxd, myd, b);
    }

    private void click() { minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.25f)); }
    private static void cmd(String c) { PacketDistributor.sendToServer(new Network.PhoneCmd(c)); }
}
