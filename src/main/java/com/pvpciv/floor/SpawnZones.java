package com.pvpciv.floor;

import com.pvpciv.combat.Mobs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Зоны, которые постоянно поддерживают популяцию мобов нужного уровня. Работают, только когда рядом игрок. */
public final class SpawnZones {
    /** y ≥ 0 — фиксированный уровень пола (город, канализация), иначе по карте высот; f2 — этаж 2. */
    public record Zone(String id, BlockPos center, int radius, int max, int minLvl, int maxLvl, float elite, int y, boolean f2, Mobs.Kind... kinds) {
        public Zone(String id, BlockPos center, int radius, int max, int minLvl, int maxLvl, Mobs.Kind... kinds) {
            this(id, center, radius, max, minLvl, maxLvl, 0f, -1, false, kinds);   // элиты теперь на постоянных точках (EliteSpots)
        }
        public Zone(String id, BlockPos center, int radius, int max, int minLvl, int maxLvl, float elite, Mobs.Kind... kinds) {
            this(id, center, radius, max, minLvl, maxLvl, elite, -1, false, kinds);
        }
        public static Zone f2(String id, BlockPos center, int radius, int max, int minLvl, int maxLvl, Mobs.Kind... kinds) {
            return new Zone(id, center, radius, max, minLvl, maxLvl, 0f, center.getY(), true, kinds);
        }
    }

    private static List<Zone> all;
    /** Время (gameTime), до которого «занят» слот убитого моба: респавн 20–30 с по уровню. */
    private static final java.util.Map<String, java.util.List<Long>> PENDING = new java.util.HashMap<>();

    /** Моб из зоны убит — его место освободится через 20–30 секунд. */
    public static void onDeath(Mob m) {
        for (String t : m.getTags()) if (t.startsWith("civzone_")) {
            long delay = 20L * Math.min(30, 20 + 2L * Math.max(0, Mobs.level(m) - 1));
            PENDING.computeIfAbsent(t.substring(8), k -> new java.util.ArrayList<>()).add(m.level().getGameTime() + delay);
        }
    }

    public static List<Zone> zones() {
        if (all == null) {
            all = new ArrayList<>(Floor1.ZONES);
            all.addAll(FloorNorth.ZONES);
            all.addAll(Floor2.ZONES);
            all.addAll(Floor3Areas.ZONES);
            all.addAll(Floor4.ZONES);
        }
        return all;
    }

    public static void tick(MinecraftServer server) {
        ServerLevel w1 = com.pvpciv.world.CivWorld.level(server), f2 = com.pvpciv.world.CivWorld.f2(server), f3 = com.pvpciv.world.CivWorld.f3(server), f4 = com.pvpciv.world.CivWorld.f4(server);
        for (Zone z : zones()) {
            ServerLevel lvl = z.id().startsWith("f4") ? f4 : z.id().startsWith("f3") ? f3 : z.f2() ? f2 : w1;
            int maxN = (int) Math.ceil(z.max() * 1.3);   // бета 11.11: спавнов ×1.3
            int baseY = z.y() >= 0 ? z.y() : Floor1.Y;
            Vec3 c = Vec3.atBottomCenterOf(z.center());
            boolean playerNear = false;
            for (ServerPlayer p : lvl.players()) if (!p.isSpectator() && p.position().distanceTo(c) < z.radius() + 44) { playerNear = true; break; }
            String tag = "civzone_" + z.id();
            List<Mob> mobs = lvl.getEntitiesOfClass(Mob.class, new AABB(z.center()).inflate(z.radius() + 40, 40, z.radius() + 40), m -> m.getTags().contains(tag));
            for (Mob m : mobs) {
                if (m.position().distanceTo(c) > z.radius() + 28 || m.getY() < baseY - 20) m.discard();
            }
            long now = lvl.getGameTime();
            java.util.List<Long> pend = PENDING.getOrDefault(z.id(), java.util.List.of());
            if (!pend.isEmpty()) pend.removeIf(t -> t <= now);
            if (!playerNear || mobs.size() + pend.size() >= maxN) continue;
            if (lvl.random.nextFloat() < 0.5f) continue;   // не все зоны сразу: популяция восстанавливается плавно
            for (int attempt = 0; attempt < 6; attempt++) {
                double a = lvl.random.nextDouble() * Math.PI * 2, r = Math.sqrt(lvl.random.nextDouble()) * z.radius();
                int x = (int) Math.floor(c.x + Math.cos(a) * r), zz = (int) Math.floor(c.z + Math.sin(a) * r);
                int y;
                if (z.y() >= 0) {
                    y = z.y();
                    BlockPos bp = new BlockPos(x, y, zz);
                    if (lvl.getBlockState(bp.below()).getCollisionShape(lvl, bp.below()).isEmpty() || !lvl.getBlockState(bp).getCollisionShape(lvl, bp).isEmpty()
                            || !lvl.getBlockState(bp.above()).getCollisionShape(lvl, bp.above()).isEmpty() || !lvl.getFluidState(bp).isEmpty()) continue;
                } else {
                    y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, zz);
                    if (y < Floor1.Y - 4 || y > Floor1.Y + 6) continue;
                }
                Vec3 pos = new Vec3(x + 0.5, y, zz + 0.5);
                boolean tooClose = false;
                for (ServerPlayer p : lvl.players()) if (p.position().distanceTo(pos) < 12) { tooClose = true; break; }
                if (tooClose) continue;
                Mobs.Kind kind = z.kinds()[lvl.random.nextInt(z.kinds().length)];
                int level = z.minLvl() + lvl.random.nextInt(z.maxLvl() - z.minLvl() + 1);
                boolean elite = lvl.random.nextFloat() < z.elite();
                Mobs.spawn(lvl, kind, level, elite, pos, kind.loot, tag);
                break;
            }
        }
    }
}
