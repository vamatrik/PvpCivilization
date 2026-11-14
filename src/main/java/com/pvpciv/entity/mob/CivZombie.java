package com.pvpciv.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Зомби цивилизации (пепельный, обмороженный, Громила): не горит на солнце, не тонет, без подкреплений. */
public class CivZombie extends Zombie {
    public CivZombie(EntityType<? extends Zombie> type, Level level) { super(type, level); }
    @Override protected boolean isSunSensitive() { return false; }
    @Override protected boolean convertsInWater() { return false; }
    @Override public boolean canBreakDoors() { return false; }
    @Override public boolean canPickUpLoot() { return false; }

    // ---- свои голоса у людей этажа 2 (бета 12.11: «гопники как зомби — не очень»)
    private int voice() {
        var t = getType();
        if (t == com.pvpciv.registry.ModMobs.THUG.get()) return 1;
        if (t == com.pvpciv.registry.ModMobs.POLICE.get()) return 2;
        if (t == com.pvpciv.registry.ModMobs.GHOUL.get()) return 3;
        return 0;
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return switch (voice()) { case 1 -> net.minecraft.sounds.SoundEvents.VINDICATOR_AMBIENT; case 2 -> net.minecraft.sounds.SoundEvents.VILLAGER_NO;
            case 3 -> net.minecraft.sounds.SoundEvents.DROWNED_AMBIENT; default -> super.getAmbientSound(); };
    }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) {
        return switch (voice()) { case 1 -> net.minecraft.sounds.SoundEvents.VINDICATOR_HURT; case 2 -> net.minecraft.sounds.SoundEvents.VILLAGER_HURT;
            case 3 -> net.minecraft.sounds.SoundEvents.DROWNED_HURT; default -> super.getHurtSound(s); };
    }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return switch (voice()) { case 1 -> net.minecraft.sounds.SoundEvents.VINDICATOR_DEATH; case 2 -> net.minecraft.sounds.SoundEvents.VILLAGER_DEATH;
            case 3 -> net.minecraft.sounds.SoundEvents.DROWNED_DEATH; default -> super.getDeathSound(); };
    }
    @Override protected net.minecraft.sounds.SoundEvent getStepSound() {
        return voice() == 0 ? super.getStepSound() : net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value();
    }
    @Override public float getVoicePitch() { return voice() == 1 ? 0.85f + random.nextFloat() * 0.1f : super.getVoicePitch(); }
    /** Мобы цивилизации не дерутся между собой — цель только игрок. */
    @Override public void setTarget(net.minecraft.world.entity.LivingEntity t) {
        if (t != null && !(t instanceof net.minecraft.world.entity.player.Player)) return;
        super.setTarget(t);
    }

    // ---- 0.11: особенности мобов этажа 4
    private int lunge;
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity t) {
        boolean r = super.doHurtTarget(t);
        if (!r || !(t instanceof net.minecraft.world.entity.LivingEntity le)) return r;
        var ty = getType();
        if (ty == com.pvpciv.registry.ModMobs.THAWED.get()) le.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
        else if (ty == com.pvpciv.registry.ModMobs.BOTANIST.get()) le.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 50, 0));
        else if (ty == com.pvpciv.registry.ModMobs.ROBOT.get()) le.knockback(0.9, getX() - le.getX(), getZ() - le.getZ());
        return r;
    }
    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide || getType() != com.pvpciv.registry.ModMobs.CHIMERA.get()) return;
        var t = getTarget();
        if (lunge > 0) { lunge--; return; }
        if (t != null && onGround()) {
            double d = distanceTo(t);
            if (d > 4 && d < 10 && hasLineOfSight(t)) {   // химера прыгает на цель
                var v = t.position().subtract(position()).normalize().scale(1.1);
                setDeltaMovement(v.x, 0.45, v.z);
                lunge = 70 + random.nextInt(40);
                playSound(net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, 0.8f, 1.4f);
            }
        }
    }
}
