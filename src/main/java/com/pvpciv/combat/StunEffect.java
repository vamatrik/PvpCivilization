package com.pvpciv.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Оглушение: цель не двигается и не атакует (атаки отменяются в CombatEvents). */
public class StunEffect extends MobEffect {
    public StunEffect() { super(MobEffectCategory.HARMFUL, 0xF0E060); }

    @Override public boolean shouldApplyEffectTickThisTick(int dur, int amp) { return true; }

    @Override
    public boolean applyEffectTick(LivingEntity e, int amp) {
        e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
        e.hurtMarked = true;
        if (e instanceof Mob m) m.getNavigation().stop();
        if (e.level() instanceof ServerLevel lvl && e.tickCount % 4 == 0) {
            double a = e.tickCount * 0.5;
            lvl.sendParticles(ParticleTypes.CRIT, e.getX() + Math.cos(a) * 0.5, e.getY() + e.getBbHeight() + 0.3, e.getZ() + Math.sin(a) * 0.5, 1, 0, 0, 0, 0);
        }
        return true;
    }
}
