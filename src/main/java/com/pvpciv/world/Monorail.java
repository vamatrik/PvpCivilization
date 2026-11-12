package com.pvpciv.world;

import com.pvpciv.entity.LiftCabin;
import com.pvpciv.floor.Floor2;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Монорельс Небесного лифта: 3D-сегменты (ItemDisplay с моделью monorail_segment, повёрнутые вдоль пути)
 * и станционные кабины. Путь на крыше — наискосок на север в небо; на этаже 2 — почти вертикально, из бездны в небо.
 */
public final class Monorail {
    public static final String TAG = "pvpciv_rail";
    public static final Vec3 S1 = new Vec3(0.5, RoofBuilder.RY, -26.5), S2 = new Vec3(0.5, Floor2.Y, 0.5);

    /** Точка пути на крыше (t ≥ 0 — вверх по рельсу). */
    public static Vec3 roof(double t) { return new Vec3(0.5, RoofBuilder.RY + t, -26.5 - 0.6 * t); }
    /** Точка пути на этаже 2 (s < 0 — ниже станции, в бездне). */
    public static Vec3 f2(double s) { return new Vec3(0.5, Floor2.Y + s, 0.5 + 0.12 * s); }

    public static List<Vec3> roofPath(double from, double to) { List<Vec3> l = new ArrayList<>(); int n = 24; for (int i = 0; i <= n; i++) l.add(roof(from + (to - from) * i / n)); return l; }
    public static List<Vec3> f2Path(double from, double to) { List<Vec3> l = new ArrayList<>(); int n = 24; for (int i = 0; i <= n; i++) l.add(f2(from + (to - from) * i / n)); return l; }

    public static void ensure(MinecraftServer server) {
        ServerLevel w1 = CivWorld.level(server), l2 = CivWorld.f2(server);
        if (loaded(w1, S1)) {
            station(w1, S1);
            // рельс начинается над крышей зала: нижние сегменты проходили сквозь кабину (бета 12.11)
            for (var e : w1.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(-3, RoofBuilder.RY - 2, -38, 4, RoofBuilder.RY + 10.5, -24), e -> e.getTags().contains(TAG))) e.discard();
            if (w1.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(S1.x - 8, S1.y - 8, S1.z - 12, S1.x + 8, S1.y + 16, S1.z + 4), e -> e.getTags().contains(TAG)).isEmpty())
                for (double t = 12; t < 170; t += 6) segment(w1, roof(t).add(0, -0.45, 0), roof(t + 6).add(0, -0.45, 0));
        }
        if (CivWorld.isF2(l2) && loaded(l2, S2)) {
            station(l2, S2);
            if (l2.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(S2.x - 8, S2.y - 10, S2.z - 8, S2.x + 8, S2.y + 10, S2.z + 8), e -> e.getTags().contains(TAG)).isEmpty())
                for (double s = -80; s < 160; s += 6) segment(l2, f2(s).add(0, 1.4, -1.9), f2(s + 6).add(0, 1.4, -1.9));
        }
    }

    private static boolean loaded(ServerLevel l, Vec3 p) { return l.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(net.minecraft.core.BlockPos.containing(p))); }

    private static void station(ServerLevel l, Vec3 at) {
        var list = l.getEntitiesOfClass(LiftCabin.class, new AABB(at, at).inflate(3), c -> !c.ride());
        for (int i = 1; i < list.size(); i++) list.get(i).discard();
        if (!list.isEmpty()) return;
        LiftCabin c = ModRegistry.LIFT_CABIN.get().create(l);
        if (c == null) return;
        c.moveTo(at.x, at.y, at.z, 0, 0);
        l.addFreshEntity(c);
    }

    /** Один 3D-сегмент между точками a и b. */
    private static void segment(ServerLevel l, Vec3 a, Vec3 b) {
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(l);
        if (d == null) return;
        Vec3 mid = a.add(b).scale(0.5), dir = b.subtract(a);
        double len = dir.length();
        dir = dir.normalize();
        // поворот +Z → dir: q = (cross(z, d), 1 + dot(z, d))
        double qx = -dir.y, qy = dir.x, qz = 0, qw = 1 + dir.z, n = Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);
        CompoundTag t = new CompoundTag();
        CompoundTag item = new CompoundTag();
        item.putString("id", "pvpciv:monorail_segment");
        item.putInt("count", 1);
        t.put("item", item);
        CompoundTag tr = new CompoundTag();
        tr.put("left_rotation", floats(qx / n, qy / n, qz / n, qw / n));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tr.put("translation", floats(0, 0, 0));
        tr.put("scale", floats(1.8, 1.8, len + 0.05));
        t.put("transformation", tr);
        t.putFloat("view_range", 6f);
        CompoundTag br = new CompoundTag(); br.putInt("sky", 15); br.putInt("block", 15);
        t.put("brightness", br);
        d.load(withPos(t, mid));
        d.addTag(TAG);
        l.addFreshEntity(d);
    }

    private static CompoundTag withPos(CompoundTag t, Vec3 p) {
        ListTag pos = new ListTag();
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(p.x)); pos.add(net.minecraft.nbt.DoubleTag.valueOf(p.y)); pos.add(net.minecraft.nbt.DoubleTag.valueOf(p.z));
        t.put("Pos", pos);
        t.putString("id", "minecraft:item_display");
        return t;
    }

    private static ListTag floats(double... v) { ListTag l = new ListTag(); for (double x : v) l.add(FloatTag.valueOf((float) x)); return l; }
}
