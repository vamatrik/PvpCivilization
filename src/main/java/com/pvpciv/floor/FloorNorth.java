package com.pvpciv.floor;

import com.pvpciv.combat.Mobs;
import com.pvpciv.world.Builder;
import com.pvpciv.world.DormBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Этаж 1, локация 2 «Инеевый Кряж» (тир 1+) — к северу от общежития, в конце Ледяной Трассы.
 * Мобы 3–5 уровня, броня, оружие 1+, лодка тира 2. Застава, Замёрзшее озеро, Ледяные Шпили, Снежные руины,
 * Хвойное кладбище и Ледяной Склеп (PvP-зона с элитной стражей).
 */
public final class FloorNorth extends Builder {
    public static final int Y = DormBuilder.Y0;
    public static final int X0 = -77, X1 = 83, Z0 = -600, Z1 = -381;
    public static final BlockPos OUTPOST = new BlockPos(3, Y, -405);
    public static final BlockPos LAKE = new BlockPos(-36, Y, -470), SPIRES = new BlockPos(46, Y, -468),
            SNOW_RUINS = new BlockPos(46, Y, -545), PINES = new BlockPos(-45, Y, -552), CRYPT = new BlockPos(3, Y, -575);
    public static final int CRYPT_R = 13;
    public static final List<BlockPos> STASHES = new ArrayList<>();
    public static final List<BlockPos> CRYPT_STASHES = new ArrayList<>();

    public static final List<Floor1.Area> AREAS = List.of(
            new Floor1.Area("Застава", OUTPOST, 9),
            new Floor1.Area("Замёрзшее озеро", LAKE, 22),
            new Floor1.Area("Ледяные Шпили", SPIRES, 26),
            new Floor1.Area("Снежные руины", SNOW_RUINS, 24),
            new Floor1.Area("Хвойное кладбище", PINES, 26));

    public static final List<SpawnZones.Zone> ZONES = List.of(
            new SpawnZones.Zone("f1b_gate", new BlockPos(3, Y, -432), 15, 3, 3, 3, Mobs.Kind.FROST_ZOMBIE, Mobs.Kind.FROST_SPIDER),
            new SpawnZones.Zone("f1b_lake", LAKE, 18, 3, 3, 4, Mobs.Kind.FROST_ZOMBIE, Mobs.Kind.FROST_ARCHER),
            new SpawnZones.Zone("f1b_spires", SPIRES, 20, 3, 3, 4, Mobs.Kind.FROST_SPIDER, Mobs.Kind.FROST_ZOMBIE),
            new SpawnZones.Zone("f1b_mid", new BlockPos(3, Y, -500), 18, 4, 4, 4, Mobs.Kind.FROST_ARCHER, Mobs.Kind.FROST_ZOMBIE, Mobs.Kind.FROST_SPIDER),
            new SpawnZones.Zone("f1b_pines", PINES, 20, 3, 4, 5, Mobs.Kind.FROST_ARCHER, Mobs.Kind.FROST_SPIDER),
            new SpawnZones.Zone("f1b_ruins", SNOW_RUINS, 20, 3, 4, 5, Mobs.Kind.FROST_ZOMBIE, Mobs.Kind.FROST_ARCHER),
            new SpawnZones.Zone("f1b_crypt", CRYPT, 9, 3, 5, 5, 0.35f, Mobs.Kind.FROST_ZOMBIE, Mobs.Kind.FROST_ARCHER));

    private FloorNorth(ServerLevel l) { super(l, 9191L); }

    public static boolean inside(int x, int z) {
        double ex = 3 + 2.5 * Math.sin(z * 0.12 + 3) + 1.5 * Math.sin(z * 0.27);
        double ez = 3 + 2.5 * Math.sin(x * 0.15 + 1) + 1.5 * Math.cos(x * 0.33);
        double ax = X0 + ex, bx = X1 - ex, az = Z0 + ez, bz = Z1;
        if (x < ax || x > bx || z < az || z > bz) return false;
        double rc = 16;
        double cx = Math.min(Math.max(x, ax + rc), bx - rc), cz = Math.min(Math.max(z, az + rc), bz - rc);
        return dist(x, z, cx, cz) <= rc;
    }

    private static double edgeDist(int x, int z) { return Math.min(Math.min(x - X0, X1 - x), Math.min(z - Z0, Z1 - z + 6)); }

    private static boolean isPath(int x, int z, int pad) {
        return Math.abs(x - 3) <= 2 + pad && z > CRYPT.getZ() + CRYPT_R;
    }

    private boolean flat(int x, int z) {
        return dist(x, z, OUTPOST) < 10 || dist(x, z, LAKE) < 21 || dist(x, z, CRYPT) < CRYPT_R + 3 || isPath(x, z, 1);
    }

    private int height(int x, int z) {
        if (flat(x, z)) return Y - 1;
        double h = 1.8 * Math.sin(x * 0.08 + 1) * Math.cos(z * 0.06) + 1.2 * Math.sin((x + z) * 0.05) + 0.7 * Math.cos(x * 0.23 - z * 0.19);
        if (dist(x, z, SPIRES) < 28) h += 1.2;
        return Y - 1 + (int) Math.round(h);
    }

    public static void build(ServerLevel level, boolean clear) {
        FloorNorth b = new FloorNorth(level);
        STASHES.clear(); CRYPT_STASHES.clear();
        if (clear) b.clearBox(X0 - 4, X1 + 4, 70, 130, Z0 - 4, Z1);
        b.terrain();
        b.path();
        b.outpost();
        b.lake();
        b.spires();
        b.snowRuins();
        b.pines();
        b.crypt();
        b.decor();
    }

    private void terrain() {
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            if (!inside(x, z)) continue;
            int h = height(x, z);
            int hash = Math.floorMod(x * 734287 + z * 912931, 20);
            Block top = hash < 11 ? Blocks.SNOW_BLOCK : hash < 14 ? Blocks.PACKED_ICE : hash < 17 ? Blocks.CALCITE : hash < 19 ? Blocks.STONE : Blocks.DIORITE;
            set(x, h, z, top);
            for (int y = h - 1; y >= Y - 4; y--) set(x, y, z, y > h - 2 ? Blocks.SNOW_BLOCK : Blocks.STONE);
            int depth = (int) Math.min(14, edgeDist(x, z) * 0.33) + Math.floorMod(x * 13 + z * 29, 3);
            for (int y = Y - 5; y >= Y - 5 - depth && y > 72; y--)
                set(x, y, z, pick(Blocks.STONE, Blocks.DIORITE, Blocks.CALCITE, Blocks.PACKED_ICE, Blocks.ANDESITE));
            if (Y - 6 - depth > 72 && Math.floorMod(x * 7 + z * 11, 9) == 0) {   // сосульки снизу
                int hh = 1 + Math.floorMod(x + z, 4);
                for (int k = 1; k <= hh; k++) set(x, Y - 5 - depth - k, z, Blocks.PACKED_ICE);
            }
        }
    }

    private void path() {
        for (int z = Z1; z > CRYPT.getZ() + CRYPT_R; z--)
            for (int x = 1; x <= 5; x++) set(x, Y - 1, z, (x + z) % 5 == 0 ? Blocks.PACKED_ICE : x == 1 || x == 5 ? Blocks.POLISHED_DIORITE : Blocks.GRAVEL);
        for (int z = OUTPOST.getZ() - 10; z > CRYPT.getZ() + CRYPT_R + 2; z -= 12)
            for (int x : new int[]{0, 6}) {
                set(x, Y, z, Blocks.SPRUCE_FENCE);
                set(x, Y + 1, z, Blocks.SPRUCE_FENCE);
                set(x, Y + 2, z, Blocks.LANTERN);
            }
    }

    private void outpost() {
        int cx = OUTPOST.getX(), cz = OUTPOST.getZ();
        for (int x = cx - 8; x <= cx + 8; x++) for (int z = cz - 8; z <= cz + 8; z++) {
            if (dist(x, z, OUTPOST) > 8.5) continue;
            set(x, Y - 1, z, dist(x, z, OUTPOST) < 2.5 ? Blocks.CHISELED_STONE_BRICKS : (x + z) % 2 == 0 ? Blocks.STONE_BRICKS : Blocks.POLISHED_DIORITE);
            for (int y = Y; y <= Y + 4; y++) set(x, y, z, Blocks.AIR);
        }
        set(cx, Y, cz, Blocks.CAMPFIRE);
        // палисад с воротами на юг (к трассе) и на север
        for (int a = 0; a < 360; a += 6) {
            int x = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * 8.5), z = cz + (int) Math.round(Math.sin(Math.toRadians(a)) * 8.5);
            if (Math.abs(x - 3) <= 3) continue;
            for (int y = Y; y <= Y + 2; y++) set(x, y, z, Blocks.STRIPPED_SPRUCE_LOG);
            set(x, Y + 3, z, Blocks.SNOW);
        }
        set(cx - 4, Y, cz - 2, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        set(cx + 4, Y, cz - 2, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        set(cx - 6, Y, cz + 3, Blocks.STONE_BRICKS);
        set(cx - 6, Y + 1, cz + 3, Blocks.STONE_BRICKS);
        sign(new BlockPos(cx - 5, Y + 1, cz + 3), Direction.EAST, DyeColor.LIGHT_BLUE, "ЭТАЖ 1 • тир 1+", "Инеевый Кряж", "ур. мобов 3–5", "Броня! Лодка т.2");
        set(cx + 6, Y, cz + 3, Blocks.STONE_BRICKS);
        set(cx + 6, Y + 1, cz + 3, Blocks.STONE_BRICKS);
        sign(new BlockPos(cx + 5, Y + 1, cz + 3), Direction.WEST, DyeColor.WHITE, "← Озеро, кладбище", "Шпили, руины →", "↑ Ледяной Склеп", "(PvP, элита)");
        stash(cx + 3, Y, cz - 5, STASHES);
    }

    private void lake() {
        int cx = LAKE.getX(), cz = LAKE.getZ();
        for (int x = cx - 22; x <= cx + 22; x++) for (int z = cz - 22; z <= cz + 22; z++) {
            double d = dist(x, z, LAKE) + 2 * Math.sin(Math.atan2(z - cz, x - cx) * 5);
            if (d > 20 || !inside(x, z)) continue;
            set(x, Y - 1, z, d < 16 ? (Math.floorMod(x * 3 + z * 5, 11) == 0 ? Blocks.BLUE_ICE : Blocks.ICE) : Blocks.PACKED_ICE);
            for (int y = Y; y <= Y + 2; y++) set(x, y, z, Blocks.AIR);
            if (d < 15 && Math.floorMod(x * 17 + z * 23, 97) == 0) { set(x, Y - 2, z, Blocks.SEA_LANTERN); }
        }
        // вмёрзшие кости и лодка-призрак в центре
        set(cx + 3, Y, cz - 2, Blocks.BONE_BLOCK);
        set(cx - 4, Y, cz + 5, Blocks.SKELETON_SKULL);
        stash(cx, Y, cz, STASHES);
    }

    private void spires() {
        for (int i = 0; i < 26; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * 25;
            int x = SPIRES.getX() + (int) (Math.cos(a) * r), z = SPIRES.getZ() + (int) (Math.sin(a) * r);
            if (!inside(x, z)) continue;
            int t = top(x, z, Y - 6, Y + 6);
            if (t == Integer.MIN_VALUE) continue;
            int h = 6 + rnd.nextInt(12);
            int lx = rnd.nextInt(3) - 1, lz = rnd.nextInt(3) - 1;   // наклон кристалла
            for (int y = 1; y <= h; y++) {
                int ox = x + lx * y / 5, oz = z + lz * y / 5;
                int w = y < h / 3 ? 1 : 0;
                for (int dx = -w; dx <= w; dx++) for (int dz = -w; dz <= w; dz++) if (Math.abs(dx) + Math.abs(dz) <= w)
                    set(ox + dx, t + y, oz + dz, y > h - 3 ? Blocks.BLUE_ICE : rnd.nextFloat() < 0.15f ? Blocks.SEA_LANTERN : Blocks.PACKED_ICE);
            }
        }
        int t = top(SPIRES.getX(), SPIRES.getZ(), Y - 6, Y + 8);
        if (t != Integer.MIN_VALUE) stash(SPIRES.getX(), t + 1, SPIRES.getZ() + 2, STASHES);
    }

    private void snowRuins() {
        Block[] walls = {Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.POLISHED_DIORITE, Blocks.PACKED_ICE, Blocks.SNOW_BLOCK};
        int[][] houses = {{0, 0}, {-12, -8}, {12, -6}, {-8, 12}, {10, 12}};
        for (int[] h : houses) {
            int x = SNOW_RUINS.getX() + h[0], z = SNOW_RUINS.getZ() + h[1];
            if (!inside(x, z)) continue;
            ruin(x, Y - 1, z, 4, Blocks.SPRUCE_PLANKS, Blocks.SNOW_BLOCK, walls, Blocks.LANTERN, STASHES, rnd.nextFloat() < 0.6f);
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++)
                if (rnd.nextFloat() < 0.35f && air(x + dx, Y, z + dz)) set(x + dx, Y, z + dz, Blocks.SNOW);
        }
    }

    private void pines() {
        for (int i = 0; i < 55; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * 26;
            int x = PINES.getX() + (int) (Math.cos(a) * r), z = PINES.getZ() + (int) (Math.sin(a) * r);
            if (!inside(x, z) || isPath(x, z, 2)) continue;
            int t = top(x, z, Y - 6, Y + 6);
            if (t == Integer.MIN_VALUE) continue;
            int h = 5 + rnd.nextInt(5);
            for (int k = 1; k <= h; k++) set(x, t + k, z, Blocks.SPRUCE_LOG);
            // голые заснеженные ветки ярусами
            for (int k = 3; k < h; k += 2) for (Direction d : Direction.Plane.HORIZONTAL) if (rnd.nextFloat() < 0.6f) {
                set(x + d.getStepX(), t + k, z + d.getStepZ(), Blocks.SPRUCE_FENCE);
                set(x + d.getStepX(), t + k + 1, z + d.getStepZ(), Blocks.SNOW);
            }
            set(x, t + h + 1, z, Blocks.SNOW);
            if (rnd.nextFloat() < 0.25f) { // надгробие
                int gx = x + 2, gz = z;
                int gt = top(gx, gz, Y - 6, Y + 6);
                if (gt != Integer.MIN_VALUE && air(gx, gt + 1, gz)) { set(gx, gt + 1, gz, Blocks.COBBLESTONE_WALL); set(gx, gt + 2, gz, Blocks.SNOW); }
            }
        }
        int t = top(PINES.getX(), PINES.getZ(), Y - 6, Y + 6);
        if (t != Integer.MIN_VALUE) stash(PINES.getX(), t + 1, PINES.getZ(), STASHES);
    }

    /** Ледяной Склеп: круглый храм изо льда и камня, внутри два тайника склепа. PvP-зона. */
    private void crypt() {
        int cx = CRYPT.getX(), cz = CRYPT.getZ(), R = CRYPT_R;
        for (int x = -R - 1; x <= R + 1; x++) for (int z = -R - 1; z <= R + 1; z++) {
            double r = Math.sqrt(x * x + z * z);
            if (r > R + 0.5) continue;
            set(cx + x, Y - 1, cz + z, r < 3 ? Blocks.CHISELED_STONE_BRICKS : r > R - 1.5 ? Blocks.STONE_BRICKS : (x + z) % 2 == 0 ? Blocks.POLISHED_DIORITE : Blocks.PACKED_ICE);
            for (int y = Y; y <= Y + 8; y++) set(cx + x, y, cz + z, Blocks.AIR);
            if (r > R - 0.8) {
                boolean gate = z > 0 && Math.abs(x) <= 2;
                if (!gate) for (int y = Y; y <= Y + 5; y++) set(cx + x, y, cz + z, y == Y + 5 ? Blocks.CHISELED_STONE_BRICKS : (y + x) % 3 == 0 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE);
            }
        }
        for (int a = 0; a < 360; a += 45) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 7), z = (int) Math.round(Math.sin(Math.toRadians(a)) * 7);
            for (int y = Y; y <= Y + 4; y++) set(cx + x, y, cz + z, Blocks.QUARTZ_PILLAR);
            set(cx + x, Y + 5, cz + z, Blocks.SOUL_LANTERN);
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) set(cx + x, Y, cz + z, Blocks.BLUE_ICE);
        set(cx, Y + 1, cz, Blocks.SOUL_CAMPFIRE);
        stash(cx - 4, Y, cz - 8, CRYPT_STASHES);
        stash(cx + 4, Y, cz - 8, CRYPT_STASHES);
        set(cx - 4, Y, cz + R + 2, Blocks.STONE_BRICKS);
        set(cx - 4, Y + 1, cz + R + 2, Blocks.STONE_BRICKS);
        sign(new BlockPos(cx - 4, Y + 1, cz + R + 3), Direction.SOUTH, DyeColor.LIGHT_BLUE, "ЛЕДЯНОЙ СКЛЕП", "Элитная стража", "Тайники склепа", "! PvP-зона !");
    }

    private void decor() {
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            if (!inside(x, z) || flat(x, z) || dist(x, z, SNOW_RUINS) < 18) continue;
            int t = top(x, z, Y - 6, Y + 6);
            if (t == Integer.MIN_VALUE || !air(x, t + 1, z)) continue;
            float f = rnd.nextFloat();
            if (f < 0.10f) set(x, t + 1, z, Blocks.SNOW);
            else if (f < 0.105f) set(x, t + 1, z, Blocks.DEAD_BUSH);
            else if (f < 0.109f) { set(x, t + 1, z, Blocks.PACKED_ICE); set(x, t + 2, z, Blocks.PACKED_ICE); }
            else if (f < 0.111f) set(x, t + 1, z, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rnd.nextInt(16)));
        }
        int[][] lone = {{-65, -410}, {70, -410}, {-20, -520}, {30, -510}, {-62, -590}, {72, -590}};
        Block[] walls = {Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.PACKED_ICE};
        for (int[] r : lone) if (inside(r[0], r[1])) ruin(r[0], Y - 1, r[1], 3, Blocks.SNOW_BLOCK, Blocks.STONE_BRICKS, walls, Blocks.LANTERN, STASHES, true);
    }
}
