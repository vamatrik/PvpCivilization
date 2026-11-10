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
    public record Zone(String id, BlockPos center, int radius, int max, int minLvl, int maxLvl, float elite, Mobs.Kind... kinds) {
        public Zone(String id, BlockPos center, int radius, int max, int minLvl, int maxLvl, Mobs.Kind... kinds) {
            this(id, center, radius, max, minLvl, maxLvl, 0.07f, kinds);
        }
    }

    private static List<Zone> all;

    public static List<Zone> zones() {
        if (all == null) {
            all = new ArrayList<>(Floor1.ZONES);
            all.addAll(FloorNorth.ZONES);
        }
        return all;
    }

    public static void tick(MinecraftServer server) {
        ServerLevel lvl = com.pvpciv.world.CivWorld.level(server);
        for (Zone z : zones()) {
            Vec3 c = Vec3.atBottomCenterOf(z.center());
            boolean playerNear = false;
            for (ServerPlayer p : lvl.players()) if (!p.isSpectator() && p.position().distanceTo(c) < z.radius() + 44) { playerNear = true; break; }
            String tag = "civzone_" + z.id();
            List<Mob> mobs = lvl.getEntitiesOfClass(Mob.class, new AABB(z.center()).inflate(z.radius() + 40, 40, z.radius() + 40), m -> m.getTags().contains(tag));
            for (Mob m : mobs) {
                if (m.position().distanceTo(c) > z.radius() + 28 || m.getY() < Floor1.Y - 20) m.discard();
            }
            if (!playerNear || mobs.size() >= z.max()) continue;
            if (lvl.random.nextFloat() < 0.5f) continue;   // не все зоны сразу: популяция восстанавливается плавно
            for (int attempt = 0; attempt < 6; attempt++) {
                double a = lvl.random.nextDouble() * Math.PI * 2, r = Math.sqrt(lvl.random.nextDouble()) * z.radius();
                int x = (int) Math.floor(c.x + Math.cos(a) * r), zz = (int) Math.floor(c.z + Math.sin(a) * r);
                int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, zz);
                if (y < Floor1.Y - 4 || y > Floor1.Y + 6) continue;
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
