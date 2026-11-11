package com.pvpciv.game;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Скилл слота R: «Рывок». */
public final class DashSkill {
    public static final int COOLDOWN_TICKS = 140;   // 7 с
    private static final Map<UUID, Long> LAST = new HashMap<>();

    public static void tryDash(ServerPlayer p) {
        if (p.isPassenger() || p.isSpectator() || !p.isAlive()) return;
        long now = p.serverLevel().getGameTime();
        Long last = LAST.get(p.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS - 2) return;
        LAST.put(p.getUUID(), now);
        perform(p);
    }

    public static void perform(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = Vec3.directionFromRotation(0, p.getYRot());
        flat = flat.normalize();
        Vec3 v = flat.scale(1.35).add(0, p.onGround() ? 0.32 : 0.15, 0);
        p.setDeltaMovement(v);
        p.hurtMarked = true;
        p.connection.send(new ClientboundSetEntityMotionPacket(p));
        p.fallDistance = 0;
        ServerLevel sl = p.serverLevel();
        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.6f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.5f, 1.8f);
        sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 12, 0.3, 0.1, 0.3, 0.02);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
    }
}
