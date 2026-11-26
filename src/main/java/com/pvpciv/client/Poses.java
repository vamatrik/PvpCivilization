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
}
