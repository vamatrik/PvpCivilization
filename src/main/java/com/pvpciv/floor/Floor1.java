package com.pvpciv.floor;

import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.DormBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.ArrayList;
import java.util.List;

/** Этаж 1 «Пепельная пустошь»: остров, лагерь, данж-арена, логово босса, тайники. */
public final class Floor1 {
    public static final int Y = DormBuilder.Y0;
    public static final BlockPos CENTER = new BlockPos(3, Y, 422);
    public static final int R = 62;
    public static final int ROAD_END = DormBuilder.ROAD_Z1 + 1;
    public static final BlockPos CAMP = new BlockPos(3, Y, 368);
    public static final BlockPos ARENA = new BlockPos(38, Y, 428);
    public static final BlockPos LAIR = new BlockPos(3, Y, 464);
    public static final int LAIR_R = 11;
    public static final List<BlockPos> STASHES = new ArrayList<>();

    private final ServerLevel level;
    private final RandomSource rnd = RandomSource.create(4242L);
    private final List<BlockPos> panes = new ArrayList<>();

    private Floor1(ServerLevel l) { level = l; }

    private void set(int x, int y, int z, BlockState s) { level.setBlock(new BlockPos(x, y, z), s, Block.UPDATE_CLIENTS); }
    private void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }
    private Block pick(Block... b) { return b[rnd.nextInt(b.length)]; }

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

    private static double dist(int x, int z, BlockPos c) { return Math.sqrt(Math.pow(x - c.getX(), 2) + Math.pow(z - c.getZ(), 2)); }

    private int height(int x, int z) {
        if (dist(x, z, ARENA) < 24 || dist(x, z, LAIR) < LAIR_R + 4 || dist(x, z, CAMP) < 8) return Y - 1;
        if (Math.abs(x - 3) <= 3 && z < LAIR.getZ()) return Y - 1;
        if (Math.abs(z - ARENA.getZ()) <= 3 && x > 3 && x < ARENA.getX()) return Y - 1;
        double h = 1.4 * Math.sin(x * 0.15) * Math.cos(z * 0.12) + 0.9 * Math.sin((x - z) * 0.08);
        return Y - 1 + (int) Math.round(h);
    }

    private double radiusAt(int x, int z) {
        double a = Math.atan2(z - CENTER.getZ(), x - CENTER.getX());
        return R - 3 + 2.5 * Math.sin(a * 5) + 1.5 * Math.cos(a * 9);
    }

    public static void build(ServerLevel level) {
        Floor1 b = new Floor1(level);
        b.clear();
        b.terrain();
        b.paths();
        b.decor();
        b.camp();
        b.arena();
        b.lair();
        b.stashes();
        for (BlockPos p : b.panes) level.setBlock(p, Blocks.SOUL_LANTERN.defaultBlockState(), Block.UPDATE_ALL);
    }

    private void clear() {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = CENTER.getX() - R - 6; x <= CENTER.getX() + R + 6; x++)
            for (int z = DormBuilder.ROAD_Z1 - 2; z <= CENTER.getZ() + R + 6; z++)
                for (int y = 82; y <= 125; y++) {
                    if (z <= DormBuilder.ROAD_Z1 && x >= DormBuilder.ROAD_X0 - 1 && x <= DormBuilder.ROAD_X1 + 1 && y >= Y - 1 && y <= Y) continue;
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, air, Block.UPDATE_CLIENTS);
                }
    }

    private void terrain() {
        for (int x = CENTER.getX() - R - 4; x <= CENTER.getX() + R + 4; x++)
            for (int z = CENTER.getZ() - R - 4; z <= CENTER.getZ() + R + 4; z++) {
                double d = dist(x, z, CENTER), rr = radiusAt(x, z);
                if (d > rr) continue;
                if (z <= DormBuilder.ROAD_Z1) continue;
                int h = height(x, z);
                int hash = Math.floorMod(x * 734287 + z * 912931, 20);
                double n = Math.sin(x * 0.3 + z * 0.17) + Math.cos(z * 0.23 - x * 0.11);
                Block top = n > 1.3 ? Blocks.SOUL_SOIL : n < -1.3 ? Blocks.TUFF
                        : hash < 9 ? Blocks.COARSE_DIRT : hash < 13 ? Blocks.GRAVEL : hash < 16 ? Blocks.SMOOTH_BASALT : hash < 18 ? Blocks.ROOTED_DIRT : Blocks.PACKED_MUD;
                set(x, h, z, top);
                for (int y = h - 1; y >= Y - 4; y--) set(x, y, z, y > h - 3 ? Blocks.DIRT : Blocks.STONE);
                int depth = (int) ((rr - d) * 0.45) + rnd.nextInt(2);
                for (int y = Y - 5; y >= Y - 5 - depth && y > 82; y--)
                    set(x, y, z, pick(Blocks.STONE, Blocks.TUFF, Blocks.DEEPSLATE, Blocks.BLACKSTONE, Blocks.ANDESITE));
            }
    }

    private void paths() {
        for (int z = DormBuilder.ROAD_Z1 + 1; z < LAIR.getZ() - LAIR_R; z++)
            for (int x = 1; x <= 5; x++) set(x, Y - 1, z, (x + z) % 4 == 0 ? Blocks.MOSSY_COBBLESTONE : x == 1 || x == 5 ? Blocks.COBBLESTONE : Blocks.GRAVEL);
        for (int x = 6; x < ARENA.getX() - 19; x++)
            for (int z = ARENA.getZ() - 2; z <= ARENA.getZ() + 2; z++) set(x, Y - 1, z, (x + z) % 4 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.GRAVEL);
        for (int z = CAMP.getZ() + 8; z < LAIR.getZ() - LAIR_R; z += 9) {
            for (int x : new int[]{0, 6}) {
                set(x, Y, z, Blocks.DARK_OAK_FENCE);
                set(x, Y + 1, z, Blocks.DARK_OAK_FENCE);
                set(x, Y + 2, z, Blocks.SOUL_LANTERN);
            }
        }
    }

    private boolean free(int x, int z) {
        return dist(x, z, ARENA) > 23 && dist(x, z, LAIR) > LAIR_R + 3 && dist(x, z, CAMP) > 9 && Math.abs(x - 3) > 4
                && !(Math.abs(z - ARENA.getZ()) <= 4 && x > 3 && x < ARENA.getX());
    }

    private int top(int x, int z) {
        for (int y = Y + 4; y > Y - 6; y--) if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) return y;
        return Integer.MIN_VALUE;
    }

    private void decor() {
        for (int x = CENTER.getX() - R; x <= CENTER.getX() + R; x++)
            for (int z = CENTER.getZ() - R; z <= CENTER.getZ() + R; z++) {
                if (!free(x, z)) continue;
                int t = top(x, z);
                if (t == Integer.MIN_VALUE || t < Y - 3) continue;
                float f = rnd.nextFloat();
                if (f < 0.05f) set(x, t + 1, z, Blocks.DEAD_BUSH);
                else if (f < 0.058f) {
                    int hh = 2 + rnd.nextInt(4);
                    for (int i = 1; i <= hh; i++) set(x, t + i, z, Blocks.BASALT);
                } else if (f < 0.066f) set(x, t + 1, z, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rnd.nextInt(16)));
                else if (f < 0.07f) set(x, t + 1, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            }
        // мёртвые деревья
        for (int i = 0; i < 18; i++) {
            int x = CENTER.getX() + rnd.nextInt(2 * R - 16) - R + 8, z = CENTER.getZ() + rnd.nextInt(2 * R - 16) - R + 8;
            if (!free(x, z) || dist(x, z, CENTER) > R - 8) continue;
            int t = top(x, z);
            if (t == Integer.MIN_VALUE) continue;
            int h = 4 + rnd.nextInt(3);
            for (int k = 1; k <= h; k++) set(x, t + k, z, Blocks.STRIPPED_DARK_OAK_LOG);
            for (Direction d : Direction.Plane.HORIZONTAL) if (rnd.nextBoolean()) {
                int bx = x + d.getStepX(), bz = z + d.getStepZ(), by = t + h - 1 - rnd.nextInt(2);
                set(bx, by, bz, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, d.getAxis()));
                set(bx + d.getStepX(), by + 1, bz + d.getStepZ(), Blocks.DARK_OAK_FENCE);
            }
        }
        // руины
        int[][] ruins = {{-32, 418}, {-24, 392}, {22, 398}, {-30, 450}, {28, 466}, {-12, 478}, {40, 392}};
        for (int[] r : ruins) ruin(r[0], r[1]);
    }

    private void ruin(int cx, int cz) {
        int t = top(cx, cz);
        if (t == Integer.MIN_VALUE) return;
        int base = Y - 1;
        for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) {
            set(x, base, z, rnd.nextFloat() < 0.7f ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE);
            for (int y = base + 1; y <= base + 5; y++) set(x, y, z, Blocks.AIR);
            boolean wall = Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3;
            if (wall) {
                int hh = rnd.nextInt(4);
                for (int y = 1; y <= hh; y++) set(x, base + y, z, pick(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.CRACKED_STONE_BRICKS, Blocks.STONE_BRICKS));
            }
        }
        set(cx, base + 1, cz, ModRegistry.STASH.get());
        STASHES.add(new BlockPos(cx, base + 1, cz));
        set(cx + 1, base + 1, cz + 1, Blocks.COBWEB);
        set(cx - 2, base + 1, cz - 2, Blocks.SOUL_LANTERN);
    }

    private void stashes() {
        int[][] extra = {{-45, 425}, {45, 452}, {-10, 440}, {14, 380}};
        for (int[] s : extra) {
            int t = top(s[0], s[1]);
            if (t == Integer.MIN_VALUE) continue;
            set(s[0], t + 1, s[1], ModRegistry.STASH.get());
            STASHES.add(new BlockPos(s[0], t + 1, s[1]));
            set(s[0] + 1, t + 1, s[1], Blocks.DEAD_BUSH);
        }
    }

    private void camp() {
        int cx = CAMP.getX(), cz = CAMP.getZ();
        for (int x = cx - 6; x <= cx + 6; x++) for (int z = cz - 6; z <= cz + 6; z++) {
            if (dist(x, z, CAMP) > 6.5) continue;
            set(x, Y - 1, z, dist(x, z, CAMP) < 2.5 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS);
            for (int y = Y; y <= Y + 4; y++) set(x, y, z, Blocks.AIR);
        }
        set(cx, Y, cz, Blocks.CAMPFIRE);
        set(cx - 3, Y, cz, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        set(cx + 3, Y, cz, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        set(cx, Y, cz + 3, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
        set(cx - 5, Y, cz - 4, Blocks.STONE_BRICKS);
        set(cx - 5, Y + 1, cz - 4, Blocks.STONE_BRICKS);
        sign(new BlockPos(cx - 4, Y + 1, cz - 4), Direction.EAST, DyeColor.ORANGE, "ЭТАЖ 1", "Пепельная", "пустошь", "ур. мобов 1–3");
        set(cx + 5, Y, cz - 4, Blocks.STONE_BRICKS);
        set(cx + 5, Y + 1, cz - 4, Blocks.STONE_BRICKS);
        sign(new BlockPos(cx + 4, Y + 1, cz - 4), Direction.WEST, DyeColor.LIGHT_BLUE, "→ Арена (данж)", "↓ Логово", "Громилы (PvP)", "Тайники в руинах");
        for (int[] o : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            set(cx + o[0], Y, cz + o[1], Blocks.DARK_OAK_FENCE);
            set(cx + o[0], Y + 1, cz + o[1], Blocks.LANTERN);
        }
    }

    private void sign(BlockPos pos, Direction facing, DyeColor c, String... lines) {
        level.setBlock(pos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), Block.UPDATE_ALL);
        DormBuilder.writeSign(level, pos, c, lines);
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
        for (int[] o : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) set(cx + o[0], Y, cz + o[1], Blocks.SOUL_LANTERN);
        sign(new BlockPos(cx - 21, Y + 2, cz - 4), Direction.WEST, DyeColor.ORANGE, "ПЕПЕЛЬНАЯ", "АРЕНА • данж", "ПКМ по Ядру", "3 волны, ур. 2–4");
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
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            set(cx + x, Y - 1, cz + z, Blocks.CRYING_OBSIDIAN);
        set(cx + 2, Y, cz + 2, Blocks.SKELETON_SKULL);
        set(cx - 2, Y, cz - 2, Blocks.WITHER_SKELETON_SKULL);
        set(cx - 4, Y, cz - LAIR_R - 2, Blocks.STONE_BRICKS);
        set(cx - 4, Y + 1, cz - LAIR_R - 2, Blocks.STONE_BRICKS);
        sign(new BlockPos(cx - 4, Y + 1, cz - LAIR_R - 3), Direction.NORTH, DyeColor.RED, "ЛОГОВО", "ГРОМИЛЫ", "Босс раз в час", "! PvP-зона !");
    }

    @SuppressWarnings("unused") private static final Half H = Half.TOP;
}
