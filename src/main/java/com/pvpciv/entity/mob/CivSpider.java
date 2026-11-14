package com.pvpciv.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;

public class CivSpider extends Spider {
    public CivSpider(EntityType<? extends Spider> type, Level level) { super(type, level); }

    /** Агрессивны и днём: обычная ближняя атака и поиск игрока без проверки света. */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.LeapAtTargetGoal(this, 0.4f));
        goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 8f));
        goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.player.Player.class, true));
    }
    /** Мобы цивилизации не дерутся между собой — цель только игрок. */
    @Override public void setTarget(net.minecraft.world.entity.LivingEntity t) {
        if (t != null && !(t instanceof net.minecraft.world.entity.player.Player)) return;
        super.setTarget(t);
    }

    /** 0.11: споровый ползун — облачко спор и отравление при укусе. */
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity t) {
        boolean r = super.doHurtTarget(t);
        if (r && getType() == com.pvpciv.registry.ModMobs.SPORELING.get() && t instanceof net.minecraft.world.entity.LivingEntity le) {
            le.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 60, 0));
            if (level() instanceof net.minecraft.server.level.ServerLevel sl)
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR, t.getX(), t.getY() + 1, t.getZ(), 20, 0.6, 0.6, 0.6, 0.01);
        }
        return r;
    }
}
