package com.pvpciv.entity.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1.0 — демоница Пустоты, фаза 2 финала. Здоровья нет — есть мана (полоса сверху): её тратят испытания
 * (цветной пол, лазеры, паркур, волны неуязвимых). Удары игроков, если дотянутся, сжигают ману чуть быстрее.
 * Логика стадий — в {@link com.pvpciv.game.VoidRift}; сама она парит над центром и колдует.
 */
public class VoidDemon extends CivBoss {
    public VoidDemon(EntityType<? extends Monster> type, Level level) { super(type, level); setNoGravity(true); }

    @Override public String defId() { return "void_demon"; }
    @Override public String model() { return "void_demon"; }
    @Override protected String[] actions() { return new String[]{"attack", "cast", "point", "laugh"}; }
    @Override protected void registerGoals() { goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 30f)); }
    @Override protected void ability(ServerLevel lvl, ServerPlayer target) {}
    @Override public boolean isPushable() { return false; }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (level().isClientSide) return false;
        if (src.getEntity() instanceof ServerPlayer p) com.pvpciv.game.VoidRift.onDemonHit(p, amount);
        return false;
    }

    @Override
    protected void passive(ServerLevel lvl) {
        cooldown = 9999;
        setDeltaMovement(0, 0, 0);
        if (tickCount % 4 == 0) lvl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 4, 0.6, 1, 0.6, 0.05);
        if (tickCount % 10 == 0) lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 2.4, getZ(), 2, 0.3, 0.1, 0.3, 0.01);
    }

    public void anim(String a) { triggerAnim("main", a); }
}
