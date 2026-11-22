package com.pvpciv.world;

import com.pvpciv.block.CivSignBlock;
import com.pvpciv.block.CivSignBlockEntity;
import com.pvpciv.registry.ModV11;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 1.1: стилизованные таблички вместо ванильных. Старые ванильные в мирах мода переделываются при загрузке чанка. */
public final class Signs {
    private Signs() {}

    /** Стиль по месту: 0 — общежитие (и подвал), 1..5 — этажи. */
    public static int styleAt(Level l, BlockPos p) {
        int f = CivWorld.floor(l);
        if (f == 1 && Math.abs(p.getX()) < 48 && Math.abs(p.getZ()) < 48) return 0;
        return Math.max(0, Math.min(5, f));
    }

    public static int color(DyeColor c) {
        if (c == null) return 0xFFFFFF;
        return switch (c) {
            case BLACK -> 0xE0E0E0; case GRAY -> 0xC8C8C8; case LIGHT_GRAY -> 0xE8E8E8; case BROWN -> 0xE0B080;
            default -> c.getTextColor();
        };
    }

    public static void place(ServerLevel l, BlockPos pos, Direction facing, int style, String... lines) { place(l, pos, facing, style, false, 0xFFE8C0, lines); }

    public static void place(ServerLevel l, BlockPos pos, Direction facing, int style, boolean standing, int color, String... lines) {
        l.setBlock(pos, ModV11.CIV_SIGN.get().defaultBlockState().setValue(CivSignBlock.FACING, facing)
                .setValue(CivSignBlock.STYLE, style).setValue(CivSignBlock.STANDING, standing), Block.UPDATE_ALL);
        if (l.getBlockEntity(pos) instanceof CivSignBlockEntity be) be.set(color, lines);
    }

    /** Ванильную табличку — в стилизованную (текст и цвет сохраняются). true — переделана. */
    public static boolean convert(ServerLevel l, BlockPos pos) {
        BlockState st = l.getBlockState(pos);
        BlockEntity be = l.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity sb)) return false;
        Direction f; boolean standing;
        if (st.getBlock() instanceof WallSignBlock) { f = st.getValue(WallSignBlock.FACING); standing = false; }
        else if (st.getBlock() instanceof StandingSignBlock) { f = Direction.from2DDataValue(Math.round(st.getValue(StandingSignBlock.ROTATION) / 4f) % 4).getOpposite(); standing = true; }
        else return false;
        var t = sb.getFrontText();
        String[] ls = new String[4];
        for (int i = 0; i < 4; i++) ls[i] = t.getMessage(i, false).getString();
        int c = color(t.getColor());
        place(l, pos.immutable(), f, styleAt(l, pos), standing, c, ls);
        return true;
    }

    /** Записать строки в табличку (любую — ванильную или нашу). */
    public static void write(ServerLevel l, BlockPos pos, DyeColor c, String... lines) {
        if (l.getBlockEntity(pos) instanceof CivSignBlockEntity be) { be.set(color(c), lines); return; }
        DormBuilder.writeSign(l, pos, c, lines);
    }
    public static void line(ServerLevel l, BlockPos pos, int i, String s) {
        if (l.getBlockEntity(pos) instanceof CivSignBlockEntity be) be.line(i, s);
    }

    // ---- переделка старых табличек при загрузке чанков
    private static final ArrayDeque<Object[]> QUEUE = new ArrayDeque<>();

    public static void onChunkLoad(ServerLevel l, LevelChunk ch) {
        if (!CivWorld.is(l)) return;
        for (Map.Entry<BlockPos, BlockEntity> e : ch.getBlockEntities().entrySet())
            if (e.getValue() instanceof SignBlockEntity) QUEUE.add(new Object[]{l, e.getKey().immutable()});
    }

    public static void tick(MinecraftServer s) {
        int n = 0;
        while (!QUEUE.isEmpty() && n++ < 64) {
            Object[] q = QUEUE.poll();
            ServerLevel l = (ServerLevel) q[0];
            BlockPos p = (BlockPos) q[1];
            if (l.isLoaded(p)) convert(l, p);
        }
    }

    /** Все таблички в радиусе (для только что построенного). */
    public static void convertAround(ServerLevel l, BlockPos c, int r) {
        List<BlockPos> todo = new ArrayList<>();
        for (int cx = (c.getX() - r) >> 4; cx <= (c.getX() + r) >> 4; cx++) for (int cz = (c.getZ() - r) >> 4; cz <= (c.getZ() + r) >> 4; cz++) {
            if (!l.hasChunk(cx, cz)) continue;
            for (var e : l.getChunk(cx, cz).getBlockEntities().entrySet()) if (e.getValue() instanceof SignBlockEntity) todo.add(e.getKey());
        }
        for (BlockPos p : todo) convert(l, p);
    }
}
