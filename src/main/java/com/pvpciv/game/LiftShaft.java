package com.pvpciv.game;

import com.pvpciv.data.CivData;
import com.pvpciv.floor.Floor5;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.1: после ремонта лифта станции шахта в атриуме раскрывается: пол расходится, вниз уходит труба шахты с монорельсом,
 * кабина опускается в красное свечение — к этажу 6 (Ад, скоро). Потом шахта закрыта стеклянным люком — видно глубину.
 */
public final class LiftShaft {
    private LiftShaft() {}
    public static final String FLAG = "shaft5_open";
    private static final int DEPTH = 44;
    private static int t = -1;
    private static final List<Entity> CAB = new ArrayList<>();
    private static final List<Vec3> OFF = new ArrayList<>();

    public static boolean isOpen(MinecraftServer s) { return CivData.get(s).bossNext.containsKey(FLAG); }

    private static BlockPos c() { return Floor5.HUB.center(); }

    /** Запуск сцены (ремонт пультом). Если шахта уже открыта — сразу кабина вниз. */
    public static void start(ServerPlayer by) {
        if (t >= 0) return;
        t = 0;
        ServerLevel l = CivWorld.f5(by.server);
        for (ServerPlayer p : l.players()) if (p.distanceToSqr(Vec3.atCenterOf(c())) < 40 * 40)
            Story.title(p, "Шахта открывается", "лифт уходит вниз — к этажу 6", 0xFF6040);
    }

    public static void tick(MinecraftServer server) {
        if (t < 0) return;
        ServerLevel l = CivWorld.f5(server);
        BlockPos c = c();
        int y0 = Floor5.Y;
        t++;
        if (t % 10 == 0 && t < 120) l.playSound(null, c, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 2f, 0.4f);
        if (t < 40) { l.sendParticles(ParticleTypes.LAVA, c.getX() + 0.5, y0, c.getZ() + 0.5, 2, 1.5, 0.1, 1.5, 0); return; }
        // 40..80: пол расходится кольцами от центра
        if (t <= 80) {
            double r = (t - 40) / 40.0 * 3.5;
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                double d = Math.hypot(x, z);
                if (d > r || d > 3.5) continue;
                BlockPos q = c.offset(x, -1, z);
                if (!l.getBlockState(q).isAir()) {
                    l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                    l.setBlock(q.below(), Blocks.AIR.defaultBlockState(), 2);
                    l.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
                }
            }
            if (t == 80) { buildShaft(l, false); l.playSound(null, c, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 2f, 0.5f); }
            return;
        }
        // 80..100: монорельс выдвигается вниз
        if (t == 100) {
            spawnCabin(l);
            l.playSound(null, c, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 0.6f);
        }
        if (t > 100 && t < 240) {
            double dy = -Math.pow((t - 100) / 140.0, 2) * (DEPTH + 6);
            Vec3 o = Vec3.atLowerCornerOf(c).add(-1, y0 + dy, -1);
            for (int i = 0; i < CAB.size(); i++) { Vec3 a = o.add(OFF.get(i)); CAB.get(i).teleportTo(a.x, a.y, a.z); }
            if (t % 3 == 0) l.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.getX() + 0.5, y0 + dy + 0.5, c.getZ() + 0.5, 6, 1.2, 0.2, 1.2, 0.1);
            if (t % 20 == 0) l.playSound(null, c, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 2f, 0.5f);
        }
        if (t == 240) {
            for (Entity e : CAB) { Rocket.KEEP.remove(e); e.discard(); }
            CAB.clear(); OFF.clear();
            l.playSound(null, c, SoundEvents.WARDEN_ROAR, SoundSource.BLOCKS, 1.5f, 0.6f);
            l.sendParticles(ParticleTypes.FLAME, c.getX() + 0.5, y0 - DEPTH + 2, c.getZ() + 0.5, 80, 1.5, 0.5, 1.5, 0.1);
        }
        if (t == 260) {
            buildShaft(l, true);
            CivData d = CivData.get(server); d.bossNext.put(FLAG, 1L); d.setDirty();
            for (ServerPlayer p : l.players()) if (p.distanceToSqr(Vec3.atCenterOf(c)) < 40 * 40)
                p.sendSystemMessage(Component.literal("✦ Кабина ушла вниз, в красное зарево. Оттуда пахнет серой. Этаж 6 — скоро.").withStyle(ChatFormatting.RED));
            t = -1;
        }
    }

    private static void spawnCabin(ServerLevel l) {
        CAB.clear(); OFF.clear();
        BlockState frame = Blocks.POLISHED_DEEPSLATE.defaultBlockState(), glass = Blocks.RED_STAINED_GLASS.defaultBlockState(), lamp = Blocks.SHROOMLIGHT.defaultBlockState();
        Object[][] parts = {
                {frame, 0f, 0f, 0f, 3f, 0.4f, 3f}, {frame, 0f, 3.2f, 0f, 3f, 0.4f, 3f},
                {glass, 0.05f, 0.4f, 0.05f, 2.9f, 2.8f, 2.9f}, {lamp, 1.1f, 3.6f, 1.1f, 0.8f, 0.4f, 0.8f},
                {frame, 0f, 0.4f, 0f, 0.3f, 2.8f, 0.3f}, {frame, 2.7f, 0.4f, 0f, 0.3f, 2.8f, 0.3f}, {frame, 0f, 0.4f, 2.7f, 0.3f, 2.8f, 0.3f}, {frame, 2.7f, 0.4f, 2.7f, 0.3f, 2.8f, 0.3f}};
        Vec3 o = Vec3.atLowerCornerOf(c()).add(-1, Floor5.Y, -1);
        for (Object[] p : parts) {
            Entity e = Rocket.display(l, (BlockState) p[0], (float) p[4], (float) p[5], (float) p[6]);
            if (e == null) continue;
            Vec3 off = new Vec3((float) p[1], (float) p[2], (float) p[3]);
            Vec3 a = o.add(off);
            e.moveTo(a.x, a.y, a.z, 0, 0);
            Rocket.KEEP.add(e);
            l.addFreshEntity(e);
            CAB.add(e); OFF.add(off);
        }
    }

    /** Труба шахты вниз, монорельс по центру, на дне — зарево. closed — стеклянный люк сверху. */
    public static void buildShaft(ServerLevel l, boolean closed) {
        BlockPos c = c();
        int y0 = Floor5.Y;
        for (int y = 1; y <= DEPTH; y++) for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            double d = Math.hypot(x, z);
            if (d > 4.6) continue;
            BlockPos q = c.offset(x, -y, z);
            boolean wall = d > 3.6;
            BlockState s = wall ? (y % 6 == 3 && (x == 0 || z == 0) ? Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true)
                    : y % 2 == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState()) : Blocks.AIR.defaultBlockState();
            if (y == DEPTH) s = d < 3 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.CRYING_OBSIDIAN.defaultBlockState();
            if (y == DEPTH - 1 && !wall) s = d < 1.5 ? Blocks.SHROOMLIGHT.defaultBlockState() : Blocks.AIR.defaultBlockState();
            l.setBlock(q, s, 2);
        }
        // монорельс: балка с цепью по краю шахты
        for (int y = 0; y < DEPTH - 1; y++) {
            l.setBlock(c.offset(3, -y - 1, 0), Blocks.IRON_BLOCK.defaultBlockState(), 2);
            l.setBlock(c.offset(-3, -y - 1, 0), Blocks.IRON_BLOCK.defaultBlockState(), 2);
        }
        // люк
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            double d = Math.hypot(x, z);
            if (d > 3.5) continue;
            BlockPos q = c.offset(x, -1, z);
            if (closed) l.setBlock(q, d > 2.9 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.RED_STAINED_GLASS.defaultBlockState(), 2);
            else l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
            if (d < 3.4) l.setBlock(q.below(), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    /** После перезапуска: если шахта была открыта — пересобрать (на случай сцены, прерванной выключением). */
    public static void startup(MinecraftServer s) {
        if (isOpen(s)) buildShaft(CivWorld.f5(s), true);
    }
}
