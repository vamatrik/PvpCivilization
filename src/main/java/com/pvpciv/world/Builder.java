package com.pvpciv.world;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Общие кисти для процедурных локаций. */
public abstract class Builder {
    protected final ServerLevel level;
    protected final RandomSource rnd;

    protected Builder(ServerLevel level, long seed) { this.level = level; this.rnd = RandomSource.create(seed); }

    protected void set(int x, int y, int z, BlockState s) { level.setBlock(new BlockPos(x, y, z), s, Block.UPDATE_CLIENTS); }
    protected void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }
    protected void setLit(int x, int y, int z, BlockState s) { level.setBlock(new BlockPos(x, y, z), s, Block.UPDATE_ALL); }
    protected Block pick(Block... b) { return b[rnd.nextInt(b.length)]; }
    protected boolean air(int x, int y, int z) { return level.getBlockState(new BlockPos(x, y, z)).isAir(); }

    protected static double dist(int x, int z, BlockPos c) { return Math.sqrt(Math.pow(x - c.getX(), 2) + Math.pow(z - c.getZ(), 2)); }
    protected static double dist(double x, double z, double cx, double cz) { return Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)); }

    /** Верхний непустой блок колонки в диапазоне [lo, hi]. */
    protected int top(int x, int z, int lo, int hi) {
        for (int y = hi; y >= lo; y--) if (!air(x, y, z)) return y;
        return Integer.MIN_VALUE;
    }

    protected void clearBox(int x0, int x1, int y0, int y1, int z0, int z1) {
        BlockState a = Blocks.AIR.defaultBlockState();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) {
            BlockPos p = new BlockPos(x, y, z);
            if (!level.getBlockState(p).isAir()) level.setBlock(p, a, Block.UPDATE_CLIENTS);
        }
    }

    protected void sign(BlockPos pos, Direction facing, DyeColor c, String... lines) {
        level.setBlock(pos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), Block.UPDATE_ALL);
        DormBuilder.writeSign(level, pos, c, lines);
    }

    protected void signPost(int x, int y, int z, Direction facing, DyeColor c, Block post, String... lines) {
        set(x, y, z, post);
        set(x, y + 1, z, post);
        sign(new BlockPos(x, y + 1, z).relative(facing), facing, c, lines);
    }

    protected void stash(int x, int y, int z, List<BlockPos> out) {
        set(x, y, z, ModRegistry.STASH.get());
        out.add(new BlockPos(x, y, z));
    }

    /** Мёртвое дерево с ветками. */
    protected void deadTree(int x, int t, int z, Block log, Block twig, int hMin, int hVar) {
        int h = hMin + rnd.nextInt(hVar + 1);
        for (int k = 1; k <= h; k++) set(x, t + k, z, log);
        for (Direction d : Direction.Plane.HORIZONTAL) if (rnd.nextBoolean()) {
            int bx = x + d.getStepX(), bz = z + d.getStepZ(), by = t + h - 1 - rnd.nextInt(2);
            set(bx, by, bz, log.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS)
                    ? log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, d.getAxis()) : log.defaultBlockState());
            set(bx + d.getStepX(), by + 1, bz + d.getStepZ(), twig);
        }
    }

    /** Разрушенная постройка из блоков walls, внутри — тайник. */
    protected void ruin(int cx, int base, int cz, int r, Block floorA, Block floorB, Block[] walls, Block light, List<BlockPos> stashes, boolean withStash) {
        for (int x = cx - r; x <= cx + r; x++) for (int z = cz - r; z <= cz + r; z++) {
            set(x, base, z, rnd.nextFloat() < 0.7f ? floorA : floorB);
            for (int y = base + 1; y <= base + 6; y++) set(x, y, z, Blocks.AIR);
            boolean wall = Math.abs(x - cx) == r || Math.abs(z - cz) == r;
            if (wall) {
                int hh = rnd.nextInt(5);
                for (int y = 1; y <= hh; y++) set(x, base + y, z, walls[rnd.nextInt(walls.length)]);
            }
        }
        if (withStash) stash(cx, base + 1, cz, stashes);
        set(cx + 1, base + 1, cz + 1, Blocks.COBWEB);
        set(cx - r + 1, base + 1, cz - r + 1, light);
    }
}
