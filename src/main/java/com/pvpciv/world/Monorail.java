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
    public static final Vec3 S1 = new Vec3(0.5, RoofBuilder.RY, -26.5), S2 = new Vec3(0.5, Floor2.Y, 0.5), S3 = new Vec3(0.5, com.pvpciv.floor.Floor3.Y, 0.5);

    /** Точка пути на крыше (t ≥ 0 — вверх по рельсу). */
    public static Vec3 roof(double t) { return new Vec3(0.5, RoofBuilder.RY + t, -26.5 - 0.6 * t); }
    /** Точка пути на этаже 2 (s < 0 — ниже станции, в бездне). */
    public static Vec3 f2(double s) { return new Vec3(0.5, Floor2.Y + s, 0.5 + 0.12 * s); }

    /** Этаж 3: кабина поднимается из бездны прямо в стеклянный колодец библиотеки. */
    public static Vec3 f3(double s) { return new Vec3(0.5, com.pvpciv.floor.Floor3.Y + s, 0.5 + 0.05 * Math.min(0, s)); }
    public static List<Vec3> f3Path(double from, double to) { List<Vec3> l = new ArrayList<>(); int n = 24; for (int i = 0; i <= n; i++) l.add(f3(from + (to - from) * i / n)); return l; }
    /** Отъезд: вверх — в небо, вниз — в бездну (бета 0.9.6: со 2 на 3 этаж кабина ехала вниз). */
    /** Этаж 4: кабина поднимается из бездны в стеклянную шахту вестибюля NaZa. */
    public static final Vec3 S4 = new Vec3(0.5, com.pvpciv.floor.Floor4.Y, 0.5);
    public static Vec3 f4(double s) { return new Vec3(0.5, com.pvpciv.floor.Floor4.Y + s, 0.5); }
    public static List<Vec3> f4Path(double from, double to) { List<Vec3> l = new ArrayList<>(); int n = 24; for (int i = 0; i <= n; i++) l.add(f4(from + (to - from) * i / n)); return l; }
    /** 1.1: станция «Орбита» — кабина в шахте атриума, рельс уходит вниз. */
    public static Vec3 S5() { var c = com.pvpciv.floor.Floor5.HUB.center(); return new Vec3(c.getX() + 0.5, com.pvpciv.floor.Floor5.Y, c.getZ() + 0.5); }
    public static Vec3 f5(double s) { return S5().add(0, s, 0); }
    public static List<Vec3> f5Path(double from, double to) { List<Vec3> l = new ArrayList<>(); int n = 24; for (int i = 0; i <= n; i++) l.add(f5(from + (to - from) * i / n)); return l; }
    public static List<Vec3> pathFrom(int floor, boolean up) { return floor == 1 ? roofPath(0, 110) : floor == 2 ? f2Path(0, up ? 60 : -60) : floor == 3 ? f3Path(0, up ? 60 : -60) : floor == 5 ? f5Path(0, -50) : f4Path(0, up ? 60 : -60); }
    /** Прибытие: поднимаясь — из бездны снизу, спускаясь — с неба сверху. */
    public static List<Vec3> pathTo(int floor, boolean up) { return floor == 1 ? roofPath(110, 0) : floor == 2 ? f2Path(up ? -60 : 60, 0) : floor == 3 ? f3Path(up ? -60 : 60, 0) : floor == 5 ? f5Path(-50, 0) : f4Path(up ? -60 : 60, 0); }

    public static List<Vec3> roofPath(double from, double to) { List<Vec3> l = new ArrayList<>(); int n = 24; for (int i = 0; i <= n; i++) l.add(roof(from + (to - from) * i / n)); return l; }
    public static List<Vec3> f2Path(double from, double to) { List<Vec3> l = new ArrayList<>(); int n = 24; for (int i = 0; i <= n; i++) l.add(f2(from + (to - from) * i / n)); return l; }

    /** Станция этажа (где стоит кабина). */
    public static Vec3 station(int floor) { return switch (floor) { case 1 -> S1; case 2 -> S2; case 3 -> S3; case 4 -> S4; default -> S5(); }; }

    /** 1.2: на время поездки стоячая кабина уезжает вниз (не видна рядом с едущей), потом ensure вернёт её. */
    public static void hideStation(ServerLevel l, Vec3 at) {
        for (LiftCabin c : l.getEntitiesOfClass(LiftCabin.class, new AABB(at, at).inflate(3), c -> !c.ride())) c.teleportTo(at.x, at.y - 300, at.z);
        HIDDEN.put(l.dimension().location() + "@" + (int) at.x + "," + (int) at.z, l.getGameTime() + 200);
    }
    private static final java.util.Map<String, Long> HIDDEN = new java.util.HashMap<>();

    /**
     * 1.2: рельс проверяется посегментно — пропавший сегмент (с 1.1 на крыше пропадала большая часть пути) ставится заново.
     * Сегмент a→b есть, если рядом с его серединой стоит наш дисплей.
     */
    private static void seg(ServerLevel l, Vec3 a, Vec3 b) {
        Vec3 mid = a.add(b).scale(0.5);
        if (!loaded(l, mid)) return;
        if (l.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(mid, mid).inflate(0.6), e -> e.getTags().contains(TAG)).isEmpty()) segment(l, a, b);
    }

    public static void ensure(MinecraftServer server) {
        ServerLevel w1 = CivWorld.level(server), l2 = CivWorld.f2(server);
        if (loaded(w1, S1)) {
            station(w1, S1);
            seg(w1, roof(3.2).add(0, -0.45, 0), roof(12).add(0, -0.45, 0));
        }
        for (double t = 12; t < 170; t += 6) seg(w1, roof(t).add(0, -0.45, 0), roof(t + 6).add(0, -0.45, 0));
        if (CivWorld.isF2(l2)) {
            if (loaded(l2, S2)) station(l2, S2);
            for (double s = -80; s < 160; s += 6) seg(l2, f2(s).add(0, 1.4, -1.9), f2(s + 6).add(0, 1.4, -1.9));
        }
    }

    public static void ensure3(MinecraftServer server) {
        ServerLevel l3 = CivWorld.f3(server);
        if (!CivWorld.isF3(l3)) return;
        if (loaded(l3, S3)) station(l3, S3);
        for (double s = -80; s < -6; s += 6) seg(l3, f3(s).add(0, 1.4, -1.9), f3(s + 6).add(0, 1.4, -1.9));
        // 1.2: рельс и вверх — сквозь стеклянный колодец библиотеки
        for (double s = 6; s < 66; s += 6) seg(l3, f3(s).add(0, 1.4, -1.9), f3(s + 6).add(0, 1.4, -1.9));
    }

    public static void ensure5(MinecraftServer server) {
        ServerLevel l5 = CivWorld.f5(server);
        Vec3 s5 = S5();
        if (l5 == null || !com.pvpciv.game.LiftShaft.isOpen(server)) return;
        if (loaded(l5, s5)) station(l5, s5);
        for (double s = -44; s < -2; s += 6) seg(l5, f5(s).add(0, 1.4, -1.9), f5(Math.min(-1.2, s + 6)).add(0, 1.4, -1.9));
    }

    public static void ensure4(MinecraftServer server) {
        ServerLevel l4 = CivWorld.f4(server);
        if (!CivWorld.isF4(l4)) return;
        if (loaded(l4, S4)) station(l4, S4);
        for (double s = -80; s < -6; s += 6) seg(l4, f4(s).add(0, 1.4, -1.9), f4(s + 6).add(0, 1.4, -1.9));
        // 1.5.3: рельс доходит до кабины вплотную (был зазор −6..+6 у станции)
        seg(l4, f4(-6).add(0, 1.4, -1.9), f4(0).add(0, 1.4, -1.9));
        seg(l4, f4(0).add(0, 1.4, -1.9), f4(6).add(0, 1.4, -1.9));
        // 1.2: вверх — к станции «Орбита». Пока лифт станции не починен — обломанный, загнутый конец
        if (com.pvpciv.game.LiftShaft.isOpen(server)) {
            for (var e : l4.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(S4.x - 4, S4.y + 5, S4.z - 6, S4.x + 4, S4.y + 20, S4.z + 4), e -> e.getTags().contains(TAG + "_broken"))) e.discard();
            for (double s = 6; s < 66; s += 6) seg(l4, f4(s).add(0, 1.4, -1.9), f4(s + 6).add(0, 1.4, -1.9));
        } else if (loaded(l4, S4) && l4.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(S4.x - 4, S4.y + 5, S4.z - 6, S4.x + 4, S4.y + 20, S4.z + 4), e -> e.getTags().contains(TAG + "_broken")).isEmpty()) {
            Vec3 a = f4(6).add(0, 1.4, -1.9), b = f4(12).add(0, 1.4, -1.9), c = b.add(1.2, 2.2, 0.9);
            segment(l4, a, b).addTag(TAG + "_broken");
            segment(l4, b, c).addTag(TAG + "_broken");
        }
    }

    private static boolean loaded(ServerLevel l, Vec3 p) { return l.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(net.minecraft.core.BlockPos.containing(p))); }

    private static void station(ServerLevel l, Vec3 at) {
        Long h = HIDDEN.get(l.dimension().location() + "@" + (int) at.x + "," + (int) at.z);
        boolean riding = !l.getEntitiesOfClass(LiftCabin.class, new AABB(at, at).inflate(4), LiftCabin::ride).isEmpty();
        if (riding || (h != null && h > l.getGameTime())) return;   // идёт поездка — стоячая кабина спрятана
        for (LiftCabin c : l.getEntitiesOfClass(LiftCabin.class, new AABB(at.x - 3, at.y - 305, at.z - 3, at.x + 3, at.y - 295, at.z + 3), c -> !c.ride())) c.teleportTo(at.x, at.y, at.z);
        var list = l.getEntitiesOfClass(LiftCabin.class, new AABB(at, at).inflate(3), c -> !c.ride());
        for (int i = 1; i < list.size(); i++) list.get(i).discard();
        if (!list.isEmpty()) return;
        LiftCabin c = ModRegistry.LIFT_CABIN.get().create(l);
        if (c == null) return;
        c.moveTo(at.x, at.y, at.z, 0, 0);
        l.addFreshEntity(c);
    }

    /** Один 3D-сегмент между точками a и b. */
    private static Display.ItemDisplay segment(ServerLevel l, Vec3 a, Vec3 b) {
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(l);
        if (d == null) return null;
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
        return d;
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
