package com.pvpciv.game;

import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.2: «небо закручивается» на эпичных атаках (червь, демоница): игрокам рядом шлём своё время суток —
 * за ticks тиков небо делает полный оборот (с разгоном и торможением) и встаёт туда же, где было.
 */
public final class SkySpin {
    private SkySpin() {}

    private static final class Spin { final ServerLevel l; final Vec3 c; final double r; final int total; int t; Spin(ServerLevel l, Vec3 c, double r, int total) { this.l = l; this.c = c; this.r = r; this.total = total; } }
    private static final List<Spin> SPINS = new ArrayList<>();

    public static void start(ServerLevel l, Vec3 c, double r, int ticks) {
        for (Spin s : SPINS) if (s.l == l && s.c.distanceTo(c) < r) return;   // уже крутится
        SPINS.add(new Spin(l, c, r, ticks));
        for (ServerPlayer p : l.players()) if (p.position().distanceTo(c) < r) {
            p.playNotifySound(SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5f, 0.5f);
            p.playNotifySound(SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 0.6f, 0.6f);
        }
    }

    public static void tick(MinecraftServer server) {
        for (int i = SPINS.size() - 1; i >= 0; i--) {
            Spin s = SPINS.get(i);
            s.t++;
            double k = Math.min(1.0, s.t / (double) s.total);
            double e = k < 0.5 ? 4 * k * k * k : 1 - Math.pow(-2 * k + 2, 3) / 2;
            long off = s.t >= s.total ? 24000 : Math.round(24000 * e);
            boolean cycle = s.l.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
            var pkt = new ClientboundSetTimePacket(s.l.getGameTime(), s.l.getDayTime() + off, cycle);
            for (ServerPlayer p : s.l.players()) if (p.position().distanceTo(s.c) < s.r) p.connection.send(pkt);
            if (s.t >= s.total) SPINS.remove(i);
        }
    }
}
