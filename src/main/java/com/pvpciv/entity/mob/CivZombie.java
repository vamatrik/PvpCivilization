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
}
