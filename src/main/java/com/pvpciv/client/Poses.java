package com.pvpciv.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.2: позы рук на клиенте. Карманный телепорт: за время каста руки идут от «вдоль тела» (−80°)
 * через T-позу к +45° над горизонтом и держатся там до конца.
 */
public final class Poses {
    private Poses() {}

    private record Pose(long start, long len) {}
    private static final Map<Integer, Pose> POSES = new HashMap<>();

    public static void set(int entityId, int ticks) {
        if (ticks <= 0) POSES.remove(entityId);
        else POSES.put(entityId, new Pose(System.currentTimeMillis(), ticks * 50L));
    }

    /** Подъём рук 0..1 (или -1 — позы нет). */
    public static float progress(Entity e) {
        Pose p = POSES.get(e.getId());
        if (p == null) return -1;
        long el = System.currentTimeMillis() - p.start;
        if (el > p.len + 6_000) { POSES.remove(e.getId()); return -1; }   // окно телепорта открыто — руки ещё немного подняты
        return Math.min(1f, el / (float) p.len);
    }

    public static void apply(Entity e, ModelPart right, ModelPart left) {
        float k = progress(e);
        if (k < 0) return;
        float ease = k < 0.5f ? 2 * k * k : 1 - (float) Math.pow(-2 * k + 2, 2) / 2;
        float deg = 10 + 125 * ease;   // от тела: 10° (−80° к горизонту) → 135° (+45°)
        float rad = (float) Math.toRadians(deg);
        right.xRot = 0; right.yRot = 0; right.zRot = rad;
        left.xRot = 0; left.yRot = 0; left.zRot = -rad;
    }

    /** 1.4: ползком (Z) — «по-пластунски»: руки тянутся вперёд по очереди, ноги толкаются. */
    public static void crawl(net.minecraft.world.entity.LivingEntity e, float ls, float amt, ModelPart ra, ModelPart la, ModelPart rl, ModelPart ll) {
        if (!e.isVisuallyCrawling()) return;
        float s = (float) Math.sin(ls * 0.75f), k = Math.min(1f, amt * 1.6f) + 0.15f;
        ra.xRot = -2.7f + 0.55f * s * k; ra.yRot = 0; ra.zRot = 0.35f + 0.25f * Math.max(0, s) * k;
        la.xRot = -2.7f - 0.55f * s * k; la.yRot = 0; la.zRot = -0.35f - 0.25f * Math.max(0, -s) * k;
        rl.xRot = 0.35f * s * k; rl.zRot = 0.12f;
        ll.xRot = -0.35f * s * k; ll.zRot = -0.12f;
    }
}
