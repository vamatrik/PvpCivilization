package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 0.10 — светящиеся линии в мире (клиент): красные лазеры-подсказки стрелков и трассеры пуль.
 * Сервер шлёт отрезок, цвет и время жизни (PhoneData 16); здесь рисуем аддитивной лентой крестом с ореолом.
 */
public final class Lines {
    private Lines() {}
    private record L(Vec3 a, Vec3 b, int color, float width, long born, long life, boolean laser) {}
    private static final java.util.List<L> LIST = new java.util.ArrayList<>();

    // ------------------------------------------------------------------ 0.10.3: шлейфы взмахов
    /** Полумесяц взмаха: центр, «вперёд» и «вбок» плоскости дуги, радиус, углы начала/конца, цвет. */
    /** o — смещение от глаз сущности ent в момент взмаха: шлейф едет вместе с ней (на бегу не остаётся позади). */
    private record S(Vec3 o, Vec3 f, Vec3 r, float rad, float a0, float a1, int color, long born, long life, float thick, int ent) {}
    /** 0.11 (бета): шлейф больше не привязан к игроку — в момент удара он «отрывается» и летит сам:
     *  своя скорость вперёд (куда смотрел игрок) + скорость игрока в момент удара. abs — точка отрыва, vel — блоков/мс. */
    private static final java.util.Map<S, Vec3[]> FREE = new java.util.IdentityHashMap<>();
    private static final double FLY = 0.0055;   // ≈5.5 блока/с вперёд
    private static final java.util.List<S> SLASH = new java.util.ArrayList<>();
    private static final java.util.Map<Integer, Integer> DIR = new java.util.HashMap<>();

    /** Взмах сущности оружием st (cls — класс оружия), видно всем. */
    public static void slash(net.minecraft.world.entity.LivingEntity e, net.minecraft.world.item.ItemStack st, boolean off) {
        int cls = st.getItem() instanceof com.pvpciv.item.CivWeaponItem w ? w.cls.ordinal() : 0;
        slash(e, cls, off, com.pvpciv.loot.Gear.rarity(st).color);
    }

    public static void slash(net.minecraft.world.entity.LivingEntity e, int cls, boolean off, int color) {
        int dir = -DIR.getOrDefault(e.getId() * 2 + (off ? 1 : 0), 1);
        DIR.put(e.getId() * 2 + (off ? 1 : 0), dir);
        Vec3 look = e.getLookAngle();
        Vec3 f = new Vec3(look.x, look.y * 0.5, look.z).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 r = f.cross(up).normalize();
        Vec3 u = r.cross(f).normalize();
        Vec3 o = e.getEyePosition().add(0, -0.1, 0).subtract(f.scale(0.35));
        float rad = 1.75f, a0 = -75, a1 = 75, thick = 0.38f;
        Vec3 side = r;
        switch (cls) {
            case 2 -> { rad = 1.35f; a0 = -18; a1 = 18; thick = 0.22f; side = r.scale(Math.cos(0.6)).add(u.scale(Math.sin(0.6) * dir)).normalize(); }   // кинжал: короткий росчерк
            case 3 -> { side = r.scale(Math.cos(0.8)).add(u.scale(Math.sin(0.8))).normalize(); a0 = -70; a1 = 70; rad = 1.9f; thick = 0.45f; }   // топор: диагональ
            case 4 -> { side = u.scale(-1); a0 = -95; a1 = 25; rad = 1.9f; thick = 0.55f; dir = 1; }   // молот: сверху вниз
            default -> { if (off) o = o.add(r.scale(-0.15)); }
        }
        if (dir < 0) { float t = a0; a0 = a1; a1 = t; }
        if (SLASH.size() > 60) FREE.remove(SLASH.remove(0));
        int life = cls == 2 ? 180 : cls == 4 ? 360 : 280;
        int delay = cls == 2 ? 20 : cls == 4 ? 140 : cls == 3 ? 75 : 55;   // шлейф — в момент удара, после замаха
        SLASH.add(new S(o.subtract(e.getEyePosition()), f, side, rad, a0, a1, color, System.currentTimeMillis() + delay, life, thick, e.getId()));
    }

    private static void renderSlashes(VertexConsumer vc, Matrix4f m, long now, float pt) {
        SLASH.removeIf(x -> { boolean dead = now - x.born > x.life; if (dead) FREE.remove(x); return dead; });
        var lvl = net.minecraft.client.Minecraft.getInstance().level;
        for (S x0 : SLASH) {
            if (now < x0.born) continue;
            Vec3[] fr = FREE.get(x0);
            if (fr == null) {   // момент удара: запоминаем точку и скорость, дальше шлейф летит сам
                var en = lvl == null ? null : lvl.getEntity(x0.ent);
                if (en == null) continue;
                Vec3 mv = en.position().subtract(en.xo, en.yo, en.zo);          // скорость игрока, блоков/тик (работает и для чужих игроков)
                if (mv.lengthSqr() < 1e-6) mv = en.getDeltaMovement().multiply(1, 0, 1);
                Vec3 vel = mv.scale(1 / 50.0).add(x0.f.scale(FLY));
                fr = new Vec3[]{en.getEyePosition(pt).add(x0.o), vel};
                FREE.put(x0, fr);
            }
            Vec3 cur = fr[0].add(fr[1].scale(now - x0.born));
            S x = new S(cur, x0.f, x0.r, x0.rad, x0.a0, x0.a1, x0.color, x0.born, x0.life, x0.thick, x0.ent);
            float t = (now - x.born) / (float) x.life;
            float head = Math.min(1f, t / 0.45f);           // кончик доходит до конца за 45% жизни
            float fade = t < 0.45f ? 1f : 1f - (t - 0.45f) / 0.55f;
            int seg = 22;
            float r = (x.color >> 16 & 255) / 255f, g = (x.color >> 8 & 255) / 255f, b = (x.color & 255) / 255f;
            for (int i = 0; i < seg; i++) {
                float s0 = i / (float) seg, s1 = (i + 1) / (float) seg;
                if (s1 > head) break;
                float ang0 = (float) Math.toRadians(x.a0 + (x.a1 - x.a0) * s0), ang1 = (float) Math.toRadians(x.a0 + (x.a1 - x.a0) * s1);
                // яркость: сильнее у кончика, хвост тает
                float k0 = Math.max(0, 1 - (head - s0) * 2.2f) * fade, k1 = Math.max(0, 1 - (head - s1) * 2.2f) * fade;
                float w0 = x.thick * (0.35f + 0.65f * (float) Math.sin(Math.PI * s0)), w1 = x.thick * (0.35f + 0.65f * (float) Math.sin(Math.PI * s1));
                Vec3 d0 = x.f.scale(Math.cos(ang0)).add(x.r.scale(Math.sin(ang0))), d1 = x.f.scale(Math.cos(ang1)).add(x.r.scale(Math.sin(ang1)));
                Vec3 o0 = x.o.add(d0.scale(x.rad)), i0 = x.o.add(d0.scale(x.rad - w0));
                Vec3 o1 = x.o.add(d1.scale(x.rad)), i1 = x.o.add(d1.scale(x.rad - w1));
                // внешняя кромка — светлее (почти белая), внутренняя — цвет редкости и прозрачнее
                quad2(vc, m, i0, o0, o1, i1, r, g, b, k0 * 0.25f, k1 * 0.25f, 0.95f, k0 * 0.9f, k1 * 0.9f);
                quad2(vc, m, i1, o1, o0, i0, r, g, b, k1 * 0.25f, k0 * 0.25f, 0.95f, k1 * 0.9f, k0 * 0.9f);   // обратная сторона
            }
        }
    }

    /** Четырёхугольник: in0→out0→out1→in1; у внешней кромки цвет смешан с белым (w). */
    private static void quad2(VertexConsumer vc, Matrix4f m, Vec3 in0, Vec3 out0, Vec3 out1, Vec3 in1, float r, float g, float b,
                              float aIn0, float aIn1, float w, float aOut0, float aOut1) {
        float rr = r + (1 - r) * w * 0.6f, gg = g + (1 - g) * w * 0.6f, bb = b + (1 - b) * w * 0.6f;
        v(vc, m, in0, r, g, b, aIn0); v(vc, m, out0, rr, gg, bb, aOut0); v(vc, m, out1, rr, gg, bb, aOut1); v(vc, m, in1, r, g, b, aIn1);
    }

    public static void add(Vec3 a, Vec3 b, int color, float width, int lifeMs, boolean laser) {
        if (LIST.size() > 400) LIST.remove(0);
        LIST.add(new L(a, b, color, width, System.currentTimeMillis(), lifeMs, laser));
    }

    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || (LIST.isEmpty() && SLASH.isEmpty())) return;
        long now = System.currentTimeMillis();
        LIST.removeIf(l -> now - l.born > l.life);
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        for (L l : LIST) {
            float t = (now - l.born) / (float) l.life;
            float a = l.laser ? (0.55f + 0.45f * (float) Math.sin(now / 45.0)) * Math.min(1f, (1 - t) * 3f) : 1 - t;
            float r = (l.color >> 16 & 255) / 255f, g = (l.color >> 8 & 255) / 255f, b = (l.color & 255) / 255f;
            ribbon(vc, m, l.a, l.b, l.width * 3f, r, g, b, a * 0.22f);   // ореол
            ribbon(vc, m, l.a, l.b, l.width, r, g, b, a * 0.9f);         // ядро
            ribbon(vc, m, l.a, l.b, l.width * 0.35f, 1, 1, 1, a * 0.7f);  // светлая сердцевина
        }
        renderSlashes(vc, m, now, e.getPartialTick().getGameTimeDeltaPartialTick(false));
        ps.popPose();
        buf.endBatch(RenderType.lightning());
    }

    private static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float w, float r, float g, float bl, float al) {
        Vec3 d = b.subtract(a);
        Vec3 n1 = d.cross(new Vec3(0, 1, 0));
        if (n1.lengthSqr() < 1e-6) n1 = d.cross(new Vec3(1, 0, 0));
        n1 = n1.normalize().scale(w);
        Vec3 n2 = d.cross(n1).normalize().scale(w);
        for (Vec3 n : new Vec3[]{n1, n2}) {
            v(vc, m, a.add(n), r, g, bl, al); v(vc, m, a.subtract(n), r, g, bl, al);
            v(vc, m, b.subtract(n), r, g, bl, al); v(vc, m, b.add(n), r, g, bl, al);
            v(vc, m, b.add(n), r, g, bl, al); v(vc, m, b.subtract(n), r, g, bl, al);
            v(vc, m, a.subtract(n), r, g, bl, al); v(vc, m, a.add(n), r, g, bl, al);
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Math.max(0, Math.min(1, a)));
    }
}
