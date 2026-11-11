package com.pvpciv.floor;

import com.pvpciv.world.Basement;
import com.pvpciv.world.DormBuilder;
import com.pvpciv.world.IceTrack;
import net.minecraft.world.phys.Vec3;

/** Зоны мира: безопасные, PvE и PvP. */
public final class Zones {
    public enum Type { SAFE, PVE, PVP }
    public record Zone(String name, Type type) {}

    public static final Zone HUB = new Zone("Общежитие", Type.SAFE);
    public static final Zone BASEMENT = new Zone("Подвал Кармиллы", Type.SAFE);
    public static final Zone ROAD = new Zone("Ледяной Тракт", Type.SAFE);
    public static final Zone TRACK = new Zone("Ледяная Трасса", Type.SAFE);
    public static final Zone FLOOR1 = new Zone("Этаж 1 · Пепельная пустошь", Type.PVE);
    public static final Zone ARENA = new Zone("Этаж 1 · Пепельная Арена", Type.PVE);
    public static final Zone LAIR = new Zone("Этаж 1 · Логово Громилы", Type.PVP);
    public static final Zone FLOOR1B = new Zone("Этаж 1 · Инеевый Кряж", Type.PVE);
    public static final Zone CRYPT = new Zone("Этаж 1 · Ледяной Склеп", Type.PVP);
    public static final Zone ABYSS = new Zone("Бездна", Type.SAFE);

    /** С учётом измерения (этаж 2 — своё небо). */
    public static Zone at(net.minecraft.world.entity.Entity e) {
        if (com.pvpciv.world.CivWorld.isF2(e.level())) return Floor2.zone(e.position());
        return at(e.position());
    }

    public static Zone at(Vec3 p) {
        if (Basement.contains(p)) return BASEMENT;
        if (Math.max(Math.abs(p.x), Math.abs(p.z)) <= 33 && p.y > DormBuilder.Y0 - 4) return HUB;
        if (p.z > 31 && p.z <= 40 && p.x > -5 && p.x < 11) return HUB;
        if (p.z < -31 && p.z >= -40 && p.x > -5 && p.x < 11) return HUB;
        if (p.z > 40 && p.z < Floor1.ROAD_END && p.x > -4 && p.x < 10) return ROAD;
        if (dist(p, Floor1.LAIR) <= Floor1.LAIR_R + 2) return LAIR;
        if (dist(p, Floor1.ARENA) <= 21) return ARENA;
        int bx = (int) Math.floor(p.x), bz = (int) Math.floor(p.z);
        if (p.z >= Floor1.Z0 && Floor1.inside(bx, bz)) {
            for (Floor1.Area a : Floor1.AREAS) if (dist(p, a.c()) <= a.r()) return new Zone(FLOOR1.name() + " · " + a.name(), Type.PVE);
            return FLOOR1;
        }
        if (FloorNorth.inside(bx, bz)) {
            if (dist(p, FloorNorth.CRYPT) <= FloorNorth.CRYPT_R + 2) return CRYPT;
            for (Floor1.Area a : FloorNorth.AREAS) if (dist(p, a.c()) <= a.r()) return new Zone(FLOOR1B.name() + " · " + a.name(), Type.PVE);
            return FLOOR1B;
        }
        if (IceTrack.near(p, 6)) return TRACK;
        return ABYSS;
    }

    private static double dist(Vec3 p, net.minecraft.core.BlockPos c) {
        double dx = p.x - c.getX() - 0.5, dz = p.z - c.getZ() - 0.5;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
