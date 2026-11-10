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

import java.util.List;

/** Зоны, которые постоянно поддерживают популяцию мобов нужного уровня. */
public final class SpawnZones {
    public record Zone(String id, BlockPos center, int radius, int max, int minLvl, int maxLvl, Mobs.Kind... kinds) {}

    public static final List<Zone> ZONES = List.of(
            new Zone("f1_west", new BlockPos(-30, Floor1.Y, 420), 13, 6, 1, 2, Mobs.Kind.ASH_ZOMBIE, Mobs.Kind.WITHERED),
            new Zone("f1_east", new BlockPos(18, Floor1.Y, 398), 10, 5, 1, 2, Mobs.Kind.ASH_ZOMBIE, Mobs.Kind.ASH_SPIDER),
            new Zone("f1_south", new BlockPos(-25, Floor1.Y, 452), 11, 6, 2, 3, Mobs.Kind.WITHERED, Mobs.Kind.ASH_ARCHER),
            new Zone("f1_ridge", new BlockPos(30, Floor1.Y, 466), 10, 5, 2, 3, Mobs.Kind.ASH_ARCHER, Mobs.Kind.ASH_SPIDER, Mobs.Kind.ASH_ZOMBIE)
    );

    public static void tick(MinecraftServer server) {
        ServerLevel lvl = server.overworld();
        for (Zone z : ZONES) {
            Vec3 c = Vec3.atBottomCenterOf(z.center());
            boolean playerNear = false;
            for (ServerPlayer p : lvl.players()) if (!p.isSpectator() && p.position().distanceTo(c) < 56) { playerNear = true; break; }
            String tag = "civzone_" + z.id();
            List<Mob> mobs = lvl.getEntitiesOfClass(Mob.class, new AABB(z.center()).inflate(z.radius() + 40, 40, z.radius() + 40), m -> m.getTags().contains(tag));
            for (Mob m : mobs) {
                if (m.position().distanceTo(c) > z.radius() + 28 || m.getY() < Floor1.Y - 20) m.discard();
            }
            if (!playerNear || mobs.size() >= z.max()) continue;
            for (int attempt = 0; attempt < 6; attempt++) {
                double a = lvl.random.nextDouble() * Math.PI * 2, r = Math.sqrt(lvl.random.nextDouble()) * z.radius();
                int x = (int) Math.floor(c.x + Math.cos(a) * r), zz = (int) Math.floor(c.z + Math.sin(a) * r);
                int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, zz);
                if (y < Floor1.Y - 4 || y > Floor1.Y + 6) continue;
                Vec3 pos = new Vec3(x + 0.5, y, zz + 0.5);
                boolean tooClose = false;
                for (ServerPlayer p : lvl.players()) if (p.position().distanceTo(pos) < 10) { tooClose = true; break; }
                if (tooClose) continue;
                Mobs.Kind kind = z.kinds()[lvl.random.nextInt(z.kinds().length)];
                int level = z.minLvl() + lvl.random.nextInt(z.maxLvl() - z.minLvl() + 1);
                boolean elite = lvl.random.nextFloat() < 0.08f;
                Mobs.spawn(lvl, kind, level, elite, pos, "floor1_mob", tag);
                break;
            }
        }
    }
}
