package com.pvpciv.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.*;

/**
 * 1.0.1 — арены боссов на клиенте.
 *  • Алтарь (босс спит, алтарь горит): вращающийся круг рун на полу и столб света.
 *  • Бой (арена закрыта): прозрачный энергетический купол. Кто касается стены (изнутри — не выпускает), от точки касания
 *    по куполу расходится волна. Войти можно, выйти нельзя (это решает сервер, тут — только картинка).
 * Данные — PhoneData 25 раз в полсекунды от сервера; через 3 с без данных арена гаснет.
 */
public final class ArenaFx {
    private ArenaFx() {}

    private static final class Arena {
        String id, type; Vec3 c; double r; boolean lock, lit; Vec3 altar; int col; long seen;
        final List<Ripple> ripples = new ArrayList<>();
        final Map<Integer, Long> touchCd = new HashMap<>();
        float fade;   // плавное появление купола
    }
    private record Ripple(Vec3 n, long born) {}
    private static final Map<String, Arena> A = new HashMap<>();

    public static void sync(JsonObject j) {
        long now = System.currentTimeMillis();
        JsonArray arr = j.getAsJsonArray("a");
        for (var el : arr) {
            JsonObject o = el.getAsJsonObject();
            String id = o.get("id").getAsString();
            Arena a = A.computeIfAbsent(id, k -> new Arena());
            a.id = id;
            a.c = new Vec3(o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble());
            a.r = o.get("r").getAsDouble();
            a.lock = o.get("lock").getAsBoolean();
            a.lit = o.get("lit").getAsBoolean();
            a.col = o.get("col").getAsInt();
            a.type = o.has("type") ? o.get("type").getAsString() : null;
            a.altar = o.has("ax") ? new Vec3(o.get("ax").getAsDouble(), o.get("ay").getAsDouble(), o.get("az").getAsDouble()) : null;
            a.seen = now;
        }
    }

    // ------------------------------------------------------------------ парящая модель босса над алтарём
    private static final Map<String, net.minecraft.world.entity.Entity> DUMMY = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel dummyLevel;

    public static void tick() {
        for (var e : DUMMY.values()) e.tickCount++;
    }

    private static void figures(RenderLevelStageEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != dummyLevel) { DUMMY.clear(); dummyLevel = mc.level; }
        Vec3 cam = e.getCamera().getPosition();
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = System.currentTimeMillis();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        for (Arena a : A.values()) {
            if (!a.lit || a.altar == null || a.type == null || a.altar.distanceTo(cam) > 64) continue;
            net.minecraft.world.entity.Entity d = DUMMY.computeIfAbsent(a.id, k -> {
                var t = net.minecraft.world.entity.EntityType.byString(a.type).orElse(null);
                return t == null ? null : t.create(mc.level);
            });
            if (d == null) continue;
            float h = Math.max(0.6f, d.getBbHeight());
            float sc = Math.min(0.42f, 0.9f / h);
            double bob = Math.sin(now / 600.0) * 0.12;
            float yaw = (now / 25f) % 360f;
            PoseStack ps = e.getPoseStack();
            ps.pushPose();
            ps.translate(a.altar.x + 0.5 - cam.x, a.altar.y + 2.1 + bob - cam.y, a.altar.z + 0.5 - cam.z);
            ps.scale(sc, sc, sc);
            d.setYRot(yaw); d.yRotO = yaw;
            if (d instanceof net.minecraft.world.entity.LivingEntity le) { le.yBodyRot = yaw; le.yBodyRotO = yaw; le.yHeadRot = yaw; le.yHeadRotO = yaw; }
            try { mc.getEntityRenderDispatcher().render(d, 0, 0, 0, yaw, pt, ps, buf, 0xF000F0); } catch (Exception ignored) {}
            ps.popPose();
        }
        buf.endBatch();
    }

    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES && !A.isEmpty()) { figures(e); return; }
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || A.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = System.currentTimeMillis();
        A.values().removeIf(a -> now - a.seen > 3000);
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        for (Arena a : A.values()) {
            if (a.c.distanceTo(cam) > a.r + 96) continue;
            float r = (a.col >> 16 & 255) / 255f, g = (a.col >> 8 & 255) / 255f, b = (a.col & 255) / 255f;
            if (a.lit && a.altar != null) altar(vc, m, a.altar, now, r, g, b);
            a.fade = Math.max(0, Math.min(1, a.fade + (a.lock ? 0.04f : -0.04f)));
            if (a.fade > 0) {
                touches(mc, a, now);
                dome(vc, m, a, now, r, g, b, cam);
            }
        }
        ps.popPose();
        buf.endBatch(RenderType.lightning());
    }

    // ------------------------------------------------------------------ касания стены → волны
    private static void touches(Minecraft mc, Arena a, long now) {
        a.ripples.removeIf(x -> now - x.born > 2200);
        for (Player p : mc.level.players()) {
            double dx = p.getX() - a.c.x, dz = p.getZ() - a.c.z;
            double flat = Math.hypot(dx, dz);
            if (Math.abs(flat - a.r) > 1.0) continue;
            Long cd = a.touchCd.get(p.getId());
            if (cd != null && now - cd < 450) continue;
            a.touchCd.put(p.getId(), now);
            Vec3 n = new Vec3(dx, Math.max(0, p.getY() + 1 - a.c.y), dz).normalize();
            if (a.ripples.size() < 24) a.ripples.add(new Ripple(n, now));
            if (p == mc.player) mc.player.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5f, 1.6f);
        }
    }

    // ------------------------------------------------------------------ купол
    private static final int LAT = 18, LON = 64;

    private static void dome(VertexConsumer vc, Matrix4f m, Arena a, long now, float r, float g, float b, Vec3 cam) {
        double R = a.r;
        Vec3 c = a.c.add(0, -1.5, 0);
        float t = (now % 100000) / 1000f;
        boolean inside = Math.hypot(cam.x - a.c.x, cam.z - a.c.z) < R;
        for (int i = -2; i < LAT; i++) {
            double la0 = Math.PI / 2 * i / LAT, la1 = Math.PI / 2 * (i + 1) / LAT;
            for (int j = 0; j < LON; j++) {
                double lo0 = Math.PI * 2 * j / LON, lo1 = Math.PI * 2 * (j + 1) / LON;
                Vec3 p00 = pt(la0, lo0), p01 = pt(la0, lo1), p11 = pt(la1, lo1), p10 = pt(la1, lo0);
                float a00 = al(a, p00, now, t, inside), a01 = al(a, p01, now, t, inside), a11 = al(a, p11, now, t, inside), a10 = al(a, p10, now, t, inside);
                // ячейка-«сота»: края ярче (сетка), середина почти прозрачна
                quad(vc, m, c, R, p00, p01, p11, p10, a00, a01, a11, a10, r, g, b, a.fade);
            }
        }
        // светящийся пояс у пола
        for (int j = 0; j < LON; j++) {
            double lo0 = Math.PI * 2 * j / LON, lo1 = Math.PI * 2 * (j + 1) / LON;
            float pulse = 0.35f + 0.25f * (float) Math.sin(t * 3 + j * 0.4);
            Vec3 q0 = c.add(Math.cos(lo0) * R, 1.5, Math.sin(lo0) * R), q1 = c.add(Math.cos(lo1) * R, 1.5, Math.sin(lo1) * R);
            Vec3 u = new Vec3(0, 0.12, 0);
            v(vc, m, q0.subtract(u), r, g, b, pulse * a.fade); v(vc, m, q1.subtract(u), r, g, b, pulse * a.fade);
            v(vc, m, q1.add(u), 1, 1, 1, pulse * 0.8f * a.fade); v(vc, m, q0.add(u), 1, 1, 1, pulse * 0.8f * a.fade);
            v(vc, m, q0.add(u), 1, 1, 1, pulse * 0.8f * a.fade); v(vc, m, q1.add(u), 1, 1, 1, pulse * 0.8f * a.fade);
            v(vc, m, q1.subtract(u), r, g, b, pulse * a.fade); v(vc, m, q0.subtract(u), r, g, b, pulse * a.fade);
        }
    }

    private static Vec3 pt(double lat, double lon) { return new Vec3(Math.cos(lat) * Math.cos(lon), Math.sin(lat), Math.cos(lat) * Math.sin(lon)); }

    /** Яркость вершины: тонкая мембрана + бегущая вверх полоса + волны от касаний (ярче всего — у точки касания). */
    private static float al(Arena a, Vec3 n, long now, float t, boolean inside) {
        double lat = Math.asin(Math.max(-1, Math.min(1, n.y)));
        float base = inside ? 0.16f : 0.22f;
        float scan = (float) Math.exp(-Math.pow((lat - ((t * 0.35) % 1.8 - 0.2)) / 0.06, 2)) * 0.3f;
        float rip = 0;
        for (Ripple r : a.ripples) {
            double age = (now - r.born) / 1000.0;
            double th = Math.acos(Math.max(-1, Math.min(1, n.dot(r.n))));
            double rad = age * 1.6;
            double fade = Math.max(0, 1 - age / 2.2);
            rip += (float) (Math.exp(-Math.pow((th - rad) / 0.07, 2)) * 0.9 * fade + Math.exp(-Math.pow(th / 0.12, 2)) * 0.9 * Math.max(0, 1 - age * 2));
        }
        return Math.min(0.9f, base + scan + rip);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 c, double R, Vec3 a, Vec3 b, Vec3 cc, Vec3 d,
                             float aa, float ab, float ac, float ad, float r, float g, float bl, float fade) {
        Vec3 A = c.add(a.scale(R)), B = c.add(b.scale(R)), C = c.add(cc.scale(R)), D = c.add(d.scale(R));
        Vec3 mid = A.add(B).add(C).add(D).scale(0.25);
        float am = (aa + ab + ac + ad) / 4 * 0.12f;
        // четыре треугольника-лепестка от середины: края ячейки светятся, середина гаснет → видна сетка сот
        tri(vc, m, mid, A, B, am, aa, ab, r, g, bl, fade);
        tri(vc, m, mid, B, C, am, ab, ac, r, g, bl, fade);
        tri(vc, m, mid, C, D, am, ac, ad, r, g, bl, fade);
        tri(vc, m, mid, D, A, am, ad, aa, r, g, bl, fade);
    }

    private static void tri(VertexConsumer vc, Matrix4f m, Vec3 o, Vec3 p, Vec3 q, float ao, float ap, float aq, float r, float g, float b, float fade) {
        // lightning() — квады: вырожденный квад (две последние вершины совпадают); обе стороны
        v(vc, m, o, r, g, b, ao * fade); v(vc, m, p, r, g, b, ap * fade); v(vc, m, q, r, g, b, aq * fade); v(vc, m, q, r, g, b, aq * fade);
        v(vc, m, o, r, g, b, ao * fade); v(vc, m, q, r, g, b, aq * fade); v(vc, m, p, r, g, b, ap * fade); v(vc, m, p, r, g, b, ap * fade);
    }

    // ------------------------------------------------------------------ алтарь
    private static void altar(VertexConsumer vc, Matrix4f m, Vec3 at, long now, float r, float g, float b) {
        double t = (now % 100000) / 1000.0;
        Vec3 c = at.add(0.5, 0.03, 0.5);
        // два кольца рун, вращаются навстречу
        ring(vc, m, c, 2.2, 2.55, t * 0.6, 16, r, g, b, 0.55f);
        ring(vc, m, c, 1.25, 1.45, -t * 0.9, 10, 1, 1, 1, 0.45f);
        // столб света
        float pulse = 0.10f + 0.05f * (float) Math.sin(t * 2.5);
        for (int k = 0; k < 2; k++) {
            double ang = k * Math.PI / 2 + t * 0.3;
            Vec3 w = new Vec3(Math.cos(ang) * 0.55, 0, Math.sin(ang) * 0.55);
            Vec3 lo = c.add(0, 0.2, 0), hi = c.add(0, 9, 0);
            v(vc, m, lo.subtract(w), r, g, b, pulse); v(vc, m, lo.add(w), r, g, b, pulse);
            v(vc, m, hi.add(w), r, g, b, 0); v(vc, m, hi.subtract(w), r, g, b, 0);
            v(vc, m, hi.subtract(w), r, g, b, 0); v(vc, m, hi.add(w), r, g, b, 0);
            v(vc, m, lo.add(w), r, g, b, pulse); v(vc, m, lo.subtract(w), r, g, b, pulse);
        }
    }

    private static void ring(VertexConsumer vc, Matrix4f m, Vec3 c, double r0, double r1, double rot, int glyphs, float r, float g, float b, float al) {
        int seg = glyphs * 4;
        for (int i = 0; i < seg; i++) {
            if (i % 4 == 3) continue;   // разрывы между «рунами»
            double a0 = rot + Math.PI * 2 * i / seg, a1 = rot + Math.PI * 2 * (i + 1) / seg;
            double rr0 = i % 4 == 1 ? r0 - 0.12 : r0;   // ступенька — силуэт руны
            Vec3 p0 = c.add(Math.cos(a0) * rr0, 0, Math.sin(a0) * rr0), p1 = c.add(Math.cos(a1) * rr0, 0, Math.sin(a1) * rr0);
            Vec3 q0 = c.add(Math.cos(a0) * r1, 0, Math.sin(a0) * r1), q1 = c.add(Math.cos(a1) * r1, 0, Math.sin(a1) * r1);
            v(vc, m, p0, r, g, b, al); v(vc, m, p1, r, g, b, al); v(vc, m, q1, 1, 1, 1, al * 0.8f); v(vc, m, q0, 1, 1, 1, al * 0.8f);
            v(vc, m, q0, 1, 1, 1, al * 0.8f); v(vc, m, q1, 1, 1, 1, al * 0.8f); v(vc, m, p1, r, g, b, al); v(vc, m, p0, r, g, b, al);
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Math.max(0, Math.min(1, a)));
    }
}
