package com.pvpciv.floor;

import com.pvpciv.combat.Mobs;
import com.pvpciv.registry.ModRegistry;
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
 * Этаж 1, локация 1 «Пепельная пустошь» — к югу от общежития. Прямоугольник ~200×330 с рваными краями:
 * Пепельные дюны, Руины Старого Квартала, Разлом с мостами, Мёртвая роща, Базальтовые столпы, данж-арена и логово босса.
 */
public final class Floor1 extends Builder {
    public static final int Y = DormBuilder.Y0;
    public static final int ROAD_END = DormBuilder.ROAD_Z1 + 1;
    public static final int X0 = -97, X1 = 103, Z0 = ROAD_END, Z1 = ROAD_END + 330;
    public static final BlockPos CENTER = new BlockPos(3, Y, (Z0 + Z1) / 2);
    public static final BlockPos CAMP = new BlockPos(3, Y, Z0 + 13);
    public static final BlockPos ARENA = new BlockPos(64, Y, 470);
    public static final BlockPos LAIR = new BlockPos(3, Y, Z1 - 22);
    public static final int LAIR_R = 11;
    public static final int CHASM_Z = 560;
    public static final BlockPos RUINS = new BlockPos(-55, Y, 470), GROVE = new BlockPos(58, Y, 612), PILLARS = new BlockPos(-55, Y, 618);
    public static final int[] BRIDGES_X = {-58, 3, 62};
    public static final List<BlockPos> STASHES = new ArrayList<>();

    /** Подзоны для баннера HUD. */
    public record Area(String name, BlockPos c, int r) {}
    public static final List<Area> AREAS = List.of(
            new Area("Лагерь", CAMP, 9),
            new Area("Руины Старого Квартала", RUINS, 34),
            new Area("Мёртвая роща", GROVE, 36),
            new Area("Базальтовые столпы", PILLARS, 36));

    public static final List<SpawnZones.Zone> ZONES = List.of(
            new SpawnZones.Zone("f1_dunes_w", new BlockPos(-42, Y, 405), 16, 3, 1, 1, Mobs.Kind.ASH_ZOMBIE, Mobs.Kind.ASH_SPIDER),
            new SpawnZones.Zone("f1_dunes_e", new BlockPos(46, Y, 400), 16, 3, 1, 2, Mobs.Kind.ASH_ZOMBIE, Mobs.Kind.ASH_SPIDER),
            new SpawnZones.Zone("f1_ruins", RUINS, 22, 4, 1, 2, Mobs.Kind.ASH_ZOMBIE, Mobs.Kind.WITHERED),
            new SpawnZones.Zone("f1_mid", new BlockPos(24, Y, 515), 16, 3, 2, 3, Mobs.Kind.WITHERED, Mobs.Kind.ASH_ARCHER, Mobs.Kind.ASH_ZOMBIE),
            new SpawnZones.Zone("f1_ruins_s", new BlockPos(-40, Y, 522), 15, 3, 2, 3, Mobs.Kind.ASH_ARCHER, Mobs.Kind.WITHERED, Mobs.Kind.ASH_ZOMBIE),
            new SpawnZones.Zone("f1_east", new BlockPos(80, Y, 528), 13, 3, 2, 3, Mobs.Kind.ASH_SPIDER, Mobs.Kind.ASH_ZOMBIE),
            new SpawnZones.Zone("f1_grove", GROVE, 22, 4, 3, 4, Mobs.Kind.ASH_SPIDER, Mobs.Kind.WITHERED, Mobs.Kind.ASH_ARCHER, Mobs.Kind.ASH_ZOMBIE),
            new SpawnZones.Zone("f1_pillars", PILLARS, 22, 4, 3, 4, Mobs.Kind.ASH_ARCHER, Mobs.Kind.WITHERED, Mobs.Kind.ASH_ZOMBIE),
            new SpawnZones.Zone("f1_south", new BlockPos(3, Y, 640), 14, 3, 4, 4, Mobs.Kind.ASH_ZOMBIE, Mobs.Kind.WITHERED, Mobs.Kind.ASH_ARCHER));

    private final List<BlockPos> lights = new ArrayList<>();

    private Floor1(ServerLevel l) { super(l, 4242L); }

    public static boolean isArenaGate(int x, int z) {
        return z >= ARENA.getZ() - 3 && z <= ARENA.getZ() + 3 && x < ARENA.getX();
    }

    public static List<BlockPos> arenaGateCells() {
        List<BlockPos> out = new ArrayList<>();
        for (int z = ARENA.getZ() - 3; z <= ARENA.getZ() + 3; z++)
            for (int x = ARENA.getX() - 21; x < ARENA.getX(); x++) {
                double r = Math.sqrt(Math.pow(x - ARENA.getX(), 2) + Math.pow(z - ARENA.getZ(), 2));
                if (r > 18.5 && r <= 20.6) for (int y = Y; y <= Y + 6; y++) out.add(new BlockPos(x, y, z));
            }
        return out;
    }

    /** Внутри ли острова (рваный прямоугольник со скруглёнными углами). */
    public static boolean inside(int x, int z) {
        double ex = 3 + 2.5 * Math.sin(z * 0.11) + 1.5 * Math.sin(z * 0.31 + 1);
        double ez = 3 + 2.5 * Math.sin(x * 0.13 + 2) + 1.5 * Math.cos(x * 0.29);
        double ax = X0 + ex, bx = X1 - ex, az = Z0, bz = Z1 - ez;
        if (x < ax || x > bx || z < az || z > bz) return z >= Z0 && z < Z0 + 4 && x >= DormBuilder.ROAD_X0 - 1 && x <= DormBuilder.ROAD_X1 + 1;
        double rc = 18;   // скругление углов
        double cx = Math.min(Math.max(x, ax + rc), bx - rc), cz = Math.min(Math.max(z, az + rc), bz - rc);
        return dist(x, z, cx, cz) <= rc;
    }

    private static double edgeDist(int x, int z) {
        return Math.min(Math.min(x - X0, X1 - x), Math.min(z - Z0 + 8, Z1 - z));
    }

    /** Разлом: волнистая трещина поперёк острова. */
    public static boolean chasm(int x, int z) {
        double cz = CHASM_Z + 6 * Math.sin(x * 0.07) + 3 * Math.sin(x * 0.19 + 1);
        double w = 2.2 + 1.2 * Math.sin(x * 0.05 + 2);
        return Math.abs(z - cz) <= w;
    }

    private boolean flatZone(int x, int z) {
        return dist(x, z, ARENA) < 24 || dist(x, z, LAIR) < LAIR_R + 4 || dist(x, z, CAMP) < 9 || isPath(x, z, 1);
    }

    /** Главная дорога (север-юг) и ответвление к арене. */
    public static boolean isPath(int x, int z, int pad) {
        if (Math.abs(x - 3) <= 2 + pad && z < LAIR.getZ() - LAIR_R) return true;
        if (Math.abs(z - ARENA.getZ()) <= 2 + pad && x > 3 && x < ARENA.getX() - 18) return true;
        return Math.abs(z - (CHASM_Z - 14)) <= 1 + pad && x >= BRIDGES_X[0] && x <= BRIDGES_X[2];   // тропа вдоль разлома к мостам
    }

    private int height(int x, int z) {
        if (flatZone(x, z)) return Y - 1;
        double h = 1.6 * Math.sin(x * 0.09) * Math.cos(z * 0.07) + 1.1 * Math.sin((x - z) * 0.05) + 0.8 * Math.sin(x * 0.21 + z * 0.17);
        if (dist(x, z, PILLARS) < 38) h += 1.5;
        return Y - 1 + (int) Math.round(h);
    }

    public static void build(ServerLevel level, boolean clear) {
        Floor1 b = new Floor1(level);
        STASHES.clear();
        if (clear) b.clearBox(X0 - 4, X1 + 4, 82, 125, Z0, Z1 + 6);
        b.terrain();
        b.paths();
        b.camp();
        b.arena();
        b.lair();
        b.ruinsQuarter();
        b.grove();
        b.pillars();
        b.decor();
        b.bridges();
        for (BlockPos p : b.lights) level.setBlock(p, Blocks.SOUL_LANTERN.defaultBlockState(), Block.UPDATE_ALL);
        b.fixShapes();
    }

    private void terrain() {
        for (int x = X0; x <= X1; x++)
            for (int z = Z0; z <= Z1; z++) {
                if (!inside(x, z)) continue;
                if (chasm(x, z)) continue;
                int h = height(x, z);
                int hash = Math.floorMod(x * 734287 + z * 912931, 20);
                double n = Math.sin(x * 0.3 + z * 0.17) + Math.cos(z * 0.23 - x * 0.11);
                Block top = n > 1.3 ? Blocks.SOUL_SOIL : n < -1.3 ? Blocks.TUFF
                        : hash < 9 ? Blocks.COARSE_DIRT : hash < 13 ? Blocks.GRAVEL : hash < 16 ? Blocks.SMOOTH_BASALT : hash < 18 ? Blocks.ROOTED_DIRT : Blocks.PACKED_MUD;
                if (dist(x, z, PILLARS) < 36 && hash < 8) top = Blocks.BLACKSTONE;
                if (dist(x, z, GROVE) < 36 && hash < 10) top = Blocks.PODZOL;
                for (int y = Y - 4; y <= h - 1; y++) set(x, y, z, y > h - 3 ? Blocks.DIRT : Blocks.STONE);
                // низ острова: сталактиты к центру, тонко у краёв
                double ed = edgeDist(x, z);
                int depth = (int) Math.min(16, ed * 0.35) + Math.floorMod(x * 31 + z * 17, 3);
                for (int y = Y - 5; y >= Y - 5 - depth && y > 70; y--)
                    set(x, y, z, pick(Blocks.STONE, Blocks.TUFF, Blocks.DEEPSLATE, Blocks.BLACKSTONE, Blocks.ANDESITE));
                // стенки разлома — обнажённая порода с тлеющими прожилками
                if (chasm(x, z + 1) || chasm(x, z - 1))
                    for (int y = h - 1; y >= Y - 8; y--) set(x, y, z, rnd.nextFloat() < 0.12f ? Blocks.MAGMA_BLOCK : pick(Blocks.BLACKSTONE, Blocks.BASALT, Blocks.TUFF));
                set(x, h, z, top);   // верх — последним, чтобы под гравием уже была опора
            }
    }

    private void paths() {
        for (int z = Z0; z < LAIR.getZ() - LAIR_R; z++)
            for (int x = 1; x <= 5; x++) if (!chasm(x, z)) set(x, Y - 1, z, (x + z) % 4 == 0 ? Blocks.MOSSY_COBBLESTONE : x == 1 || x == 5 ? Blocks.COBBLESTONE : Blocks.GRAVEL);
        for (int x = 6; x < ARENA.getX() - 19; x++)
            for (int z = ARENA.getZ() - 2; z <= ARENA.getZ() + 2; z++) set(x, Y - 1, z, (x + z) % 4 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.GRAVEL);
        for (int x = BRIDGES_X[0]; x <= BRIDGES_X[2]; x++)
            for (int z = CHASM_Z - 15; z <= CHASM_Z - 13; z++) if (inside(x, z) && !chasm(x, z)) set(x, Y - 1, z, (x * 3 + z) % 5 == 0 ? Blocks.COBBLESTONE : Blocks.GRAVEL);
        for (int z = CAMP.getZ() + 9; z < LAIR.getZ() - LAIR_R; z += 11) {
            if (Math.abs(z - CHASM_Z) < 12) continue;
            for (int x : new int[]{0, 6}) {
                set(x, Y, z, Blocks.DARK_OAK_FENCE);
                set(x, Y + 1, z, Blocks.DARK_OAK_FENCE);
                set(x, Y + 2, z, Blocks.SOUL_LANTERN);
            }
        }
    }

    private void bridges() {
        for (int bx : BRIDGES_X) {
            int zc = CHASM_Z;
            for (int z = zc - 12; z <= zc + 12; z++) {
                boolean over = false;
                for (int x = bx - 3; x <= bx + 3; x++) if (chasmRaw(x, z)) over = true;
                if (!over) continue;
                for (int x = bx - 2; x <= bx + 2; x++) {
                    set(x, Y - 1, z, Math.abs(x - bx) == 2 ? Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z) : Blocks.SPRUCE_PLANKS.defaultBlockState());
                    for (int y = Y; y <= Y + 3; y++) set(x, y, z, Blocks.AIR);
                }
                set(bx - 3, Y, z, Blocks.DARK_OAK_FENCE);
                set(bx + 3, Y, z, Blocks.DARK_OAK_FENCE);
                if (z % 4 == 0) { set(bx - 3, Y + 1, z, Blocks.CHAIN); set(bx + 3, Y + 1, z, Blocks.CHAIN); }
            }
            for (int z = zc - 12; z <= zc + 12; z++) {
                boolean over = false;
                for (int x = bx - 3; x <= bx + 3; x++) if (chasmRaw(x, z)) over = true;
                if (over) continue;
                if (z < zc) { set(bx - 3, Y + 1, z + 1, Blocks.SOUL_LANTERN); z = zc; }
            }
        }
        sign(new BlockPos(6, Y + 1, CHASM_Z - 11), Direction.NORTH, DyeColor.RED, "РАЗЛОМ", "Внизу — Бездна.", "Держись мостов.", "−10% ✦ за падение");
        set(6, Y, CHASM_Z - 11, Blocks.STONE_BRICKS);
        set(6, Y + 1, CHASM_Z - 10, Blocks.STONE_BRICKS);
    }

    /** Разлом без учёта мостов — чтобы понять, где строить настил. */
    private static boolean chasmRaw(int x, int z) {
        double cz = CHASM_Z + 6 * Math.sin(x * 0.07) + 3 * Math.sin(x * 0.19 + 1);
        double w = 2.2 + 1.2 * Math.sin(x * 0.05 + 2);
        return Math.abs(z - cz) <= w + 1;
    }

    private boolean free(int x, int z) {
        return inside(x, z) && !chasmRaw(x, z) && dist(x, z, ARENA) > 23 && dist(x, z, LAIR) > LAIR_R + 3 && dist(x, z, CAMP) > 10 && !isPath(x, z, 2)
                && dist(x, z, RUINS) > 30;
    }

    private int topY(int x, int z) { return top(x, z, Y - 6, Y + 6); }

    private void decor() {
        for (int x = X0; x <= X1; x++)
            for (int z = Z0; z <= Z1; z++) {
                if (!free(x, z)) continue;
                int t = topY(x, z);
                if (t == Integer.MIN_VALUE || t < Y - 3) continue;
                if (!air(x, t + 1, z)) continue;
                float f = rnd.nextFloat();
                if (f < 0.04f) set(x, t + 1, z, Blocks.DEAD_BUSH);
                else if (f < 0.046f) {
                    int hh = 2 + rnd.nextInt(3);
                    for (int i = 1; i <= hh; i++) set(x, t + i, z, Blocks.BASALT);
                } else if (f < 0.051f) set(x, t + 1, z, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rnd.nextInt(16)));
                else if (f < 0.054f) set(x, t + 1, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                else if (f < 0.056f) set(x, t + 1, z, Blocks.BONE_BLOCK);
            }
        // одиночные мёртвые деревья по дюнам
        for (int i = 0; i < 40; i++) {
            int x = X0 + 10 + rnd.nextInt(X1 - X0 - 20), z = Z0 + 20 + rnd.nextInt(Z1 - Z0 - 40);
            if (!free(x, z)) continue;
            int t = topY(x, z);
            if (t != Integer.MIN_VALUE) deadTree(x, t, z, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_FENCE, 4, 2);
        }
        // одинокие руины с тайниками по всей пустоши
        int[][] lone = {{-80, 395}, {82, 430}, {-20, 440}, {30, 590}, {-25, 650}, {85, 660}, {-85, 560}, {40, 535}};
        Block[] walls = {Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.CRACKED_STONE_BRICKS, Blocks.STONE_BRICKS};
        for (int[] r : lone) if (inside(r[0], r[1]) && !chasmRaw(r[0], r[1])) ruin(r[0], Y - 1, r[1], 3, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, walls, Blocks.SOUL_LANTERN, STASHES, true);
    }

    /** Руины Старого Квартала: улочка из разрушенных домов, площадь с колодцем, ратуша. */
    private void ruinsQuarter() {
        int cx = RUINS.getX(), cz = RUINS.getZ();
        for (int x = cx - 30; x <= cx + 30; x++) for (int z = cz - 30; z <= cz + 30; z++) {
            if (!inside(x, z) || dist(x, z, RUINS) > 31) continue;
            boolean street = Math.abs(z - cz) <= 2 || Math.abs(x - cx) <= 2;
            set(x, Y - 1, z, street ? ((x + z) % 3 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS)
                    : rnd.nextFloat() < 0.6f ? Blocks.COARSE_DIRT : Blocks.GRAVEL);
            for (int y = Y; y <= Y + 2; y++) set(x, y, z, Blocks.AIR);
        }
        Block[] walls = {Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS};
        int[][] houses = {{-16, -12}, {-16, 12}, {14, -12}, {14, 12}, {-16, -24}, {14, 24}, {-26, 0}, {24, 0}};
        for (int[] h : houses) ruin(cx + h[0], Y - 1, cz + h[1], 4, Blocks.SPRUCE_PLANKS, Blocks.STONE_BRICKS, walls, Blocks.SOUL_LANTERN, STASHES, rnd.nextFloat() < 0.6f);
        // колодец на площади
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            boolean rim = Math.abs(x) == 2 || Math.abs(z) == 2;
            if (rim) set(cx + x, Y, cz + z, Blocks.COBBLESTONE_WALL);
            else for (int y = Y - 1; y >= Y - 4; y--) set(cx + x, y, cz + z, y == Y - 4 ? Blocks.SOUL_SOIL : Blocks.AIR);
        }
        set(cx, Y - 4, cz, Blocks.SOUL_FIRE);
        for (int y = Y - 3; y <= Y - 1; y++) setLit(cx - 1, y, cz - 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));   // бета: из колодца можно вылезти
        for (int[] o : new int[][]{{2, 2}, {-2, -2}}) for (int y = Y + 1; y <= Y + 3; y++) set(cx + o[0], y, cz + o[1], Blocks.DARK_OAK_FENCE);
        for (int x = -2; x <= 2; x++) set(cx + x, Y + 4, cz, Blocks.DARK_OAK_SLAB);
        set(cx, Y + 3, cz, Blocks.CHAIN);
        set(cx, Y + 2, cz, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // ратуша: высокий остов с колокольней
        int hx = cx, hz = cz - 22;
        for (int x = hx - 6; x <= hx + 6; x++) for (int z = hz - 5; z <= hz + 5; z++) {
            set(x, Y - 1, z, Blocks.POLISHED_ANDESITE);
            boolean wall = Math.abs(x - hx) == 6 || Math.abs(z - hz) == 5;
            if (!wall) continue;
            boolean door = z == hz + 5 && Math.abs(x - hx) <= 1;
            int hh = door ? 0 : 3 + rnd.nextInt(5);
            for (int y = 0; y < hh; y++) set(x, Y + y, z, rnd.nextFloat() < 0.2f ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
        }
        for (int y = 0; y < 12; y++) for (int[] o : new int[][]{{-6, -5}, {6, -5}}) set(hx + o[0], Y + y, hz + o[1], Blocks.CHISELED_STONE_BRICKS);
        set(hx, Y, hz - 3, Blocks.LECTERN);
        stash(hx - 3, Y, hz - 3, STASHES);
        stash(hx + 3, Y, hz - 3, STASHES);
        sign(new BlockPos(cx + 3, Y + 1, cz + 3), Direction.SOUTH, DyeColor.ORANGE, "СТАРЫЙ", "КВАРТАЛ", "Здесь жили", "до Пепла");
        set(cx + 3, Y, cz + 3, Blocks.STONE_BRICKS);
        set(cx + 3, Y + 1, cz + 2, Blocks.STONE_BRICKS);
    }

    /** Мёртвая роща: густой лес обугленных деревьев, паутина, тлеющие пни. */
    private void grove() {
        for (int i = 0; i < 70; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * 34;
            int x = GROVE.getX() + (int) (Math.cos(a) * r), z = GROVE.getZ() + (int) (Math.sin(a) * r);
            if (!inside(x, z) || chasmRaw(x, z) || isPath(x, z, 2) || dist(x, z, LAIR) < LAIR_R + 4) continue;
            int t = topY(x, z);
            if (t == Integer.MIN_VALUE) continue;
            deadTree(x, t, z, rnd.nextBoolean() ? Blocks.STRIPPED_DARK_OAK_LOG : Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_FENCE, 5, 4);
            if (rnd.nextFloat() < 0.3f) set(x + 1, t + 1, z, Blocks.COBWEB);
        }
        int t = topY(GROVE.getX(), GROVE.getZ());
        if (t != Integer.MIN_VALUE) {
            for (int y = 1; y <= 9; y++) for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) set(GROVE.getX() + dx, t + y, GROVE.getZ() + dz, Blocks.DARK_OAK_LOG);
            stash(GROVE.getX() + 2, t + 1, GROVE.getZ(), STASHES);
            lights.add(new BlockPos(GROVE.getX() - 1, t + 1, GROVE.getZ()));
        }
    }

    /** Базальтовые столпы: высокие колонны из базальта и чернокамня, между ними — гнёзда лучников. */
    private void pillars() {
        for (int i = 0; i < 34; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * 34;
            int x = PILLARS.getX() + (int) (Math.cos(a) * r), z = PILLARS.getZ() + (int) (Math.sin(a) * r);
            if (!inside(x, z) || chasmRaw(x, z) || isPath(x, z, 2)) continue;
            int t = topY(x, z);
            if (t == Integer.MIN_VALUE) continue;
            int h = 5 + rnd.nextInt(10);
            int w = rnd.nextFloat() < 0.3f ? 1 : 0;
            for (int y = 1; y <= h; y++) for (int dx = 0; dx <= w; dx++) for (int dz = 0; dz <= w; dz++)
                set(x + dx, t + y, z + dz, y > h - 2 && rnd.nextBoolean() ? Blocks.POLISHED_BASALT : pick(Blocks.BASALT, Blocks.BASALT, Blocks.BLACKSTONE));
            if (rnd.nextFloat() < 0.25f) set(x, t + h + 1, z, Blocks.SOUL_CAMPFIRE);
        }
        int[][] nests = {{-62, 600}, {-44, 632}};
        for (int[] n : nests) {
            int t = topY(n[0], n[1]);
            if (t == Integer.MIN_VALUE) continue;
            stash(n[0], t + 1, n[1], STASHES);
            set(n[0] + 1, t + 1, n[1], Blocks.BONE_BLOCK);
        }
    }

    private void camp() {
        int cx = CAMP.getX(), cz = CAMP.getZ();
        for (int x = cx - 7; x <= cx + 7; x++) for (int z = cz - 7; z <= cz + 7; z++) {
            if (dist(x, z, CAMP) > 7.5) continue;
            set(x, Y - 1, z, dist(x, z, CAMP) < 2.5 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS);
            for (int y = Y; y <= Y + 4; y++) set(x, y, z, Blocks.AIR);
        }
        set(cx, Y, cz, Blocks.CAMPFIRE);
        set(cx - 3, Y, cz, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        set(cx + 3, Y, cz, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        set(cx, Y, cz + 3, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
        set(cx - 6, Y, cz - 4, Blocks.STONE_BRICKS);
        set(cx - 6, Y + 1, cz - 4, Blocks.STONE_BRICKS);
        sign(new BlockPos(cx - 5, Y + 1, cz - 4), Direction.EAST, DyeColor.ORANGE, "ЭТАЖ 1", "Пепельная", "пустошь", "ур. мобов 1–4");
        set(cx + 6, Y, cz - 4, Blocks.STONE_BRICKS);
        set(cx + 6, Y + 1, cz - 4, Blocks.STONE_BRICKS);
        sign(new BlockPos(cx + 5, Y + 1, cz - 4), Direction.WEST, DyeColor.LIGHT_BLUE, "← Руины  Арена →", "↓ Разлом, роща,", "столпы, Логово", "Громилы (PvP)");
        for (int[] o : new int[][]{{-7, -7}, {7, -7}, {-7, 7}, {7, 7}}) {
            set(cx + o[0], Y, cz + o[1], Blocks.DARK_OAK_FENCE);
            set(cx + o[0], Y + 1, cz + o[1], Blocks.LANTERN);
        }
    }

    private void arena() {
        int cx = ARENA.getX(), cz = ARENA.getZ();
        for (int x = -21; x <= 21; x++) for (int z = -21; z <= 21; z++) {
            double r = Math.sqrt(x * x + z * z);
            int wx = cx + x, wz = cz + z;
            if (r <= 18.5) {
                Block floor = Blocks.SMOOTH_SANDSTONE;
                if (r > 11.5 && r <= 12.5) floor = Blocks.CUT_RED_SANDSTONE;
                if (r <= 2.5) floor = Blocks.CHISELED_SANDSTONE;
                if (r > 6.5 && r <= 7.2) floor = Blocks.ORANGE_TERRACOTTA;
                set(wx, Y - 1, wz, floor);
                for (int y = Y; y <= Y + 8; y++) set(wx, y, wz, Blocks.AIR);
            } else if (r <= 20.6) {
                set(wx, Y - 1, wz, Blocks.STONE_BRICKS);
                if (isArenaGate(wx, wz)) {
                    set(wx, Y - 1, wz, Blocks.SMOOTH_SANDSTONE);
                    for (int y = Y; y <= Y + 6; y++) set(wx, y, wz, Blocks.AIR);
                } else for (int y = Y; y <= Y + 6; y++)
                    set(wx, y, wz, pick(Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS));
                set(wx, Y + 7, wz, Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
        }
        for (int a = 0; a < 360; a += 30) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 19.5), z = (int) Math.round(Math.sin(Math.toRadians(a)) * 19.5);
            if (isArenaGate(cx + x, cz + z)) continue;
            set(cx + x, Y + 8, cz + z, Blocks.SOUL_CAMPFIRE);
        }
        for (int y = Y; y <= Y + 8; y++) { set(cx - 20, y, cz - 4, Blocks.CHISELED_STONE_BRICKS); set(cx - 20, y, cz + 4, Blocks.CHISELED_STONE_BRICKS); }
        set(cx, Y, cz, ModRegistry.ARENA_CORE.get());
        for (int[] o : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) set(cx + o[0], Y, cz + o[1], Blocks.SOUL_LANTERN);
        sign(new BlockPos(cx - 21, Y + 2, cz - 4), Direction.WEST, DyeColor.ORANGE, "ПЕПЕЛЬНАЯ", "АРЕНА • данж", "ПКМ по Ядру", "3 волны, ур. 2–5");
    }

    /** 0.7.1: обновить табличку у логова. */
    public static void lairSign(net.minecraft.server.level.ServerLevel level) {
        BlockPos p = new BlockPos(LAIR.getX() - 4, Y + 1, LAIR.getZ() - LAIR_R - 3);
        if (level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sb) {
            sb.setText(sb.getFrontText().setMessage(2, net.minecraft.network.chat.Component.literal("Раз в 20 минут")), true);
            sb.setChanged();
            level.sendBlockUpdated(p, sb.getBlockState(), sb.getBlockState(), 3);
        }
    }

    private void lair() {
        int cx = LAIR.getX(), cz = LAIR.getZ();
        for (int x = -LAIR_R - 1; x <= LAIR_R + 1; x++) for (int z = -LAIR_R - 1; z <= LAIR_R + 1; z++) {
            double r = Math.sqrt(x * x + z * z);
            if (r > LAIR_R + 0.5) continue;
            Block f = r < 3 ? Blocks.POLISHED_BLACKSTONE_BRICKS : rnd.nextFloat() < 0.25f ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : rnd.nextFloat() < 0.5f ? Blocks.BLACKSTONE : Blocks.POLISHED_BLACKSTONE;
            set(cx + x, Y - 1, cz + z, f);
            for (int y = Y; y <= Y + 6; y++) set(cx + x, y, cz + z, Blocks.AIR);
        }
        for (int a = 0; a < 360; a += 30) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * LAIR_R), z = (int) Math.round(Math.sin(Math.toRadians(a)) * LAIR_R);
            if (a == 270) continue; // вход с севера
            int h = 4 + rnd.nextInt(3);
            for (int y = 0; y < h; y++) set(cx + x, Y + y, cz + z, Blocks.BASALT);
            set(cx + x, Y + h, cz + z, Blocks.SOUL_CAMPFIRE);
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) set(cx + x, Y - 1, cz + z, Blocks.CRYING_OBSIDIAN);
        set(cx + 2, Y, cz + 2, Blocks.SKELETON_SKULL);
        set(cx - 2, Y, cz - 2, Blocks.WITHER_SKELETON_SKULL);
        set(cx - 4, Y, cz - LAIR_R - 2, Blocks.STONE_BRICKS);
        set(cx - 4, Y + 1, cz - LAIR_R - 2, Blocks.STONE_BRICKS);
        sign(new BlockPos(cx - 4, Y + 1, cz - LAIR_R - 3), Direction.NORTH, DyeColor.RED, "ЛОГОВО", "ГРОМИЛЫ", "Раз в 20 минут", "! PvP-зона !");
    }
}
