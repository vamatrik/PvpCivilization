package com.pvpciv.entity.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 1.0 — Луна на арене испытаний. С каждой стадией сильнее (силы этажа: 5, 6, 7…) и знает больше приёмов:
 *  1+ лунный серп (конус), затмение (круги под игроком);  2+ лунная волна (кольцо — перепрыгни);
 *  3+ рывок-полумесяц по линии;  4+ притяжение луны (тянет к ней, затем вспышка).
 * Арена своя у каждого игрока — см. {@link com.pvpciv.game.LunaTrial}.
 */
public class LunaBoss extends CivBoss {
    private static final Vector3f MOON = new Vector3f(0.8f, 0.6f, 1f), PALE = new Vector3f(0.95f, 0.9f, 1f), DEEP = new Vector3f(0.45f, 0.2f, 0.8f);
    public int stage = 1;
    private int pull;
    private Vec3 pullC;

    public LunaBoss(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override public String defId() { return "luna_trial"; }
    @Override public String model() { return "luna"; }
    @Override public String texture() { return "luna_hd"; }
    @Override protected String[] actions() { return new String[]{"attack", "slash", "cast", "dash"}; }

    @Override
    protected void ability(ServerLevel lvl, ServerPlayer t) {
        int r = random.nextInt(stage >= 4 ? 5 : stage >= 3 ? 4 : stage >= 2 ? 3 : 2);
        switch (r) {
            case 0 -> crescent(lvl, t);
            case 1 -> eclipse(lvl, t);
            case 2 -> wave(lvl);
            case 3 -> dash(lvl, t);
            default -> pull(lvl);
        }
        cooldown = Math.max(26, 70 - stage * 8) + random.nextInt(20);
    }

    private void crescent(ServerLevel lvl, ServerPlayer t) {
        faceTo(t.position());
        play("slash", 20);
        double yaw = Math.atan2(t.getZ() - getZ(), t.getX() - getX());
        zone(lvl, position(), 7, 18, MOON).cone(yaw, Math.toRadians(60)).onHit(p -> { p.hurt(damageSources().mobAttack(this), dmg() * 1.6f); knock(p, position(), 0.7, 0.3); });
        lvl.playSound(null, blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.HOSTILE, 1f, 1.4f);
    }

    private void eclipse(ServerLevel lvl, ServerPlayer t) {
        play("cast", 24);
        castYaw = getYRot();
        int n = 2 + stage;
        for (int i = 0; i < n; i++) {
            Vec3 at = i == 0 ? t.position() : t.position().add((random.nextDouble() - 0.5) * 9, 0, (random.nextDouble() - 0.5) * 9);
            Vec3 c = home != null ? home : position();
            Vec3 g = safeSpot(lvl, at, c);
            later(i * 4, () -> zone(lvl, g, 2.6, 22, DEEP).onHit(p -> p.hurt(damageSources().indirectMagic(this, this), dmg() * 1.4f))
                    .fx(c2 -> lvl.sendParticles(ParticleTypes.END_ROD, c2.x, c2.y + 0.5, c2.z, 25, 1, 1.5, 1, 0.05)));
        }
        lvl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2f, 0.6f);
    }

    private void wave(ServerLevel lvl) {
        play("cast", 16);
        castYaw = getYRot();
        later(10, () -> { if (isAlive()) wave(lvl, position(), 15, 0.45 + stage * 0.05, dmg() * 1.2f, PALE); });
        if (stage >= 4) later(30, () -> { if (isAlive()) wave(lvl, position(), 15, 0.6, dmg() * 1.2f, MOON); });
    }

    private void dash(ServerLevel lvl, ServerPlayer t) {
        faceTo(t.position());
        play("dash", 26);
        Vec3 from = position(), dir = t.position().subtract(from).multiply(1, 0, 1).normalize();
        Vec3 c = home != null ? home : from;
        for (int i = 1; i <= 7; i++) {
            Vec3 at = from.add(dir.scale(i * 2.0));
            if (at.distanceTo(c) > 14) break;
            zone(lvl, at, 1.6, 20, MOON).onHit(p -> { p.hurt(damageSources().mobAttack(this), dmg() * 1.5f); knock(p, from, 1.0, 0.4); });
        }
        later(20, () -> {
            if (!isAlive()) return;
            Vec3 to = from.add(dir.scale(12));
            if (to.distanceTo(c) > 13) to = c.add(to.subtract(c).normalize().scale(12));
            Vec3 s = safeSpot(lvl, to, c);
            for (int i = 0; i < 12; i++) { Vec3 q = from.lerp(s, i / 12.0); lvl.sendParticles(ParticleTypes.END_ROD, q.x, q.y + 1, q.z, 4, 0.2, 0.4, 0.2, 0.02); }
            teleportTo(s.x, s.y, s.z);
            lvl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 1.6f);
        });
    }

    private void pull(ServerLevel lvl) {
        play("cast", 50);
        castYaw = getYRot();
        pull = 40;
        pullC = position();
        lvl.playSound(null, blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1f, 1.8f);
        later(40, () -> zone(lvl, position(), 4.5, 12, DEEP).onHit(p -> { p.hurt(damageSources().indirectMagic(this, this), dmg() * 2.2f); knock(p, position(), 1.2, 0.5); }));
    }

    @Override
    protected void passive(ServerLevel lvl) {
        if (pull > 0 && pullC != null) {
            pull--;
            for (ServerPlayer p : victims(lvl, 24)) {
                Vec3 to = pullC.subtract(p.position());
                if (to.length() < 1) continue;
                p.setDeltaMovement(p.getDeltaMovement().add(to.normalize().scale(0.07).multiply(1, 0, 1)));
                p.hurtMarked = true;
            }
            if (pull % 3 == 0) lvl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 20, 3, 1, 3, 0.4);
        }
        if (tickCount % 6 == 0) lvl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.2, getZ(), 1, 0.3, 0.1, 0.3, 0.01);
    }

    @Override
    protected void onEnrage(ServerLevel lvl) {
        cooldown = 10;
        for (ServerPlayer p : victims(lvl, 30))
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("☾ Луна: «Неплохо. А теперь — по-настоящему.»").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
    }

    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putInt("stage", stage); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); stage = Math.max(1, t.getInt("stage")); }
}
