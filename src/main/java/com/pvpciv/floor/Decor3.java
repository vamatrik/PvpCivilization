package com.pvpciv.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * «Живые» детали этажа 3 (0.9.0): лепестки сакуры под деревьями, трава, папоротники, цветы, камешки, кусты —
 * всё только на свободной траве острова, дорожки и пол не трогает.
 */
public final class Decor3 {
    private Decor3() {}

    /** Пройтись по траве в круге (cx, cz, r) на уровне y. autumn — осенняя палитра (деревня Кумихо). */
    public static void scatter(ServerLevel l, RandomSource rnd, int cx, int y, int cz, int r, boolean autumn) {
        for (int x = cx - r; x <= cx + r; x++) for (int z = cz - r; z <= cz + r; z++) {
            if ((x - cx) * (x - cx) + (z - cz) * (z - cz) > r * r) continue;
            BlockPos g = new BlockPos(x, y - 1, z), at = g.above();
            if (!l.getBlockState(g).is(Blocks.GRASS_BLOCK) || !l.getBlockState(at).isAir()) continue;
            int sak = nearCherry(l, x, y, z);
            float f = rnd.nextFloat();
            BlockState s = null;
            if (sak > 0 && f < (sak == 1 ? 0.75f : 0.35f))
                s = Blocks.PINK_PETALS.defaultBlockState().setValue(PinkPetalsBlock.AMOUNT, 1 + rnd.nextInt(sak == 1 ? 4 : 2))
                        .setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue(rnd.nextInt(4)));
            else if (f < 0.10f) s = Blocks.SHORT_GRASS.defaultBlockState();
            else if (f < 0.125f) s = Blocks.FERN.defaultBlockState();
            else if (f < 0.145f) s = pick(rnd, autumn ? new Block[]{Blocks.ORANGE_TULIP, Blocks.RED_TULIP, Blocks.TORCHFLOWER, Blocks.DANDELION}
                    : new Block[]{Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.LILY_OF_THE_VALLEY, Blocks.PINK_TULIP, Blocks.CORNFLOWER}).defaultBlockState();
            else if (f < 0.150f) s = (autumn ? Blocks.MOSS_CARPET : Blocks.MOSSY_COBBLESTONE_SLAB).defaultBlockState();
            else if (f < 0.153f) s = Blocks.FLOWERING_AZALEA.defaultBlockState();
            else if (autumn && f < 0.17f) s = Blocks.RED_MUSHROOM.defaultBlockState();
            if (s != null) l.setBlock(at, s, 2);
        }
    }

    /** 1 — прямо под кроной сакуры, 2 — рядом, 0 — далеко. */
    private static int nearCherry(ServerLevel l, int x, int y, int z) {
        int best = 0;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (!l.getBlockState(new BlockPos(x + dx, y + 4, z + dz)).is(Blocks.CHERRY_LEAVES) && !l.getBlockState(new BlockPos(x + dx, y + 5, z + dz)).is(Blocks.CHERRY_LEAVES)) continue;
            int d = Math.abs(dx) + Math.abs(dz);
            if (d <= 2) return 1;
            best = 2;
        }
        return best;
    }

    private static Block pick(RandomSource r, Block[] b) { return b[r.nextInt(b.length)]; }

    /** Обстановка домика: татами, низкий столик с фонарём, подушки, бочка, полка. */
    public static void interior(ServerLevel l, RandomSource rnd, int cx, int y, int cz, int w, int d) {
        for (int x = -w + 1; x <= w - 1; x++) for (int z = -d + 1; z <= d - 1; z++)
            l.setBlock(new BlockPos(cx + x, y - 1, cz + z), ((x + z) & 1) == 0 ? Blocks.BAMBOO_MOSAIC.defaultBlockState() : Blocks.STRIPPED_BAMBOO_BLOCK.defaultBlockState(), 2);
        put(l, cx, y, cz, Blocks.SPRUCE_SLAB.defaultBlockState());
        put(l, cx, y + 1, cz, Blocks.LANTERN.defaultBlockState());
        put(l, cx - 1, y, cz, Blocks.RED_CARPET.defaultBlockState());
        put(l, cx + 1, y, cz, Blocks.RED_CARPET.defaultBlockState());
        put(l, cx + w - 1, y, cz - d + 1, Blocks.BARREL.defaultBlockState());
        put(l, cx - w + 1, y, cz + d - 1, rnd.nextBoolean() ? Blocks.BOOKSHELF.defaultBlockState() : Blocks.DECORATED_POT.defaultBlockState());
        put(l, cx + w - 1, y, cz + d - 1, Blocks.FLOWER_POT.defaultBlockState());
    }

    private static void put(ServerLevel l, int x, int y, int z, BlockState s) {
        BlockPos p = new BlockPos(x, y, z);
        if (l.getBlockState(p).isAir()) l.setBlock(p, s, 2);
    }

    /** Мелочи у дома снаружи: бочки, сено, бамбуковый забор. */
    public static void yard(ServerLevel l, RandomSource rnd, int cx, int y, int cz, int w, int d) {
        int side = rnd.nextBoolean() ? 1 : -1;
        put(l, cx + side * (w + 1), y, cz + d - 1, Blocks.BARREL.defaultBlockState());
        if (rnd.nextBoolean()) put(l, cx + side * (w + 1), y, cz + d - 2, Blocks.HAY_BLOCK.defaultBlockState());
        for (int z = -d; z <= d - 3; z++) if (rnd.nextFloat() < 0.8f) put(l, cx - side * (w + 2), y, cz + z, Blocks.BAMBOO_FENCE.defaultBlockState());
    }
}
