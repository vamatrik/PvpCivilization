package com.pvpciv.client.screen;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pvpciv.client.ClientState;
import com.pvpciv.client.ui.Fx;
import com.pvpciv.client.ui.Skin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.util.*;

/**
 * 1.3 — Древо классов в HD: звёздное полотно с орбитами, медальоны-узлы (свои спрайты ×4),
 * светящиеся связи с бегущей «энергией», масштаб колесом, перетаскивание, подсказка у курсора.
 * Изучение — двойной щелчок по узлу (первый — «взвести», второй — подтвердить).
 */
public class ClassScreen extends Screen {
    // ---- геометрия древа (в «единицах древа»)
    private static final double R_CLASS = 58, R_CHAIN1 = 92, R_CHAIN2 = 122, R_SKILL = 154, R_MAX = 178;
    private static final double A_CHAIN1 = 13, A_CHAIN2 = 10;
    private static final String[] PRE = {"w", "s", "m", "r", "a", "g"};   // порядок = PClass.values()
    private static final String[] CLASS_ICON = {"sword", "spellblade", "mage", "archer", "daggers", "hammer"};

    // ---- состояние, переживающее пересоздание экрана после синхронизации
    private static float zoom = -1, panX, panY;
    private static String armed;
    private static long armedAt;
    private static final Set<String> SEEN = new HashSet<>();

    private int L, T, W, H;          // окно
    private int cx0, cy0, cw, ch;    // полотно
    private final Fx fx = new Fx();
    private boolean dragging, pressed;
    private double pressX, pressY;
    private String hover;

    public ClassScreen() { super(Component.literal("Древо классов")); }

    @Override public boolean isPauseScreen() { return false; }

    private static long lastShown;

    @Override
    protected void init() {
        if (System.currentTimeMillis() - lastShown > 3000) { zoom = -1; panX = panY = 0; }   // новое открытие — вид по центру
        W = Math.min(width - 20, 560); H = Math.min(height - 26, 320);
        L = (width - W) / 2; T = (height - H) / 2 + 4;
        cx0 = L + 10; cy0 = T + 12; cw = W - 20; ch = H - 22;
        float fit = (float) (Math.min(cw, ch) / (2 * R_MAX));
        if (zoom < 0) zoom = fit;
        zoom = Math.max(fit * 0.8f, Math.min(fit * 3.2f, zoom));
        // вспышка на только что изученных узлах
        for (JsonObject n : nodes()) {
            String id = n.get("id").getAsString();
            if (n.get("have").getAsBoolean() && SEEN.add(id) && !first) {
                double[] p = pos().get(id);
                if (p != null) { int[] s = toScreen(p); fx.burst(s[0], s[1], 26, color(n), 1.6f); }
            }
        }
        first = false;
    }
    private static boolean first = true;

    // ------------------------------------------------------------------ данные
    private JsonObject j() { return ClientState.classes; }
    private List<JsonObject> list(String key) {
        List<JsonObject> l = new ArrayList<>();
        if (j() != null && j().has(key)) for (var el : j().getAsJsonArray(key)) l.add(el.getAsJsonObject());
        return l;
    }
    private List<JsonObject> nodes() { return list("nodes"); }
    private JsonObject node(String id) { for (JsonObject o : nodes()) if (o.get("id").getAsString().equals(id)) return o; return null; }
    private JsonObject cls(String id) { for (JsonObject o : list("classes")) if (o.get("id").getAsString().equals(id)) return o; return null; }
    private int dm() { return j() == null ? 0 : j().get("dm").getAsInt(); }
    private boolean have(String id) { JsonObject n = node(id); return n != null && n.get("have").getAsBoolean(); }
    private boolean can(JsonObject n) { return !n.get("have").getAsBoolean() && n.get("why").getAsString().isEmpty() && dm() >= n.get("cost").getAsInt(); }
    private boolean reachable(JsonObject n) { return !n.get("have").getAsBoolean() && n.get("why").getAsString().isEmpty(); }
    private int color(JsonObject n) {
        String c = n == null ? "" : n.get("cls").getAsString();
        JsonObject k = c.isEmpty() ? null : cls(c);
        return k == null ? 0xFFC9A0FF : 0xFF000000 | k.get("color").getAsInt();
    }

    /** Позиции узлов (единицы древа, центр — 0,0). */
    private Map<String, double[]> pos() {
        Map<String, double[]> m = new LinkedHashMap<>();
        m.put("tp", new double[]{0, 0});
        for (int i = 0; i < 6; i++) {
            double th = Math.toRadians(-90 + i * 60);
            String p = PRE[i];
            m.put(p + "_base", polar(th, 0, R_CLASS));
            m.put(p + "_a1", polar(th, -A_CHAIN1, R_CHAIN1));
            m.put(p + "_a2", polar(th, -A_CHAIN2, R_CHAIN2));
            m.put(p + "_b1", polar(th, A_CHAIN1, R_CHAIN1));
            m.put(p + "_b2", polar(th, A_CHAIN2, R_CHAIN2));
            m.put(p + "_skill", polar(th, 0, R_SKILL));
            m.put("#" + i, polar(th, 0, R_CLASS));
        }
        return m;
    }
    private static double[] polar(double th, double offDeg, double r) { double a = th + Math.toRadians(offDeg); return new double[]{Math.cos(a) * r, Math.sin(a) * r}; }

    private int[] toScreen(double[] p) { return new int[]{(int) Math.round(cx0 + cw / 2.0 + panX + p[0] * zoom), (int) Math.round(cy0 + ch / 2.0 + panY + p[1] * zoom)}; }
    private double sx(double x) { return cx0 + cw / 2.0 + panX + x * zoom; }
    private double sy(double y) { return cy0 + ch / 2.0 + panY + y * zoom; }

    private static double size(String kind) { return switch (kind) { case "CENTER" -> 50; case "BASE" -> 36; case "SKILL" -> 34; default -> 25; }; }

    private static String icon(String id, JsonObject n) {
        String kind = n.get("kind").getAsString();
        if (kind.equals("CENTER")) return "portal";
        if (kind.equals("SKILL")) return id.startsWith("g") ? "bastion" : id.startsWith("w") ? "rage" : "retreat";
        if (kind.equals("BASE")) { for (int i = 0; i < 6; i++) if (id.startsWith(PRE[i] + "_")) return CLASS_ICON[i]; }
        String d = n.get("desc").getAsString();   // стат — по первому стату в описании
        String[][] map = {{"здоровья", "hp"}, {"защиты", "def"}, {"реген", "regen"}, {"скорости атаки", "aspd"}, {"скорости бега", "mspd"},
                {"крит-урона", "critdmg"}, {"шанса крита", "crit"}, {"уклонения", "dodge"}, {"вампиризма", "lifesteal"}, {"урона", "dmg"}};
        int best = Integer.MAX_VALUE; String ic = "dmg";
        for (String[] e : map) { int k = d.indexOf(e[0]); if (k >= 0 && k < best) { best = k; ic = e[1]; } }
        return ic;
    }

    // ------------------------------------------------------------------ отрисовка
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) { g.fillGradient(0, 0, width, height, 0xC0050408, 0xE00A0612); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        lastShown = System.currentTimeMillis();
        renderBackground(g, mx, my, pt);
        Skin.panel(g, L - 4, T - 4, W + 8, H + 8);
        Skin.header(g, font, L + W / 2, T - 12, "Древо классов");
        if (j() == null) return;
        long now = System.currentTimeMillis();
        if (armed != null && now - armedAt > 3500) armed = null;

        g.enableScissor(cx0, cy0, cx0 + cw, cy0 + ch);
        drawSky(g, now);
        Map<String, double[]> P = pos();
        drawLinks(g, P, now);
        hover = null;
        // узлы: сначала «скоро», потом настоящие
        List<JsonObject> cl = list("classes");
        for (int i = 0; i < 6 && i < cl.size(); i++) if (!cl.get(i).get("open").getAsBoolean()) drawSoon(g, cl.get(i), i, P.get("#" + i), mx, my);
        for (JsonObject n : nodes()) {
            String id = n.get("id").getAsString();
            double[] p = P.get(id);
            if (p == null) continue;
            if (drawNode(g, id, n, p, mx, my, now)) hover = id;
        }
        g.fillGradient(cx0, cy0 + ch - 22, cx0 + cw, cy0 + ch, 0x00000000, 0xC0080510);   // подложка под подсказку
        g.disableScissor();

        // плашки поверх полотна
        String pts = dm() + " очк. древа";
        int pw = font.width(pts) + 22;
        Skin.plate(g, cx0 + 6, cy0 + 6, pw, 18);
        blit(g, "tree/i_mage", cx0 + 10, cy0 + 9, 12, 12, 0xFFC9A0FF);
        g.drawString(font, pts, cx0 + 25, cy0 + 11, 0xFFE6D6FF, false);
        // кнопки масштаба
        int bx = cx0 + cw - 66, by = cy0 + 6;
        String[] zb = {"−", "+", "⟲"};
        for (int k = 0; k < 3; k++) {
            boolean hv = Skin.hover(mx, my, bx + k * 21, by, 19, 18);
            Skin.plate(g, bx + k * 21, by, 19, 18);
            if (hv) g.fill(bx + k * 21 + 2, by + 2, bx + k * 21 + 17, by + 16, 0x30FFFFFF);
            g.drawCenteredString(font, zb[k], bx + k * 21 + 10, by + 5, hv ? Skin.GOLD_L : Skin.DIM);
        }
        String hint = "Колесо — масштаб · тяни — двигать · два щелчка — изучить";
        g.pose().pushPose(); g.pose().translate(cx0 + cw / 2f, cy0 + ch - 10, 0); g.pose().scale(0.75f, 0.75f, 1);
        g.drawCenteredString(font, hint, 0, 0, 0xFF7A6E92);
        g.pose().popPose();

        if (hover != null && !dragging) tooltip(g, node(hover), hover, mx, my);
        else { String sh = soonHover(mx, my); if (sh != null) soonTip(g, sh, mx, my); }
        fx.render(g, font, width, height);
    }

    /** Фон полотна: глубокий градиент, туманность, мерцающие звёзды, орбиты ветвей, секторы классов. */
    private void drawSky(GuiGraphics g, long now) {
        g.fillGradient(cx0, cy0, cx0 + cw, cy0 + ch, 0xFF0B0816, 0xFF130C22);
        double cxs = sx(0), cys = sy(0);
        // туманность
        blitF(g, "tree/halo", cxs - R_MAX * zoom * 1.3, cys - R_MAX * zoom * 1.3, R_MAX * zoom * 2.6, 0x2A6A3AB0);
        blitF(g, "tree/halo", cxs - 60 * zoom, cys - 60 * zoom, 120 * zoom, 0x309070FF);
        // звёзды (фиксированы к полотну, слабый параллакс)
        Random r = new Random(1337);
        for (int i = 0; i < 140; i++) {
            double x = cx0 + r.nextDouble() * cw + panX * 0.15, y = cy0 + r.nextDouble() * ch + panY * 0.15;
            x = cx0 + Math.floorMod((int) (x - cx0), cw); y = cy0 + Math.floorMod((int) (y - cy0), ch);
            float tw = 0.35f + 0.65f * (float) Math.abs(Math.sin(now / (900.0 + i * 13) + i));
            int a = (int) (tw * (r.nextBoolean() ? 140 : 70));
            int c = r.nextInt(5) == 0 ? 0xC9A0FF : r.nextInt(4) == 0 ? 0xFFE6B0 : 0xFFFFFF;
            g.fill((int) x, (int) y, (int) x + 1, (int) y + 1, a << 24 | c);
        }
        // секторы классов и орбиты
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        List<JsonObject> cl = list("classes");
        for (int i = 0; i < 6 && i < cl.size(); i++) {
            JsonObject c = cl.get(i);
            int col = c.get("color").getAsInt();
            boolean mine = c.get("id").getAsString().equals(j().get("cls").getAsString());
            int a = mine ? 0x22 : c.get("open").getAsBoolean() ? 0x10 : 0x06;
            double th = Math.toRadians(-90 + i * 60);
            for (int k = 0; k < 10; k++) {
                double a0 = th - Math.toRadians(30) + Math.toRadians(6) * k, a1 = a0 + Math.toRadians(6);
                vertex(vc, m, sx(0), sy(0), 0, col);
                vertex(vc, m, sx(Math.cos(a0) * R_MAX), sy(Math.sin(a0) * R_MAX), a, col);
                vertex(vc, m, sx(Math.cos(a1) * R_MAX), sy(Math.sin(a1) * R_MAX), a, col);
                vertex(vc, m, sx(0), sy(0), 0, col);
                vertex(vc, m, sx(0), sy(0), 0, col);
                vertex(vc, m, sx(Math.cos(a1) * R_MAX), sy(Math.sin(a1) * R_MAX), a, col);
                vertex(vc, m, sx(Math.cos(a0) * R_MAX), sy(Math.sin(a0) * R_MAX), a, col);
                vertex(vc, m, sx(0), sy(0), 0, col);
            }
        }
        for (double rr : new double[]{R_CLASS, R_CHAIN1, R_CHAIN2, R_SKILL}) circle(vc, m, rr, 1.0, 0x22, 0xB8A0E0);
        g.flush();
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, double x, double y, int a, int rgb) {
        vc.addVertex(m, (float) x, (float) y, 0).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a);
    }

    private void circle(VertexConsumer vc, Matrix4f m, double r, double w, int a, int rgb) {
        int n = 96;
        for (int i = 0; i < n; i++) {
            double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
            double ri = r * zoom - w / 2, ro = r * zoom + w / 2, x = sx(0), y = sy(0);
            line(vc, m, x + Math.cos(a0) * r * zoom, y + Math.sin(a0) * r * zoom, x + Math.cos(a1) * r * zoom, y + Math.sin(a1) * r * zoom, w, a, rgb);
        }
    }

    /** Толстая линия (прямоугольник вдоль отрезка). */
    private static void line(VertexConsumer vc, Matrix4f m, double x0, double y0, double x1, double y1, double w, int a, int rgb) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy);
        if (len < 1e-3) return;
        double nx = -dy / len * w / 2, ny = dx / len * w / 2;
        vertex(vc, m, x0 + nx, y0 + ny, a, rgb);
        vertex(vc, m, x0 - nx, y0 - ny, a, rgb);
        vertex(vc, m, x1 - nx, y1 - ny, a, rgb);
        vertex(vc, m, x1 + nx, y1 + ny, a, rgb);
        vertex(vc, m, x1 + nx, y1 + ny, a, rgb);   // и обратная сторона (без отсечения граней)
        vertex(vc, m, x1 - nx, y1 - ny, a, rgb);
        vertex(vc, m, x0 - nx, y0 - ny, a, rgb);
        vertex(vc, m, x0 + nx, y0 + ny, a, rgb);
    }

    private String[] parents(String id) {
        if (id.equals("tp")) return new String[0];
        String p = id.substring(0, 1);
        if (id.endsWith("_base")) return new String[]{"tp"};
        if (id.endsWith("_skill")) return new String[]{p + "_a2", p + "_b2"};
        if (id.endsWith("1")) return new String[]{p + "_base"};
        return new String[]{id.substring(0, id.length() - 1) + "1"};
    }

    private void drawLinks(GuiGraphics g, Map<String, double[]> P, long now) {
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        List<double[]> flows = new ArrayList<>();
        List<Integer> flowCol = new ArrayList<>();
        // «скоро»: пунктир от центра
        List<JsonObject> cl = list("classes");
        for (int i = 0; i < 6 && i < cl.size(); i++) if (!cl.get(i).get("open").getAsBoolean()) {
            double[] b = P.get("#" + i);
            for (double t = 0.14; t < 0.86; t += 0.08)
                line(vc, m, sx(b[0] * t), sy(b[1] * t), sx(b[0] * (t + 0.04)), sy(b[1] * (t + 0.04)), Math.max(1, 1.2 * zoom), 0x40, 0x6A5A88);
        }
        for (JsonObject n : nodes()) {
            String id = n.get("id").getAsString();
            for (String f : parents(id)) {
                double[] a = P.get(f), b = P.get(id);
                if (a == null || b == null) continue;
                boolean lit = have(f) && have(id), open = have(f) && reachable(n);
                int col = color(n) & 0xFFFFFF;
                double x0 = sx(a[0]), y0 = sy(a[1]), x1 = sx(b[0]), y1 = sy(b[1]);
                if (lit) {
                    line(vc, m, x0, y0, x1, y1, 7 * zoom, 0x30, col);
                    line(vc, m, x0, y0, x1, y1, Math.max(1.4, 2.4 * zoom), 0xF0, col);
                    flows.add(new double[]{x0, y0, x1, y1}); flowCol.add(col);
                } else if (open) {
                    line(vc, m, x0, y0, x1, y1, Math.max(1.2, 1.8 * zoom), 0x90, 0xCDB8F0);
                } else line(vc, m, x0, y0, x1, y1, Math.max(1, 1.3 * zoom), 0x50, 0x4A4060);
            }
        }
        // бегущая энергия по изученным связям
        for (int i = 0; i < flows.size(); i++) {
            double[] f = flows.get(i);
            double t = ((now / 1400.0) + i * 0.37) % 1.0;
            double x = f[0] + (f[2] - f[0]) * t, y = f[1] + (f[3] - f[1]) * t, s = Math.max(1.5, 2.6 * zoom);
            line(vc, m, x - s, y, x + s, y, s * 1.4, 0xE0, 0xFFFFFF);
        }
        g.flush();
    }

    /** Узел-медальон. Возвращает true, если под курсором. */
    private boolean drawNode(GuiGraphics g, String id, JsonObject n, double[] p, int mx, int my, long now) {
        String kind = n.get("kind").getAsString();
        double s = size(kind) * zoom;
        double x = sx(p[0]), y = sy(p[1]);
        boolean hv = Math.hypot(mx - x, my - y) < s * 0.52 && mx >= cx0 && mx < cx0 + cw && my >= cy0 && my < cy0 + ch;
        boolean hav = n.get("have").getAsBoolean(), can = can(n), reach = reachable(n);
        int col = color(n);
        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 260.0);
        boolean isArmed = id.equals(armed);
        String base = kind.equals("CENTER") ? "center" : kind.equals("BASE") ? "class" : kind.equals("SKILL") ? "skill" : "node";
        // свечение
        if (hav) blitF(g, "tree/halo", x - s * 1.1, y - s * 1.1, s * 2.2, Skin.alpha(col, 0.55f));
        else if (can) blitF(g, "tree/halo", x - s * 1.15, y - s * 1.15, s * 2.3, Skin.alpha(0xFFFFD27A, 0.25f + 0.35f * pulse));
        if (isArmed) blitF(g, "tree/halo", x - s * 1.4, y - s * 1.4, s * 2.8, Skin.alpha(0xFFFFE08A, 0.5f + 0.4f * pulse));
        if (kind.equals("CENTER")) {   // звезда-роза за медальоном, медленно вращается
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) (now / 9000.0)));
            blitF(g, "tree/center_star", -s * 0.85, -s * 0.85, s * 1.7, hav ? Skin.alpha(0xFFFFD890, 0.38f) : 0x40A090C0);
            g.pose().popPose();
        }
        blitF(g, "tree/" + base + "_base", x - s / 2, y - s / 2, s, 0xFFFFFFFF);
        if (hav) blitF(g, "tree/" + base + "_base", x - s / 2, y - s / 2, s, Skin.alpha(col, 0.22f));
        // внутренняя кромка — цвет состояния; золотая чеканная рамка поверх
        int acc = hav ? col : isArmed ? 0xFFFFE08A : can ? Skin.alpha(0xFFFFD27A, 0.55f + 0.45f * pulse) : reach ? 0xFF6A5A88 : 0xFF2A2436;
        if (hv && !hav) acc = 0xFFFFF0C0;
        blitF(g, "tree/" + base + "_accent", x - s / 2, y - s / 2, s, acc);
        int frame = hav || can || isArmed || hv ? 0xFFFFFFFF : reach ? 0xFFC8BED4 : 0xFF6E6478;
        blitF(g, "tree/" + base + "_frame", x - s / 2, y - s / 2, s, frame);
        double is = s * (kind.equals("CENTER") ? 0.44 : kind.equals("STAT") ? 0.42 : 0.4);
        int icCol = hav ? 0xFFFFFFFF : reach ? col : 0xFF5A5268;
        blitF(g, "tree/i_" + icon(id, n), x - is / 2, y - is / 2 + (kind.equals("BASE") ? s * 0.03 : 0), is, icCol);
        // замок у недоступных (кроме «нет очков»)
        if (!hav && !reach && !kind.equals("CENTER")) { double ls = s * 0.36; blitF(g, "tree/i_lock", x + s * 0.24, y + s * 0.18, ls, 0xFF8A7AA0); }
        // подписи классов
        if (kind.equals("BASE")) {
            JsonObject c = cls(n.get("cls").getAsString());
            if (c != null) label(g, c.get("name").getAsString(), x, y + s * 0.62, Math.max(0.6f, Math.min(1.1f, zoom * 0.95f)), col, true);
        }
        return hv;
    }

    private void drawSoon(GuiGraphics g, JsonObject c, int i, double[] p, int mx, int my) {
        double s = 31 * zoom, x = sx(p[0]), y = sy(p[1]);
        int col = 0xFF000000 | c.get("color").getAsInt();
        blitF(g, "tree/class_base", x - s / 2, y - s / 2, s, 0xC0FFFFFF);
        blitF(g, "tree/class_accent", x - s / 2, y - s / 2, s, Skin.alpha(col, 0.3f));
        blitF(g, "tree/class_frame", x - s / 2, y - s / 2, s, 0xA05A5068);
        blitF(g, "tree/i_" + CLASS_ICON[i], x - s * 0.24, y - s * 0.27, s * 0.48, Skin.alpha(col, 0.35f));
        label(g, c.get("name").getAsString(), x, y + s * 0.62, Math.max(0.55f, Math.min(1f, zoom * 0.85f)), Skin.alpha(col, 0.55f), false);
        label(g, "скоро", x, y + s * 0.62 + 10 * Math.max(0.55f, Math.min(1f, zoom * 0.85f)), Math.max(0.5f, Math.min(0.85f, zoom * 0.7f)), 0x907A6E92, false);
    }

    private String soonHover(int mx, int my) {
        List<JsonObject> cl = list("classes");
        Map<String, double[]> P = pos();
        for (int i = 0; i < 6 && i < cl.size(); i++) {
            if (cl.get(i).get("open").getAsBoolean()) continue;
            double[] p = P.get("#" + i);
            if (Math.hypot(mx - sx(p[0]), my - sy(p[1])) < 12 * zoom) return cl.get(i).get("id").getAsString();
        }
        return null;
    }

    private void label(GuiGraphics g, String s, double x, double y, float sc, int col, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(sc, sc, 1);
        int w = font.width(s);
        if (shadow) g.fill(-w / 2 - 3, -2, w / 2 + 3, 10, 0x90000000);
        g.drawString(font, s, -w / 2, 0, col, shadow);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ подсказка
    private void tooltip(GuiGraphics g, JsonObject n, String id, int mx, int my) {
        if (n == null) return;
        int col = color(n);
        String kind = n.get("kind").getAsString();
        String sub = switch (kind) { case "CENTER" -> "Центр древа · навык слота 3"; case "BASE" -> "Выбор класса · пассивное умение"; case "SKILL" -> "Навык слота 4"; default -> "Улучшение"; };
        String name = n.get("name").getAsString();
        JsonObject c = n.get("cls").getAsString().isEmpty() ? null : cls(n.get("cls").getAsString());
        int tw = 190;
        List<String> desc = Skin.wrap(font, n.get("desc").getAsString(), tw - 16);
        boolean hav = n.get("have").getAsBoolean();
        String why = n.get("why").getAsString();
        int cost = n.get("cost").getAsInt();
        String status; int stc;
        if (hav) { status = "✔ Изучено"; stc = Skin.GOOD; }
        else if (!why.isEmpty()) { status = "🔒 " + Character.toUpperCase(why.charAt(0)) + why.substring(1); stc = Skin.BAD; }
        else if (dm() < cost) { status = "Не хватает очков: нужно " + cost + ", есть " + dm(); stc = Skin.BAD; }
        else if (id.equals(armed)) { status = "Щёлкни ещё раз — изучить"; stc = Skin.GOLD_L; }
        else { status = "Щёлкни дважды — изучить"; stc = Skin.GOLD_L; }
        int th = 8 + 12 + 10 + (c != null && kind.equals("BASE") ? 10 : 0) + 6 + desc.size() * 10 + 8 + 12 + 6;
        int x = mx + 14, y = my - 10;
        if (x + tw > width - 4) x = mx - tw - 10;
        if (y + th > height - 4) y = height - 4 - th;
        if (y < 4) y = 4;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        Skin.plate(g, x, y, tw, th);
        g.fill(x + 3, y + 3, x + 5, y + th - 3, col);                 // цветная кромка
        int yy = y + 7;
        g.drawString(font, name, x + 10, yy, col, true);
        String c2 = cost + " ◆";
        if (!hav) g.drawString(font, c2, x + tw - 8 - font.width(c2), yy, dm() >= cost ? 0xFFC9A0FF : Skin.BAD, false);
        yy += 12;
        g.drawString(font, sub, x + 10, yy, Skin.DIM, false); yy += 10;
        if (c != null && kind.equals("BASE")) { g.drawString(font, "Оружие: " + c.get("weapons").getAsString(), x + 10, yy, Skin.FAINT, false); yy += 10; }
        g.fill(x + 10, yy + 2, x + tw - 8, yy + 3, Skin.alpha(col, 0.35f)); yy += 7;
        for (String line : desc) { g.drawString(font, line, x + 10, yy, Skin.TEXT, false); yy += 10; }
        yy += 6;
        g.drawString(font, status, x + 10, yy, stc, false);
        g.pose().popPose();
    }

    private void soonTip(GuiGraphics g, String clsId, int mx, int my) {
        JsonObject c = cls(clsId);
        if (c == null) return;
        int col = 0xFF000000 | c.get("color").getAsInt(), tw = 170;
        List<String> d = Skin.wrap(font, c.get("desc").getAsString(), tw - 16);
        int th = 8 + 12 + d.size() * 10 + 8;
        int x = Math.min(mx + 14, width - tw - 4), y = Math.max(4, Math.min(my - 10, height - th - 4));
        g.pose().pushPose(); g.pose().translate(0, 0, 400);
        Skin.plate(g, x, y, tw, th);
        g.fill(x + 3, y + 3, x + 5, y + th - 3, col);
        g.drawString(font, c.get("name").getAsString() + " · скоро", x + 10, y + 7, col, true);
        int yy = y + 19;
        for (String l : d) { g.drawString(font, l, x + 10, yy, Skin.DIM, false); yy += 10; }
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ спрайты с плавающими координатами
    private static final Map<String, ResourceLocation> IDS = new HashMap<>();
    private static void blit(GuiGraphics g, String name, double x, double y, double w, double h, int argb) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale((float) (w / 64), (float) (h / 64), 1);
        g.blitSprite(IDS.computeIfAbsent(name, com.pvpciv.PvpCiv::id), 0, 0, 64, 64);
        g.pose().popPose();
        g.setColor(1, 1, 1, 1);
    }
    private static void blitF(GuiGraphics g, String name, double x, double y, double s, int argb) { blit(g, name, x, y, s, s, argb); }

    // ------------------------------------------------------------------ ввод
    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        float fit = (float) (Math.min(cw, ch) / (2 * R_MAX));
        float nz = Math.max(fit * 0.8f, Math.min(fit * 3.2f, zoom * (dy > 0 ? 1.15f : 1 / 1.15f)));
        // масштаб к курсору
        double ox = mx - (cx0 + cw / 2.0) - panX, oy = my - (cy0 + ch / 2.0) - panY;
        panX -= (float) (ox * (nz / zoom - 1));
        panY -= (float) (oy * (nz / zoom - 1));
        zoom = nz;
        clampPan();
        return true;
    }

    private void clampPan() {
        float lim = (float) (R_MAX * zoom);
        panX = Math.max(-lim, Math.min(lim, panX));
        panY = Math.max(-lim, Math.min(lim, panY));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        pressX = mx; pressY = my; dragging = false; pressed = true;
        int bx = cx0 + cw - 66, by = cy0 + 6;
        for (int k = 0; k < 3; k++) if (Skin.hover((int) mx, (int) my, bx + k * 21, by, 19, 18)) {
            if (k == 2) { zoom = (float) (Math.min(cw, ch) / (2 * R_MAX)); panX = panY = 0; }
            else mouseScrolled(cx0 + cw / 2.0 + panX, cy0 + ch / 2.0 + panY, 0, k == 1 ? 1 : -1);
            click(); return true;
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        if (!pressed) return true;   // кнопку зажали ещё в прошлом окне — это не перетаскивание
        if (Math.hypot(mx - pressX, my - pressY) > 3) dragging = true;
        if (dragging) { panX += (float) dx; panY += (float) dy; clampPan(); }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int b) {
        boolean was = pressed;
        pressed = false;
        if (!was) return true;
        if (dragging) { dragging = false; return true; }
        if (b == 0 && hover != null) {
            JsonObject n = node(hover);
            if (n != null && can(n)) {
                if (hover.equals(armed) && System.currentTimeMillis() - armedAt < 3500) {
                    PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("cls:node:" + hover));
                    armed = null;
                } else { armed = hover; armedAt = System.currentTimeMillis(); }
                click();
            }
        }
        return super.mouseReleased(mx, my, b);
    }

    private void click() {
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
        PacketDistributor.sendToServer(new com.pvpciv.net.Network.OpenInventory());
    }
}
