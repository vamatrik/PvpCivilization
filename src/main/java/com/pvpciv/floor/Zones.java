package com.pvpciv.floor;

import net.minecraft.world.phys.Vec3;

/** Зоны мира: безопасные, PvE и PvP. */
public final class Zones {
    public enum Type { SAFE, PVE, PVP }
    public record Zone(String name, Type type) {}

    public static final Zone HUB = new Zone("Общежитие", Type.SAFE);
    public static final Zone ROAD = new Zone("Ледяной Тракт", Type.SAFE);
    public static final Zone FLOOR1 = new Zone("Этаж 1 · Пепельная пустошь", Type.PVE);
    public static final Zone ARENA = new Zone("Этаж 1 · Пепельная Арена", Type.PVE);
    public static final Zone LAIR = new Zone("Этаж 1 · Логово Громилы", Type.PVP);
    public static final Zone ABYSS = new Zone("Бездна", Type.SAFE);

    public static Zone at(Vec3 p) {
        if (Math.max(Math.abs(p.x), Math.abs(p.z)) <= 33 || (p.z > 31 && p.z <= 40 && p.x > -5 && p.x < 11)) return HUB;
        if (p.z > 40 && p.z < Floor1.ROAD_END && p.x > -4 && p.x < 10) return ROAD;
        if (dist(p, Floor1.LAIR) <= Floor1.LAIR_R + 2) return LAIR;
        if (dist(p, Floor1.ARENA) <= 21) return ARENA;
        if (dist(p, Floor1.CENTER) <= Floor1.R + 4) return FLOOR1;
        return ABYSS;
    }

    private static double dist(Vec3 p, net.minecraft.core.BlockPos c) {
        double dx = p.x - c.getX() - 0.5, dz = p.z - c.getZ() - 0.5;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
