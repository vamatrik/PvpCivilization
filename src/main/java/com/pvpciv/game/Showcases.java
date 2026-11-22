package com.pvpciv.game;

import com.pvpciv.block.TrophyShelfBlock;
import com.pvpciv.block.TrophyShelfBlockEntity;
import com.pvpciv.data.CivData;
import com.pvpciv.data.StoryData;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModF4;
import com.pvpciv.registry.ModF5;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.registry.ModV11;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 1.1: витрины трофеев в квартире — по секции на этаж (1–3 блока), на стенах на высоте глаз.
 * Левая стена: этаж 1 (2), этаж 2 (2), этаж 4 (1). Правая: этаж 3 (2), этаж 5 (3). Всего 10 мест.
 */
public final class Showcases {
    public record Trophy(int bit, int floor, String name, Supplier<? extends Item> item) {}
    public static final List<Trophy> ALL = List.of(
            new Trophy(1, 1, "Гнилой Громила", ModRegistry.BRUTE_TROPHY), new Trophy(2, 1, "Хранительница Склепа", ModRegistry.KEEPER_TROPHY),
            new Trophy(4, 2, "Дон Витторио", ModRegistry.DON_TROPHY), new Trophy(8, 2, "Крысиный король", ModRegistry.RAT_KING_TROPHY),
            new Trophy(16, 3, "Братья-каппы", ModRegistry.KAPPA_TROPHY), new Trophy(32, 3, "Кумихо", ModRegistry.KUMIHO_TROPHY),
            new Trophy(64, 4, "Доктор Ноэль и Лиана", ModF4.NAZA_TROPHY),
            new Trophy(128, 5, "Инопланетный дипломат", ModF5.DIPLOMAT_TROPHY), new Trophy(256, 5, "Галактический червь", ModF5.WORM_TROPHY),
            new Trophy(512, 5, "Демоница Пустоты", ModV11.DEMON_TROPHY));

    public static int bitOf(Item it) { for (Trophy t : ALL) if (t.item.get() == it) return t.bit; return 0; }
    public static Trophy of(int bit) { for (Trophy t : ALL) if (t.bit == bit) return t; return null; }
    public static String name(int bit) { Trophy t = of(bit); return t == null ? null : t.name; }
    public static Item item(int bit) { Trophy t = of(bit); return t == null ? null : t.item.get(); }

    /** Ячейка: позиция, направление «в комнату», этаж-стиль, бит, часть секции. */
    public record Cell(BlockPos pos, Direction facing, int style, int bit, TrophyShelfBlock.Part part) {}

    // [стена (−1 левая/+1 правая), глубина 1..5, бит]
    private static final int[][] LAYOUT = {
            {-1, 1, 1}, {-1, 2, 2}, {-1, 3, 4}, {-1, 4, 8}, {-1, 5, 64},
            {1, 1, 16}, {1, 2, 32}, {1, 3, 128}, {1, 4, 256}, {1, 5, 512}};

    public static List<Cell> cells(CivData.Apartment a) {
        int ix = Integer.signum(a.sign.getX() - a.door.getX()), iz = Integer.signum(a.sign.getZ() - a.door.getZ());
        int ux = Math.abs(iz), uz = Math.abs(ix);
        List<Cell> out = new ArrayList<>();
        for (int[] c : LAYOUT) {
            int du = c[0] * 2, k = c[1];
            BlockPos p = a.door.offset(ux * du - ix * k, 2, uz * du - iz * k);
            Direction f = Direction.fromDelta(-c[0] * ux, 0, -c[0] * uz);
            int style = of(c[2]).floor;
            out.add(new Cell(p, f, style, c[2], TrophyShelfBlock.Part.SINGLE));
        }
        // части секций: сосед того же этажа по оси модели (+x модели = facing по часовой)
        List<Cell> res = new ArrayList<>();
        for (Cell c : out) {
            Direction px = c.facing.getClockWise();
            boolean plus = false, minus = false;
            for (Cell o : out) {
                if (o.style != c.style) continue;
                if (o.pos.equals(c.pos.relative(px))) plus = true;
                if (o.pos.equals(c.pos.relative(px.getOpposite()))) minus = true;
            }
            var part = plus && minus ? TrophyShelfBlock.Part.MIDDLE : plus ? TrophyShelfBlock.Part.LEFT : minus ? TrophyShelfBlock.Part.RIGHT : TrophyShelfBlock.Part.SINGLE;
            res.add(new Cell(c.pos, c.facing, c.style, c.bit, part));
        }
        return res;
    }

    /** Ставит витрины во все квартиры игроков, убирает старую витрину-ящик. */
    public static void build(ServerLevel l, CivData data) {
        int n = 0;
        for (CivData.Apartment a : data.apartments) {
            if (!a.npc.isEmpty()) continue;
            n++;
            for (Cell c : cells(a)) {
                BlockState s = ModV11.TROPHY_SHELF.get().defaultBlockState().setValue(TrophyShelfBlock.FACING, c.facing)
                        .setValue(TrophyShelfBlock.STYLE, c.style).setValue(TrophyShelfBlock.PART, c.part);
                l.setBlock(c.pos, s, Block.UPDATE_ALL);
                if (l.getBlockEntity(c.pos) instanceof TrophyShelfBlockEntity be) be.set(c.bit, false);
            }
            // старая витрина 0.8 (у сундука) — убрать, её трофеи переедут на полки
            BlockPos.betweenClosedStream(a.door.offset(-6, 0, -6), a.door.offset(6, 0, 6)).forEach(q -> {
                if (l.getBlockState(q).is(ModRegistry.TROPHY_CASE.get())) l.setBlock(q.immutable(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            });
        }
        com.mojang.logging.LogUtils.getLogger().info("[PvpCiv] 1.1: витрины трофеев в {} квартирах ({})", n, l.dimension().location());
    }

    public static CivData.Apartment home(ServerPlayer p) {
        for (CivData.Apartment a : CivData.get(p.server).apartments) if (p.getUUID().equals(a.owner)) return a;
        return null;
    }

    /** Синхронизирует выставленные трофеи владельца с полками его квартиры. */
    public static void refresh(ServerPlayer p) {
        CivData.Apartment a = home(p);
        if (a == null) return;
        ServerLevel l = CivWorld.level(p.server);
        int shown = Progress.d(p).showcase;
        for (Cell c : cells(a))
            if (l.isLoaded(c.pos) && l.getBlockEntity(c.pos) instanceof TrophyShelfBlockEntity be) be.set(c.bit, (shown & c.bit) != 0);
    }

    /** Раз в несколько секунд: хозяин рядом со своей квартирой — подтянуть полки (чанк мог быть не загружен при входе). */
    public static void refreshNear(ServerPlayer p) {
        CivData.Apartment a = home(p);
        if (a != null && p.level() == CivWorld.level(p.server) && p.blockPosition().distSqr(a.door) < 20 * 20) refresh(p);
    }

    public static void place(ServerPlayer sp, BlockPos pos, ItemStack st, int bit) {
        if (!HomeChest.canUse(sp, pos)) { sp.displayClientMessage(Component.literal("Это чужая витрина.").withStyle(ChatFormatting.RED), true); return; }
        StoryData d = Progress.d(sp);
        if ((d.showcase & bit) != 0) { sp.displayClientMessage(Component.literal("Этот трофей уже стоит на витрине.").withStyle(ChatFormatting.GRAY), true); return; }
        d.showcase |= bit;
        sp.setData(ModRegistry.STORY.get(), d);
        st.shrink(1);
        refresh(sp);
        Trophy t = of(bit);
        ServerLevel l = sp.serverLevel();
        CivData.Apartment a = home(sp);
        if (a != null) for (Cell c : cells(a)) if (c.bit == bit)
            l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.pos.getX() + 0.5, c.pos.getY() + 0.5, c.pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.3);
        l.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.7f, 1.4f);
        sp.sendSystemMessage(Component.literal("🏆 «" + t.name + "» — на витрине этажа " + t.floor + "!").withStyle(ChatFormatting.GOLD));
        Story.sync(sp);
    }
}
