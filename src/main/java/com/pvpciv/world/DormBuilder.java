package com.pvpciv.world;

import com.pvpciv.data.CivData;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

/**
 * Процедурно строит Эпизод 1: общежитие с атриумом, Ледяной Тракт над Бездной и Пепельную Арену.
 * Всё в координатах вокруг (0, 100, 0).
 */
public final class DormBuilder {
    public static final int Y0 = 100, FLOORS = 4, FH = 6;
    public static final int ATR = 21, GAL_IN = 22, GAL_OUT = 24, BAND_IN = 25, BAND_OUT = 31;
    public static final int SLOTS = 6, SLOT_W = 7, SLOT_START = -21;
    public static final int ROAD_X0 = 0, ROAD_X1 = 6, ROAD_CX = 3;
    public static final int DOCK_Z0 = 32, DOCK_Z1 = 38;
    public static final int ROAD_Z0 = 39, ROAD_LEN = 320, ROAD_Z1 = ROAD_Z0 + ROAD_LEN - 1;
    public static final int ARENA_Z = ROAD_Z1 + 20;
    public static final BlockPos ARENA_CENTER = new BlockPos(ROAD_CX, Y0, ARENA_Z);
    public static final BlockPos DOCK_CENTER = new BlockPos(ROAD_CX, Y0, 35);
    public static final BlockPos ARENA_DOCK = new BlockPos(ROAD_CX, Y0, ROAD_Z1 - 3);
    public static final BlockPos ATRIUM_CENTER = new BlockPos(0, Y0, 6);
    public static final BlockPos LUNA_SPOT = new BlockPos(-4, Y0, -23);

    private static final int N = 0, E = 1, S = 2, W = 3;
    private final ServerLevel level;
    private final RandomSource rnd = RandomSource.create(1337L);
    private final List<BlockPos> paneQueue = new ArrayList<>();
    private final List<BlockState> paneStates = new ArrayList<>();

    private DormBuilder(ServerLevel level) { this.level = level; }

    // ---------------------------------------------------------------- geometry helpers
    private static int wx(int side, int u, int d) {
        return switch (side) { case N, S -> u; case W -> -d; default -> d; };
    }
    private static int wz(int side, int u, int d) {
        return switch (side) { case N -> -d; case S -> d; default -> u; };
    }
    private static Direction inward(int side) {
        return switch (side) { case N -> Direction.SOUTH; case S -> Direction.NORTH; case W -> Direction.EAST; default -> Direction.WEST; };
    }
    private static float yawOf(Direction d) {
        return switch (d) { case SOUTH -> 0f; case WEST -> 90f; case NORTH -> 180f; default -> -90f; };
    }
    public static boolean isArenaGate(int x, int z) {
        return x >= ROAD_X0 && x <= ROAD_X1 && z < ARENA_Z;
    }
    public static List<BlockPos> arenaGateCells() {
        List<BlockPos> out = new ArrayList<>();
        for (int x = ROAD_X0; x <= ROAD_X1; x++)
            for (int z = ARENA_Z - 21; z < ARENA_Z; z++) {
                double r = Math.sqrt(Math.pow(x - ROAD_CX, 2) + Math.pow(z - ARENA_Z, 2));
                if (r > 18.5 && r <= 20.6)
                    for (int y = Y0; y <= Y0 + 6; y++) out.add(new BlockPos(x, y, z));
            }
        return out;
    }

    private void set(int x, int y, int z, BlockState s) { level.setBlock(new BlockPos(x, y, z), s, Block.UPDATE_CLIENTS); }
    private void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }
    private void pane(int x, int y, int z, Block b) { paneQueue.add(new BlockPos(x, y, z)); paneStates.add(b.defaultBlockState()); }

    private Block pick(Block... blocks) { return blocks[rnd.nextInt(blocks.length)]; }

    private void sign(BlockPos pos, Direction facing, DyeColor color, String... lines) {
        level.setBlock(pos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), Block.UPDATE_ALL);
        writeSign(level, pos, color, lines);
    }

    public static void writeSign(ServerLevel level, BlockPos pos, DyeColor color, String... lines) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SignBlockEntity sbe) {
            SignText t = new SignText().setColor(color).setHasGlowingText(true);
            for (int i = 0; i < Math.min(4, lines.length); i++) t = t.setMessage(i, Component.literal(lines[i]));
            sbe.setText(t, true);
            sbe.setText(t, false);
            sbe.setWaxed(true);
            sbe.setChanged();
            BlockState st = level.getBlockState(pos);
            level.sendBlockUpdated(pos, st, st, Block.UPDATE_ALL);
        }
    }

    // ---------------------------------------------------------------- entry
    public static void build(ServerLevel level, CivData data) {
        DormBuilder b = new DormBuilder(level);
        data.apartments.clear();
        b.clear();
        b.island();
        b.base();
        b.floorsAndWalls();
        b.apartments(data);
        b.ladders();
        b.atrium();
        b.roadAndDock();
        b.flushPanes();
        b.signs(data);
        data.built = true;
        data.version = 1;
        data.setDirty();
    }

    private void clear() {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -34; x <= 34; x++) for (int z = -34; z <= 40; z++) for (int y = 84; y <= 130; y++) {
            BlockPos p = new BlockPos(x, y, z);
            if (!level.getBlockState(p).isAir()) level.setBlock(p, air, Block.UPDATE_CLIENTS);
        }
    }

    /** Перевёрнутая гора под общежитием — летающий остров над Бездной. */
    private void island() {
        for (int k = 0; k < 15; k++) {
            int r = 31 - 2 * k - rnd.nextInt(2);
            if (r < 2) break;
            int y = Y0 - 2 - k;
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                boolean edge = Math.abs(x) == r || Math.abs(z) == r;
                if (edge && rnd.nextFloat() < 0.35f) continue;
                set(x, y, z, k == 0 ? Blocks.STONE_BRICKS : pick(Blocks.STONE, Blocks.ANDESITE, Blocks.TUFF, Blocks.DEEPSLATE, Blocks.COBBLESTONE, Blocks.STONE));
            }
        }
    }

    private void base() {
        for (int x = -BAND_OUT; x <= BAND_OUT; x++) for (int z = -BAND_OUT; z <= BAND_OUT; z++) {
            int m = Math.max(Math.abs(x), Math.abs(z));
            if (m <= ATR) { set(x, Y0 - 1, z, Blocks.GRASS_BLOCK); set(x, Y0 - 2, z, Blocks.DIRT); }
            else if (m <= GAL_OUT) set(x, Y0 - 1, z, Blocks.POLISHED_ANDESITE);
            else set(x, Y0 - 1, z, Blocks.SMOOTH_STONE);
        }
    }

    private void floorsAndWalls() {
        for (int f = 0; f < FLOORS; f++) {
            int y = Y0 + f * FH;
            for (int x = -BAND_OUT; x <= BAND_OUT; x++) for (int z = -BAND_OUT; z <= BAND_OUT; z++) {
                int m = Math.max(Math.abs(x), Math.abs(z));
                if (m < GAL_IN) continue;
                if (f > 0) {
                    if (m == BAND_OUT) set(x, y - 1, z, Blocks.CYAN_TERRACOTTA);
                    else set(x, y - 1, z, m <= GAL_OUT ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
                    if (m == GAL_IN) pane(x, y, z, Blocks.WHITE_STAINED_GLASS_PANE);
                }
                if (m >= BAND_IN) {
                    boolean corner = Math.abs(x) >= GAL_IN && Math.abs(z) >= GAL_IN;
                    for (int dy = 0; dy < FH - 1; dy++)
                        set(x, y + dy, z, corner ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.WHITE_CONCRETE);
                }
            }
            // светильники на галерее
            if (f > 0) for (int side = 0; side < 4; side++) for (int u = -18; u <= 18; u += 6)
                set(wx(side, u, GAL_IN + 1), y + 4, wz(side, u, GAL_IN + 1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        int roof = Y0 + FLOORS * FH - 1;
        for (int x = -BAND_OUT; x <= BAND_OUT; x++) for (int z = -BAND_OUT; z <= BAND_OUT; z++) {
            int m = Math.max(Math.abs(x), Math.abs(z));
            if (m < GAL_IN) continue;
            set(x, roof, z, Blocks.SMOOTH_STONE);
            if (m == BAND_OUT || m == GAL_IN) set(x, roof + 1, z, Blocks.STONE_BRICKS);
        }
        // большие светящиеся буквы-маяки на крыше по углам
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1})
            for (int dy = 1; dy <= 4; dy++) set(sx * 28, roof + dy, sz * 28, dy == 4 ? Blocks.SEA_LANTERN : Blocks.QUARTZ_PILLAR);
    }

    private void apartments(CivData data) {
        int id = 0;
        for (int f = 0; f < FLOORS; f++) {
            int y = Y0 + f * FH;
            int seq = 1;
            for (int side = 0; side < 4; side++) for (int slot = 0; slot < SLOTS; slot++) {
                int s = SLOT_START + slot * SLOT_W;
                if (f == 0 && side == S && slot == 3) { exitHall(y, s); continue; }
                Direction in = inward(side);
                Direction out = in.getOpposite();
                String npc = "";
                if (f == 0 && side == N && slot == 2) npc = "luna";
                if (f == 0 && side == S && slot == 1) npc = "agafya";
                if (f == 0 && side == W && slot == 0) npc = "zero";
                // интерьер
                for (int u = s + 1; u <= s + 5; u++) for (int d = BAND_IN + 1; d <= BAND_OUT - 1; d++) {
                    int x = wx(side, u, d), z = wz(side, u, d);
                    set(x, y - 1, z, npc.equals("luna") ? Blocks.CHERRY_PLANKS : Blocks.SPRUCE_PLANKS);
                    for (int dy = 0; dy < FH - 1; dy++) set(x, y + dy, z, Blocks.AIR);
                }
                // окно в Бездну
                for (int u = s + 2; u <= s + 4; u++) for (int dy = 1; dy <= 2; dy++)
                    set(wx(side, u, BAND_OUT), y + dy, wz(side, u, BAND_OUT), Blocks.LIGHT_BLUE_STAINED_GLASS);
                // кровать
                DyeColor bedColor = npc.equals("luna") ? DyeColor.PURPLE : DyeColor.LIGHT_BLUE;
                Block bed = npc.equals("luna") ? Blocks.PURPLE_BED : Blocks.LIGHT_BLUE_BED;
                set(wx(side, s + 1, 29), y, wz(side, s + 1, 29), bed.defaultBlockState().setValue(BedBlock.FACING, out).setValue(BedBlock.PART, BedPart.FOOT));
                set(wx(side, s + 1, 30), y, wz(side, s + 1, 30), bed.defaultBlockState().setValue(BedBlock.FACING, out).setValue(BedBlock.PART, BedPart.HEAD));
                // мебель
                set(wx(side, s + 5, 30), y, wz(side, s + 5, 30), Blocks.BARREL);
                set(wx(side, s + 5, 30), y + 1, wz(side, s + 5, 30), Blocks.POTTED_CHERRY_SAPLING);
                set(wx(side, s + 5, 26), y, wz(side, s + 5, 26), Blocks.BOOKSHELF);
                set(wx(side, s + 5, 27), y, wz(side, s + 5, 27), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, in));
                for (int u = s + 2; u <= s + 4; u++) for (int d = 27; d <= 29; d++)
                    set(wx(side, u, d), y, wz(side, u, d), npc.equals("luna") ? Blocks.PINK_CARPET : Blocks.LIGHT_GRAY_CARPET);
                set(wx(side, s + 3, 28), y + 4, wz(side, s + 3, 28), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
                // дверь
                int dx = wx(side, s + 3, BAND_IN), dz = wz(side, s + 3, BAND_IN);
                BlockState door = ModRegistry.APARTMENT_DOOR.get().defaultBlockState()
                        .setValue(DoorBlock.FACING, out).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.OPEN, false);
                set(dx, y, dz, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
                set(dx, y + 1, dz, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

                CivData.Apartment a = new CivData.Apartment();
                a.id = id++;
                a.floor = f;
                a.number = npc.equals("zero") ? 0 : (f + 1) * 100 + seq;
                seq++;
                a.door = new BlockPos(dx, y, dz);
                a.sign = new BlockPos(wx(side, s + 3, GAL_OUT), y + 2, wz(side, s + 3, GAL_OUT));
                a.spawn = new BlockPos(wx(side, s + 3, 28), y, wz(side, s + 3, 28));
                a.yaw = yawOf(in);
                a.npc = npc;
                data.apartments.add(a);
            }
        }
    }

    private void exitHall(int y, int s) {
        int side = S;
        for (int u = s + 1; u <= s + 5; u++) for (int d = BAND_IN; d <= BAND_OUT; d++) {
            int x = wx(side, u, d), z = wz(side, u, d);
            set(x, y - 1, z, Blocks.POLISHED_DEEPSLATE);
            for (int dy = 0; dy < 4; dy++) set(x, y + dy, z, Blocks.AIR);
        }
        for (int d = BAND_IN; d <= BAND_OUT; d += 3) {
            set(wx(side, s + 3, d), y + 3, wz(side, s + 3, d), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            set(wx(side, s + 3, d), y + 4, wz(side, s + 3, d), Blocks.WHITE_CONCRETE);
        }
    }

    private void ladders() {
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            int x = sx * GAL_OUT, z = sz * GAL_OUT;
            Direction facing = sx > 0 ? Direction.WEST : Direction.EAST;
            for (int y = Y0; y <= Y0 + (FLOORS - 1) * FH + 2; y++)
                set(x, y, z, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing));
        }
    }

    private void atrium() {
        // дорожки
        for (int x = -ATR; x <= ATR; x++) for (int z = -ATR; z <= ATR; z++) {
            boolean path = (Math.abs(x) <= 1) || (Math.abs(z) <= 1) || (x >= 2 && x <= 4 && z >= 15);
            if (path) set(x, Y0 - 1, z, (x + z) % 5 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
        }
        // фонтан
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            boolean rim = Math.abs(x) == 3 || Math.abs(z) == 3;
            if (rim) { set(x, Y0 - 1, z, Blocks.POLISHED_DIORITE); set(x, Y0, z, Blocks.QUARTZ_BRICKS); }
            else { set(x, Y0 - 2, z, Blocks.PRISMARINE); set(x, Y0 - 1, z, Blocks.SEA_LANTERN); }
        }
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) if (x != 0 || z != 0)
            level.setBlock(new BlockPos(x, Y0, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        for (int y = Y0; y <= Y0 + 3; y++) set(0, y, 0, Blocks.QUARTZ_PILLAR);
        set(0, Y0 + 4, 0, Blocks.SEA_LANTERN);
        // деревья
        int[][] cherries = {{-12, -12}, {12, -12}, {-12, 12}, {12, 12}, {-16, 6}, {16, -6}, {-7, -17}, {8, 17}};
        for (int[] c : cherries) tree(c[0], c[1], Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES);
        int[][] oaks = {{-17, -5}, {17, 7}, {6, -16}, {-8, 16}};
        for (int[] c : oaks) tree(c[0], c[1], Blocks.BIRCH_LOG, Blocks.AZALEA_LEAVES);
        // цветы
        Block[] flowers = {Blocks.PINK_TULIP, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS};
        for (int x = -ATR; x <= ATR; x++) for (int z = -ATR; z <= ATR; z++) {
            BlockPos p = new BlockPos(x, Y0, z);
            if (level.getBlockState(p.below()).is(Blocks.GRASS_BLOCK) && level.getBlockState(p).isAir() && rnd.nextFloat() < 0.18f)
                set(x, Y0, z, flowers[rnd.nextInt(flowers.length)]);
        }
        // фонари вдоль дорожек и скамейки
        for (int t = 6; t <= 20; t += 7) for (int sgn : new int[]{-1, 1}) {
            lamp(2, sgn * t); lamp(-2, sgn * t); lamp(sgn * t, 2); lamp(sgn * t, -2);
        }
        for (int sgn : new int[]{-1, 1}) {
            set(-2, Y0, sgn * 9, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            set(-2, Y0, sgn * 10, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            set(sgn * 9, Y0, -2, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            set(sgn * 10, Y0, -2, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        }
    }

    private void lamp(int x, int z) {
        if (!level.getBlockState(new BlockPos(x, Y0, z)).isAir() && !level.getBlockState(new BlockPos(x, Y0, z)).canBeReplaced()) return;
        set(x, Y0, z, Blocks.DARK_OAK_FENCE);
        set(x, Y0 + 1, z, Blocks.DARK_OAK_FENCE);
        set(x, Y0 + 2, z, Blocks.LANTERN);
    }

    private void tree(int x, int z, Block log, Block leaves) {
        int h = 4 + rnd.nextInt(2);
        BlockState lv = leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dy = 2; dy <= h + 2; dy++) {
            int r = dy >= h + 1 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.abs(dx) == r && Math.abs(dz) == r && rnd.nextFloat() < 0.6f) continue;
                BlockPos p = new BlockPos(x + dx, Y0 + dy, z + dz);
                if (level.getBlockState(p).isAir()) level.setBlock(p, lv, Block.UPDATE_CLIENTS);
            }
        }
        for (int dy = 0; dy < h; dy++) set(x, Y0 + dy, z, log);
    }

    private void roadAndDock() {
        // пристань у выхода
        for (int x = -3; x <= 9; x++) for (int z = DOCK_Z0; z <= DOCK_Z1; z++) {
            set(x, Y0 - 1, z, (x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES);
            set(x, Y0 - 2, z, Blocks.COBBLED_DEEPSLATE);
            boolean edge = x == -3 || x == 9 || (z == DOCK_Z1 && (x < ROAD_X0 || x > ROAD_X1));
            if (edge) pane(x, Y0, z, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
        }
        for (int x : new int[]{-3, 9}) { set(x, Y0, DOCK_Z0, Blocks.SEA_LANTERN); set(x, Y0, DOCK_Z1, Blocks.SEA_LANTERN); }
        // Ледяной Тракт
        for (int z = ROAD_Z0; z <= ROAD_Z1; z++) {
            for (int x = ROAD_X0; x <= ROAD_X1; x++) set(x, Y0 - 1, z, Blocks.PACKED_ICE);
            set(ROAD_X0 - 1, Y0 - 1, z, Blocks.BLUE_ICE);
            set(ROAD_X1 + 1, Y0 - 1, z, Blocks.BLUE_ICE);
            if (z % 12 == 0) {
                set(ROAD_X0 - 1, Y0, z, Blocks.SEA_LANTERN);
                set(ROAD_X1 + 1, Y0, z, Blocks.SEA_LANTERN);
                set(ROAD_CX, Y0 - 2, z, Blocks.PACKED_ICE);
                set(ROAD_CX, Y0 - 3, z, Blocks.PACKED_ICE);
                set(ROAD_CX, Y0 - 4, z, Blocks.ICE);
            } else {
                pane(ROAD_X0 - 1, Y0, z, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
                pane(ROAD_X1 + 1, Y0, z, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
            }
        }
    }

    private void arena() {
        int cx = ROAD_CX, cz = ARENA_Z;
        // подставка-остров
        for (int k = 0; k < 12; k++) {
            int r = 21 - 2 * k;
            if (r < 2) break;
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                if (x * x + z * z > r * r) continue;
                if (x * x + z * z > (r - 1) * (r - 1) && rnd.nextFloat() < 0.3f) continue;
                set(cx + x, Y0 - 2 - k, cz + z, pick(Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.TERRACOTTA, Blocks.STONE));
            }
        }
        for (int x = -21; x <= 21; x++) for (int z = -21; z <= 21; z++) {
            double r = Math.sqrt(x * x + z * z);
            int wxp = cx + x, wzp = cz + z;
            if (r <= 18.5) {
                Block floor = Blocks.SMOOTH_SANDSTONE;
                if (r > 11.5 && r <= 12.5) floor = Blocks.CUT_RED_SANDSTONE;
                if (r <= 2.5) floor = Blocks.CHISELED_SANDSTONE;
                if (r > 6.5 && r <= 7.2) floor = Blocks.ORANGE_TERRACOTTA;
                set(wxp, Y0 - 1, wzp, floor);
                for (int y = Y0; y <= Y0 + 8; y++) set(wxp, y, wzp, Blocks.AIR);
            } else if (r <= 20.6) {
                set(wxp, Y0 - 1, wzp, Blocks.STONE_BRICKS);
                if (isArenaGate(wxp, wzp)) {
                    set(wxp, Y0 - 1, wzp, Blocks.SMOOTH_SANDSTONE);
                    for (int y = Y0; y <= Y0 + 6; y++) set(wxp, y, wzp, Blocks.AIR);
                    set(wxp, Y0 + 7, wzp, Blocks.POLISHED_BLACKSTONE_BRICKS);
                } else {
                    for (int y = Y0; y <= Y0 + 6; y++)
                        set(wxp, y, wzp, pick(Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS));
                    set(wxp, Y0 + 7, wzp, Blocks.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }
        // голубые огни на стенах
        for (int a = 0; a < 360; a += 30) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 19.5), z = (int) Math.round(Math.sin(Math.toRadians(a)) * 19.5);
            if (isArenaGate(cx + x, cz + z)) continue;
            set(cx + x, Y0 + 8, cz + z, Blocks.SOUL_CAMPFIRE);
        }
        // ворота-арка
        for (int y = Y0; y <= Y0 + 8; y++) { set(ROAD_X0 - 1, y, cz - 20, Blocks.CHISELED_STONE_BRICKS); set(ROAD_X1 + 1, y, cz - 20, Blocks.CHISELED_STONE_BRICKS); }
        set(cx, Y0, cz, ModRegistry.ARENA_CORE.get());
        for (int[] o : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) set(cx + o[0], Y0, cz + o[1], Blocks.SOUL_LANTERN);
    }

    private void flushPanes() {
        for (int i = 0; i < paneQueue.size(); i++) level.setBlock(paneQueue.get(i), paneStates.get(i), Block.UPDATE_ALL);
        paneQueue.clear();
        paneStates.clear();
    }

    private void signs(CivData data) {
        for (CivData.Apartment a : data.apartments) {
            Direction facing = Direction.getNearest(a.sign.getX() - a.door.getX(), 0, a.sign.getZ() - a.door.getZ());
            String l2 = switch (a.npc) {
                case "luna" -> "Луна";
                case "agafya" -> "Комендантская";
                case "zero" -> "???";
                default -> "свободна";
            };
            DyeColor color = a.npc.equals("luna") ? DyeColor.PINK : a.npc.equals("zero") ? DyeColor.RED : DyeColor.LIGHT_BLUE;
            sign(a.sign, facing, color, "", "Кв. " + a.number, l2, "");
        }
        sign(new BlockPos(ROAD_CX, Y0 + 4, BAND_IN - 1), Direction.NORTH, DyeColor.YELLOW, "▼ ВЫХОД ▼", "Ледяной Тракт", "→ Этаж 1:", "Пепельная пустошь");
        set(-4, Y0 + 1, DOCK_Z0 + 2, Blocks.SEA_LANTERN);
        sign(new BlockPos(-3, Y0 + 1, DOCK_Z0 + 2), Direction.EAST, DyeColor.LIGHT_BLUE, "Пристань", "Ледоруба", "Лодка: ПКМ", "по льду");
    }

    /** Кузница в каждой квартире (на месте книжной полки). */
    public static void placeForges(ServerLevel level, CivData data) {
        for (CivData.Apartment a : data.apartments) {
            int ix = Integer.signum(a.sign.getX() - a.door.getX()), iz = Integer.signum(a.sign.getZ() - a.door.getZ());
            int ux = Math.abs(iz), uz = Math.abs(ix);
            BlockPos f = a.door.offset(ux * 2 - ix, 0, uz * 2 - iz);
            level.setBlock(f, ModRegistry.FORGE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Разборочный стол в каждой квартире (на месте бочки у окна). */
    public static void placeDismantlers(ServerLevel level, CivData data) {
        for (CivData.Apartment a : data.apartments) {
            int ix = Integer.signum(a.sign.getX() - a.door.getX()), iz = Integer.signum(a.sign.getZ() - a.door.getZ());
            int ux = Math.abs(iz), uz = Math.abs(ix);
            BlockPos f = a.door.offset(ux * 2 - ix * 5, 0, uz * 2 - iz * 5);
            level.setBlock(f, ModRegistry.DISMANTLE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void updateApartmentSign(ServerLevel level, CivData.Apartment a) {
        DyeColor c = DyeColor.LIGHT_BLUE;
        writeSign(level, a.sign, c, "", "Кв. " + a.number, a.ownerName.isEmpty() ? "свободна" : a.ownerName, "");
    }
}
