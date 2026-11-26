package com.pvpciv.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pvpciv.PvpCiv;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.4 — лучи разлома демоницы: объёмные неоновые лазеры с бегущей текстурой.
 * Сервер присылает луч один раз (PhoneData 31): тип, траекторию и время по часам мира; клиент сам двигает его плавно, без мигания.
 * Перед появлением — неброское предупреждение: тонкая бледная нить там, где луч войдёт (у столбов — сужающееся кольцо на полу).
 */
public final class RiftFx {
    private RiftFx() {}
    private static final ResourceLocation TEX = PvpCiv.id("textures/misc/rift_beam.png");

    /** k: 0 — едущий отрезок (a,b + v·t), 1 — вращающийся (o,len,ang,om,two), 2 — вертикальный столб (o,h). */
    private record B(int k, Vec3 a, Vec3 b, Vec3 v, double len, double ang, double om, boolean two, double h,
                     long t0, int warn, int life, int color, float w) {}
    private static final List<B> LIST = new ArrayList<>();

    private static final ResourceLocation SOFT = PvpCiv.id("textures/misc/rift_soft.png"), LINE = PvpCiv.id("textures/misc/rift_line.png");
    /** 1.4: HD-оформление арены: 0 — выкл, 1 — плиты (стадия 1), 2 — пол (лазеры/стена/финал). Плюс светящиеся контуры стен. */
    private static int mode; private static Vec3 at; private static int gridN, tile, rad; private static long fxOn;
    public static void sync(JsonObject j) {
        if (j.has("fx")) {
            mode = j.get("fx").getAsInt();
            if (mode > 0) { at = v(j, "at"); gridN = j.has("n") ? j.get("n").getAsInt() : 0; tile = j.has("s") ? j.get("s").getAsInt() : 4; rad = j.has("r") ? j.get("r").getAsInt() : 14; fxOn = System.currentTimeMillis(); }
            return;
        }
        if (j.has("clear")) { LIST.clear(); return; }
        int k = j.get("k").getAsInt();
        Vec3 a = v(j, "a"), b = j.has("b") ? v(j, "b") : a, vel = j.has("v") ? v(j, "v") : Vec3.ZERO;
        if (LIST.size() > 120) LIST.remove(0);
        LIST.add(new B(k, a, b, vel, d(j, "len"), d(j, "ang"), d(j, "om"), j.has("two") && j.get("two").getAsBoolean(), d(j, "h"),
                j.get("t0").getAsLong(), j.get("warn").getAsInt(), j.get("life").getAsInt(), j.get("c").getAsInt(), (float) d(j, "w")));
    }

    private static double d(JsonObject j, String k) { return j.has(k) ? j.get(k).getAsDouble() : 0; }
    private static Vec3 v(JsonObject j, String k) { JsonArray x = j.getAsJsonArray(k); return new Vec3(x.get(0).getAsDouble(), x.get(1).getAsDouble(), x.get(2).getAsDouble()); }

    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || (LIST.isEmpty() && mode == 0)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { LIST.clear(); mode = 0; return; }
        if (mode > 0) arena(e, mc);
        double now = mc.level.getGameTime() + e.getPartialTick().getGameTimeDeltaPartialTick(false);
        LIST.removeIf(b -> now > b.t0 + b.life + 2);
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        RenderType rt = RenderType.eyes(TEX);
        VertexConsumer vc = buf.getBuffer(rt);
        float scroll = (float) (now * 0.09);
        for (B b : LIST) {
            double t = now - b.t0;
            float r = (b.color >> 16 & 255) / 255f, g = (b.color >> 8 & 255) / 255f, bl = (b.color & 255) / 255f;
            if (t < 0) {   // предупреждение
                if (-t > b.warn) continue;
                float pulse = 0.18f + 0.12f * (float) Math.sin(now * 0.9);
                float k = pulse * (float) (1 - (-t) / b.warn * 0.6);
                if (b.k == 2) ringWarn(vc, m, b.a, 0.6 + 0.9 * (-t / b.warn), r * k * 1.6f, g * k * 1.6f, bl * k * 1.6f, scroll);
                else { Vec3[] ab = ends(b, 0); box(vc, m, ab[0], ab[1], 0.03f, r * k, g * k, bl * k, scroll); }
                continue;
            }
            if (t > b.life) continue;
            float fade = (float) Math.min(1, Math.min(t / 3.0, (b.life - t) / 4.0));
            Vec3[] ab = ends(b, t);
            float w = b.w * (b.k == 2 ? (float) Math.min(1, t / 2.0) : 1f);
            box(vc, m, ab[0], ab[1], w * 2.4f, r * 0.22f * fade, g * 0.22f * fade, bl * 0.22f * fade, scroll * 0.6f);   // ореол
            box(vc, m, ab[0], ab[1], w, r * 0.95f * fade, g * 0.95f * fade, bl * 0.95f * fade, scroll);                  // тело
            box(vc, m, ab[0], ab[1], w * 0.35f, 0.75f * fade, 0.75f * fade, 0.75f * fade, -scroll * 1.4f);               // белая сердцевина
        }
        ps.popPose();
        buf.endBatch(rt);
    }

    private static Vec3[] ends(B b, double t) {
        return switch (b.k) {
            case 1 -> {
                double an = b.ang + b.om * t;
                Vec3 dir = new Vec3(Math.cos(an), 0, Math.sin(an)).scale(b.len);
                yield new Vec3[]{b.two ? b.a.subtract(dir) : b.a, b.a.add(dir)};
            }
            case 2 -> new Vec3[]{b.a, b.a.add(0, b.h, 0)};
            default -> new Vec3[]{b.a.add(b.v.scale(t)), b.b.add(b.v.scale(t))};
        };
    }

    /** Брусок-луч от a до b с полушириной w; текстура бежит вдоль. */
    static void box(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float w, float r, float g, float bl, float scroll) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1e-4) return;
        Vec3 dn = d.scale(1 / len);
        Vec3 n1 = Math.abs(dn.y) > 0.9 ? new Vec3(1, 0, 0) : dn.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 n2 = dn.cross(n1).normalize();
        n1 = n1.scale(w); n2 = n2.scale(w);
        float u0 = -scroll, u1 = u0 + (float) (len / 2.5);
        Vec3[] c = {n1.add(n2), n1.subtract(n2), n1.scale(-1).subtract(n2), n1.scale(-1).add(n2)};
        for (int i = 0; i < 4; i++) {
            Vec3 p = c[i], q = c[(i + 1) % 4];
            Vec3 a0 = a.add(p), a1 = a.add(q), b1 = b.add(q), b0 = b.add(p);
            vert(vc, m, a0, u0, 0, r, g, bl); vert(vc, m, a1, u0, 1, r, g, bl); vert(vc, m, b1, u1, 1, r, g, bl); vert(vc, m, b0, u1, 0, r, g, bl);
            vert(vc, m, b0, u1, 0, r, g, bl); vert(vc, m, b1, u1, 1, r, g, bl); vert(vc, m, a1, u0, 1, r, g, bl); vert(vc, m, a0, u0, 0, r, g, bl);
        }
    }

    private static void ringWarn(VertexConsumer vc, Matrix4f m, Vec3 o, double rad, float r, float g, float bl, float scroll) {
        int n = 20;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            Vec3 p = o.add(Math.cos(a0) * rad, 0.04, Math.sin(a0) * rad), q = o.add(Math.cos(a1) * rad, 0.04, Math.sin(a1) * rad);
            box(vc, m, p, q, 0.035f, r, g, bl, scroll);
        }
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Vec3 p, float u, float v, float r, float g, float b) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(Math.min(1, r), Math.min(1, g), Math.min(1, b), 1f)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
    }


    // ------------------------------------------------------------------ HD-арена: цельные поверхности + свет + HD-полоски
    private static final List<float[]> EDGES = new ArrayList<>();   // x0,y0,z0,x1,y1,z1,group
    private static long edgesAt = -1;

    private static void arena(RenderLevelStageEvent e, Minecraft mc) {
        if (mc.player == null || at == null || mc.player.position().distanceTo(at) > 100) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        double sec = (System.currentTimeMillis() - fxOn) / 1000.0;
        float in = (float) Math.min(1, sec / 1.2);
        RenderType soft = RenderType.eyes(SOFT);
        VertexConsumer vc = buf.getBuffer(soft);
        if (mode == 1) tileFill(vc, m, mc, sec, in); else floorFill(vc, m, sec, in);
        buf.endBatch(soft);
        RenderType line = RenderType.eyes(LINE);
        vc = buf.getBuffer(line);
        if (mode == 1) tileEdges(vc, m, mc, sec, in); else floorLines(vc, m, sec, in);
        wallEdges(vc, m, mc, sec, in);
        buf.endBatch(line);
        ps.popPose();
    }

    private static final float[][] TCOL = {{1f, 0.19f, 0.31f}, {0.23f, 0.48f, 1f}, {1f, 0.82f, 0.23f}, {0.22f, 1f, 0.48f}};

    /** −1 — нет плиты, 0..3 — цвет, 4 — погасшая. */
    private static int tileAt(Minecraft mc, int gx, int gz) {
        var st = mc.level.getBlockState(net.minecraft.core.BlockPos.containing(at.x + gx * tile + 0.5, at.y - 0.5, at.z + gz * tile + 0.5));
        if (st.is(com.pvpciv.registry.ModRegistry.RIFT_NEON_RED.get())) return 0;
        if (st.is(com.pvpciv.registry.ModRegistry.RIFT_NEON_BLUE.get())) return 1;
        if (st.is(com.pvpciv.registry.ModRegistry.RIFT_NEON_YELLOW.get())) return 2;
        if (st.is(com.pvpciv.registry.ModRegistry.RIFT_NEON_GREEN.get())) return 3;
        if (st.is(com.pvpciv.registry.ModRegistry.RIFT_NEON_OFF.get())) return 4;
        return -1;
    }

    /** Плиты: по цельной поверхности бежит мягкая диагональная волна света. */
    private static void tileFill(VertexConsumer vc, Matrix4f m, Minecraft mc, double sec, float in) {
        double y = at.y + 0.011;
        for (int gx = 0; gx < gridN; gx++) for (int gz = 0; gz < gridN; gz++) {
            int c = tileAt(mc, gx, gz);
            if (c < 0 || c == 4) continue;
            float[] col = TCOL[c];
            for (int x = 0; x < tile; x++) for (int z = 0; z < tile; z++) {
                double x0 = at.x + gx * tile + x, z0 = at.z + gz * tile + z;
                float k00 = sweep(x0, z0, sec) * in, k10 = sweep(x0 + 1, z0, sec) * in, k11 = sweep(x0 + 1, z0 + 1, sec) * in, k01 = sweep(x0, z0 + 1, sec) * in;
                flatV(vc, m, x0, y, z0, 0, 0, col, k00); flatV(vc, m, x0, y, z0 + 1, 0, 1, col, k01); flatV(vc, m, x0 + 1, y, z0 + 1, 1, 1, col, k11); flatV(vc, m, x0 + 1, y, z0, 1, 0, col, k10);
            }
        }
    }

    private static float sweep(double x, double z, double sec) {
        double w = Math.pow(Math.max(0, Math.cos((x + z) * 0.11 - sec * 2.4)), 6);
        double w2 = Math.pow(Math.max(0, Math.cos((x - z) * 0.07 + sec * 1.3)), 10) * 0.4;
        return (float) (0.12 + w * 0.6 + w2);
    }

    /** HD-рамки плит: тонкая полоска по контуру и иногда пробегающий по ней огонёк. Гаснущие плиты тревожно мигают. */
    private static void tileEdges(VertexConsumer vc, Matrix4f m, Minecraft mc, double sec, float in) {
        double y = at.y + 0.014, inset = 0.14;
        int per = tile * 4;
        for (int gx = 0; gx < gridN; gx++) for (int gz = 0; gz < gridN; gz++) {
            int c = tileAt(mc, gx, gz);
            if (c < 0) continue;
            boolean off = c == 4;
            float[] col = off ? new float[]{1f, 0.25f, 0.3f} : TCOL[c];
            double x0 = at.x + gx * tile + inset, z0 = at.z + gz * tile + inset, x1 = at.x + (gx + 1) * tile - inset, z1 = at.z + (gz + 1) * tile - inset;
            double ph = ((gx * 7 + gz * 13) % 10) / 10.0, cyc = sec * 0.45 + ph * 2;
            boolean run = !off && cyc % 2 < 1;
            double head = (cyc % 1) * per;
            float base = off ? (float) (0.45 + 0.4 * Math.sin(sec * 25)) : 0.6f;
            for (int i = 0; i < per; i++) {
                int side = i / tile, k = i % tile;
                double ka = k / (double) tile, kb = (k + 1) / (double) tile;
                double ax, az, bx, bz;
                switch (side) {
                    case 0 -> { ax = x0 + (x1 - x0) * ka; az = z0; bx = x0 + (x1 - x0) * kb; bz = z0; }
                    case 1 -> { ax = x1; az = z0 + (z1 - z0) * ka; bx = x1; bz = z0 + (z1 - z0) * kb; }
                    case 2 -> { ax = x1 - (x1 - x0) * ka; az = z1; bx = x1 - (x1 - x0) * kb; bz = z1; }
                    default -> { ax = x0; az = z1 - (z1 - z0) * ka; bx = x0; bz = z1 - (z1 - z0) * kb; }
                }
                float k1 = base;
                float wh = 0;
                if (run) { double d = (head - (i + 0.5) + per) % per; if (d < 5) { float q = (float) Math.pow(1 - d / 5, 2); k1 += q * 1.4f; wh = q; } }
                float wt = 0.3f + wh * 0.6f;   // рамка светлее самой плиты
                float[] cc = {col[0] + (1 - col[0]) * wt, col[1] + (1 - col[1]) * wt, col[2] + (1 - col[2]) * wt};
                strip(vc, m, ax, az, bx, bz, y, 0.09, cc, k1 * in);
                strip(vc, m, ax, az, bx, bz, y - 0.001, 0.32, col, k1 * in * 0.35f);   // ореол
            }
        }
    }

    /** Пол: цельная тёмная поверхность, по ней — кольца-волны от центра и вспышки в такт. */
    private static void floorFill(VertexConsumer vc, Matrix4f m, double sec, float in) {
        double y = at.y + 0.011;
        float flash = (float) Math.pow(1 - (sec / 0.47) % 1.0, 3) * 0.12f;
        for (int x = -rad; x < rad; x++) for (int z = -rad; z < rad; z++) {
            if (Math.hypot(x + 0.5, z + 0.5) > rad + 0.3) continue;
            double x0 = at.x + x, z0 = at.z + z;
            fillV(vc, m, x0, y, z0, 0, 0, x, z, sec, flash, in); fillV(vc, m, x0, y, z0 + 1, 0, 1, x, z + 1, sec, flash, in);
            fillV(vc, m, x0 + 1, y, z0 + 1, 1, 1, x + 1, z + 1, sec, flash, in); fillV(vc, m, x0 + 1, y, z0, 1, 0, x + 1, z, sec, flash, in);
        }
    }

    private static double ringWave(double d, double sec) { return Math.pow(Math.max(0, Math.cos((d - sec * 6.0) * 0.5)), 8); }

    private static float[] floorCol(double d) {
        float mix = (float) Math.min(1, d / 14.0);
        return new float[]{1.0f * (1 - mix) + 0.2f * mix, 0.25f * (1 - mix) + 0.9f * mix, 1f};
    }

    private static void fillV(VertexConsumer vc, Matrix4f m, double x, double y, double z, float u, float v, int gx, int gz, double sec, float flash, float in) {
        double d = Math.hypot(gx, gz);
        float k = (float) ((0.09 + ringWave(d, sec) * 0.7 + flash) * in);
        flatV(vc, m, x, y, z, u, v, floorCol(d), k);
    }

    /** HD-линии пола: два кольца (r=6 и край) с бегущим огоньком и крупная сетка 4×4, которая вспыхивает, когда через неё проходит волна. */
    private static void floorLines(VertexConsumer vc, Matrix4f m, double sec, float in) {
        double y = at.y + 0.014, cx = at.x + 0.5, cz = at.z + 0.5;
        float flash = (float) Math.pow(1 - (sec / 0.47) % 1.0, 3);
        for (double rr : new double[]{6, rad + 0.2}) {
            int n = (int) (rr * 6);
            double head = (sec * 0.3 * (rr < 10 ? -1 : 1)) % 1.0;
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                double dd = ((head < 0 ? head + 1 : head) - (i + 0.5) / n + 1) % 1.0;
                float k = 0.3f + flash * 0.25f + (dd < 0.12 ? (float) Math.pow(1 - dd / 0.12, 2) * 1.3f : 0);
                float[] c = floorCol(rr);
                strip(vc, m, cx + Math.cos(a0) * rr, cz + Math.sin(a0) * rr, cx + Math.cos(a1) * rr, cz + Math.sin(a1) * rr, y, 0.08, c, k * in);
                strip(vc, m, cx + Math.cos(a0) * rr, cz + Math.sin(a0) * rr, cx + Math.cos(a1) * rr, cz + Math.sin(a1) * rr, y - 0.001, 0.35, c, k * in * 0.22f);
            }
        }
        for (int axis = 0; axis < 2; axis++) for (int g = -12; g <= 12; g += 4) {
            double half = Math.sqrt(Math.max(0, rad * rad - g * g)) - 0.3;
            for (double s0 = -half; s0 < half; s0 += 1) {
                double s1 = Math.min(half, s0 + 1), sm = (s0 + s1) / 2;
                double d = Math.hypot(g, sm);
                float k = (float) (0.13 + ringWave(d, sec) * 1.2);
                float[] c = floorCol(d);
                if (axis == 0) strip(vc, m, cx + g, cz + s0, cx + g, cz + s1, y, 0.06, c, k * in);
                else strip(vc, m, cx + s0, cz + g, cx + s1, cz + g, y, 0.06, c, k * in);
            }
        }
    }

    /** Контуры стен разлома: светящиеся рёбра по силуэту (стена — фиолетовые, хрупкие — бирюзовые, ступени — жёлтые). */
    private static void wallEdges(VertexConsumer vc, Matrix4f m, Minecraft mc, double sec, float in) {
        long gt = mc.level.getGameTime();
        if (gt != edgesAt) { edgesAt = gt; scanEdges(mc); }
        float[][] gc = {{0.85f, 0.35f, 1f}, {0.3f, 1f, 1f}, {1f, 0.8f, 0.2f}};
        for (float[] ed : EDGES) {
            int g = (int) ed[6];
            double mx = (ed[0] + ed[3]) / 2, mz = (ed[2] + ed[5]) / 2;
            float k = (float) (0.9 + 0.7 * Math.pow(Math.max(0, Math.cos((mx + mz) * 0.25 - sec * 4)), 6)) * in;
            Vec3 a = new Vec3(ed[0], ed[1], ed[2]), b = new Vec3(ed[3], ed[4], ed[5]);
            float[] c = gc[g];
            box(vc, m, a, b, 0.045f, c[0] * k, c[1] * k, c[2] * k, 0);
            box(vc, m, a, b, 0.13f, c[0] * k * 0.3f, c[1] * k * 0.3f, c[2] * k * 0.3f, 0);
        }
    }

    private static int group(net.minecraft.world.level.block.state.BlockState st) {
        if (st.is(com.pvpciv.registry.ModRegistry.RIFT_WALL.get())) return 0;
        if (st.is(com.pvpciv.registry.ModRegistry.RIFT_WALL_WEAK.get())) return 1;
        if (st.is(com.pvpciv.registry.ModRegistry.RIFT_STEP.get())) return 2;
        return -1;
    }

    private static void scanEdges(Minecraft mc) {
        EDGES.clear();
        int R = 17, bx = (int) Math.floor(at.x), by = (int) Math.floor(at.y), bz = (int) Math.floor(at.z);
        if (mode == 1) { bx += gridN * tile / 2; bz += gridN * tile / 2; }
        var lvl = mc.level;
        net.minecraft.core.BlockPos.MutableBlockPos q = new net.minecraft.core.BlockPos.MutableBlockPos();
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) for (int y = 0; y <= 5; y++) {
            int g = group(lvl.getBlockState(q.set(bx + x, by + y, bz + z)));
            if (g < 0) continue;
            boolean[] open = new boolean[6];
            for (int i = 0; i < 6; i++) open[i] = group(lvl.getBlockState(q.set(bx + x + dirs[i][0], by + y + dirs[i][1], bz + z + dirs[i][2]))) != g;
            float X = bx + x, Y = by + y, Z = bz + z;
            // 12 рёбер: рисуем, если открыты обе соседние грани
            for (int sx = 0; sx < 2; sx++) for (int sy = 0; sy < 2; sy++) if (open[sx == 1 ? 0 : 1] && open[sy == 1 ? 2 : 3]) EDGES.add(new float[]{X + sx, Y + sy, Z, X + sx, Y + sy, Z + 1, g});
            for (int sx = 0; sx < 2; sx++) for (int sz = 0; sz < 2; sz++) if (open[sx == 1 ? 0 : 1] && open[sz == 1 ? 4 : 5]) EDGES.add(new float[]{X + sx, Y, Z + sz, X + sx, Y + 1, Z + sz, g});
            for (int sy = 0; sy < 2; sy++) for (int sz = 0; sz < 2; sz++) if (open[sy == 1 ? 2 : 3] && open[sz == 1 ? 4 : 5]) EDGES.add(new float[]{X, Y + sy, Z + sz, X + 1, Y + sy, Z + sz, g});
        }
    }

    /** Плоская полоса на полу от a до b, ширина w (поперёк — профиль текстуры-линии). */
    static void strip(VertexConsumer vc, Matrix4f m, double ax, double az, double bx, double bz, double y, double w, float[] c, float k) {
        double dx = bx - ax, dz = bz - az, L = Math.hypot(dx, dz);
        if (L < 1e-4) return;
        double nx = -dz / L * w, nz = dx / L * w;
        flatV(vc, m, ax - nx, y, az - nz, 0, 0, c, k); flatV(vc, m, ax + nx, y, az + nz, 0, 1, c, k);
        flatV(vc, m, bx + nx, y, bz + nz, 1, 1, c, k); flatV(vc, m, bx - nx, y, bz - nz, 1, 0, c, k);
        flatV(vc, m, bx - nx, y, bz - nz, 1, 0, c, k); flatV(vc, m, bx + nx, y, bz + nz, 1, 1, c, k);
        flatV(vc, m, ax + nx, y, az + nz, 0, 1, c, k); flatV(vc, m, ax - nx, y, az - nz, 0, 0, c, k);
    }

    private static void flatV(VertexConsumer vc, Matrix4f m, double x, double y, double z, float u, float v, float[] c, float k) {
        vc.addVertex(m, (float) x, (float) y, (float) z).setColor(Math.min(1, c[0] * k), Math.min(1, c[1] * k), Math.min(1, c[2] * k), 1f)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
    }
}
