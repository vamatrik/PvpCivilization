package com.pvpciv.world;

import com.pvpciv.data.CivData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Ледяная Трасса — узкая извилистая ледяная дорога от северной пристани к Инеевому Кряжу.
 * Сплайн Катмулла–Рома по опорным точкам; на ней идёт заезд на время (старт у пристани, финиш у Кряжа).
 */
public final class IceTrack extends Builder {
    public static final int Y = DormBuilder.Y0;
    private static final double[][] CP = {
            {3, -25}, {3, -39}, {3, -58}, {12, -80}, {30, -100}, {42, -125}, {31, -150}, {2, -160}, {-27, -170}, {-44, -192},
            {-36, -218}, {-10, -228}, {18, -238}, {40, -255}, {46, -280}, {28, -300}, {4, -306}, {-18, -318}, {-26, -340},
            {-11, -358}, {3, -369}, {3, -382}, {3, -396}};
    /** Точки трассы через ~0.5 блока: x, z, полуширина. */
    public static final List<double[]> PTS = new ArrayList<>();
    public static int START, FINISH;

    static {
        for (int i = 1; i < CP.length - 2; i++) {
            double[] p0 = CP[i - 1], p1 = CP[i], p2 = CP[i + 1], p3 = CP[i + 2];
            double len = Math.hypot(p2[0] - p1[0], p2[1] - p1[1]);
            int n = (int) Math.ceil(len * 2);
            for (int k = 0; k < n; k++) {
                double t = k / (double) n, t2 = t * t, t3 = t2 * t;
                double x = 0.5 * (2 * p1[0] + (-p0[0] + p2[0]) * t + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2 + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3);
                double z = 0.5 * (2 * p1[1] + (-p0[1] + p2[1]) * t + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2 + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3);
                PTS.add(new double[]{x, z, 0});
            }
        }
        for (int i = 0; i < PTS.size(); i++) {
            double[] p = PTS.get(i);
            double fromEnd = Math.min(i, PTS.size() - 1 - i) * 0.5;
            p[2] = fromEnd < 26 ? 2.6 : 1.75;    // у пристани и у Кряжа шире, в середине — узко
        }
        START = 14;
        FINISH = PTS.size() - 14;
    }

    private IceTrack(ServerLevel level) { super(level, 777L); }

    /** Индекс ближайшей точки трассы и расстояние до неё (поиск вокруг подсказки hint, -1 — полный). */
    public static double[] nearest(double x, double z, int hint) {
        int lo = 0, hi = PTS.size() - 1;
        if (hint >= 0) { lo = Math.max(0, hint - 30); hi = Math.min(PTS.size() - 1, hint + 60); }
        int best = -1; double bd = Double.MAX_VALUE;
        for (int i = lo; i <= hi; i++) {
            double[] p = PTS.get(i);
            double d = (p[0] - x) * (p[0] - x) + (p[1] - z) * (p[1] - z);
            if (d < bd) { bd = d; best = i; }
        }
        return new double[]{best, Math.sqrt(bd)};
    }

    public static boolean near(Vec3 p, double pad) {
        if (p.z > -34 || p.z < -390 || p.x < -60 || p.x > 64) return false;
        return nearest(p.x, p.z, -1)[1] <= 3 + pad;
    }

    public static void build(ServerLevel level) {
        IceTrack b = new IceTrack(level);
        Set<Long> core = new HashSet<>(), edge = new HashSet<>();
        for (double[] p : PTS) {
            double hw = p[2];
            for (int x = (int) Math.floor(p[0] - hw - 1); x <= (int) Math.ceil(p[0] + hw + 1); x++)
                for (int z = (int) Math.floor(p[1] - hw - 1); z <= (int) Math.ceil(p[1] + hw + 1); z++) {
                    double d = Math.hypot(x + 0.5 - p[0], z + 0.5 - p[1]);
                    long key = BlockPos.asLong(x, 0, z);
                    if (d <= hw - 0.7) core.add(key);
                    else if (d <= hw) edge.add(key);
                }
        }
        edge.removeAll(core);
        for (long k : edge) { BlockPos p = BlockPos.of(k); b.set(p.getX(), Y - 1, p.getZ(), Blocks.BLUE_ICE); }
        for (long k : core) { BlockPos p = BlockPos.of(k); b.set(p.getX(), Y - 1, p.getZ(), Blocks.PACKED_ICE); }
        // подсветка снизу и сосульки
        for (int i = 0; i < PTS.size(); i += 12) {
            double[] p = PTS.get(i);
            int x = (int) Math.floor(p[0]), z = (int) Math.floor(p[1]);
            b.set(x, Y - 2, z, Blocks.PACKED_ICE);
            b.set(x, Y - 3, z, i % 24 == 0 ? Blocks.SEA_LANTERN : Blocks.ICE);
            if (i % 24 == 0) b.set(x, Y - 4, z, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN));
        }
        b.gate(START, "СТАРТ", "Ледяная Трасса", "Лодка: ПКМ по льду", "Заезд на время!");
        b.gate(FINISH, "ФИНИШ", "Инеевый Кряж", "Этаж 1 • ур. 3–5", "Мобы сильнее!");
        // клетчатые линии
        for (int idx : new int[]{START, FINISH}) {
            double[] p = PTS.get(idx);
            for (int x = (int) Math.floor(p[0] - 3); x <= (int) Math.ceil(p[0] + 3); x++)
                for (int dz = 0; dz <= 1; dz++) {
                    int z = (int) Math.floor(p[1]) - dz;
                    if (core.contains(BlockPos.asLong(x, 0, z)) || edge.contains(BlockPos.asLong(x, 0, z)))
                        b.set(x, Y - 1, z, ((x + z) & 1) == 0 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE);
                }
        }
    }

    private void gate(int idx, String l1, String l2, String l3, String l4) {
        double[] p = PTS.get(idx);
        int cx = (int) Math.floor(p[0]), cz = (int) Math.floor(p[1]);
        int w = (int) Math.ceil(p[2]) + 2;
        for (int y = Y - 1; y <= Y + 4; y++) { set(cx - w, y, cz, Blocks.QUARTZ_PILLAR); set(cx + w, y, cz, Blocks.QUARTZ_PILLAR); }
        for (int x = cx - w; x <= cx + w; x++) set(x, Y + 5, cz, (x - cx) % 2 == 0 ? Blocks.SEA_LANTERN : Blocks.LIGHT_BLUE_STAINED_GLASS);
        sign(new BlockPos(cx - w, Y + 2, cz + 1), Direction.SOUTH, DyeColor.LIGHT_BLUE, l1, l2, l3, l4);
        sign(new BlockPos(cx + w, Y + 2, cz - 1), Direction.NORTH, DyeColor.LIGHT_BLUE, l1, l2, l3, l4);
    }

    // ------------------------------------------------------------------ заезд на время
    private static final class Run { int idx = -1; long start = -1; }
    private static final Map<UUID, Run> RUNS = new HashMap<>();

    public static void tick(MinecraftServer server) {
        ServerLevel lvl = CivWorld.level(server);
        long now = lvl.getGameTime();
        for (ServerPlayer p : lvl.players()) {
            boolean inBoat = p.getVehicle() instanceof Boat;
            Run r = RUNS.get(p.getUUID());
            if (!inBoat || p.getZ() > -30 || p.getZ() < -395) { if (r != null && r.start >= 0) fail(p, "Заезд прерван"); RUNS.remove(p.getUUID()); continue; }
            if (r == null) { r = new Run(); RUNS.put(p.getUUID(), r); }
            double[] n = nearest(p.getX(), p.getZ(), r.idx);
            int idx = (int) n[0];
            if (n[1] > PTS.get(idx)[2] + 2.5 || p.getY() < Y - 3) {
                if (r.start >= 0) fail(p, "Вылет с трассы!");
                r.start = -1; r.idx = idx; continue;
            }
            if (r.idx >= 0 && idx - r.idx > 40) { r.start = -1; }  // телепорт/срезка — не считается
            if (r.start < 0 && r.idx >= 0 && r.idx < START && idx >= START) {
                r.start = now;
                p.displayClientMessage(Component.literal("⏱ СТАРТ!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
                lvl.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 1.6f);
            }
            if (r.start >= 0) {
                long t = now - r.start;
                if (idx >= FINISH) { finish(p, t); r.start = -1; }
                else if (now % 2 == 0) {
                    long best = CivData.get(server).trackBest.getOrDefault(p.getUUID(), 0L);
                    p.displayClientMessage(Component.literal("⏱ " + fmt(t)).withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(best > 0 ? "   личный рекорд " + fmt(best) : "").withStyle(ChatFormatting.GRAY)), true);
                }
            }
            if (idx < r.idx - 20) r.start = -1;   // развернулся назад
            r.idx = idx;
        }
    }

    private static void fail(ServerPlayer p, String why) {
        p.displayClientMessage(Component.literal("⏱ " + why).withStyle(ChatFormatting.RED), true);
    }

    private static void finish(ServerPlayer p, long ticks) {
        CivData d = CivData.get(p.server);
        long best = d.trackBest.getOrDefault(p.getUUID(), 0L);
        boolean pb = best == 0 || ticks < best;
        boolean wr = d.trackRecord == 0 || ticks < d.trackRecord;
        if (pb) d.trackBest.put(p.getUUID(), ticks);
        if (wr) { d.trackRecord = ticks; d.trackRecordName = p.getGameProfile().getName(); }
        d.setDirty();
        com.pvpciv.game.Story.title(p, "ФИНИШ " + fmt(ticks), pb ? "Новый личный рекорд!" : "Рекорд: " + fmt(best), 0x9ADFFF);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.4f);
        if (wr) {
            Component msg = Component.literal("❄ Рекорд Ледяной Трассы: " + p.getGameProfile().getName() + " — " + fmt(ticks)).withStyle(ChatFormatting.AQUA);
            for (ServerPlayer o : p.server.getPlayerList().getPlayers()) o.sendSystemMessage(msg);
        }
    }

    public static String fmt(long ticks) {
        long ms = ticks * 50;
        return String.format("%02d:%02d.%02d", ms / 60000, (ms / 1000) % 60, (ms / 10) % 100);
    }
}
