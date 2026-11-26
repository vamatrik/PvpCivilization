package com.pvpciv.entity.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/** 1.0 — сегмент тела Галактического червя. Положение ставит голова; удар по сегменту ранит голову (×0.8). */
public class WormSegment extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public UUID head;
    public int index;
    private int orphan;

    public WormSegment(EntityType<? extends Mob> t, Level l) { super(t, l); setNoGravity(true); noPhysics = true; setPersistenceRequired(); }

    public static AttributeSupplier.Builder attributes() { return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1000).add(Attributes.KNOCKBACK_RESISTANCE, 1); }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (level().isClientSide || head == null || !(level() instanceof net.minecraft.server.level.ServerLevel l)) return false;
        if (src.getEntity() instanceof net.minecraft.world.entity.player.Player && l.getEntity(head) instanceof WormBoss w && w.isAlive()) {
            w.invulnerableTime = 0;
            return w.hurt(src, amount * 0.8f);
        }
        return false;
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        if (level().isClientSide) return;
        // 1.5.2: голова жива, но ведёт другое тело — это лишний сегмент (второй «червь»)
        if (head == null || !(level() instanceof net.minecraft.server.level.ServerLevel l) || !(l.getEntity(head) instanceof WormBoss w) || !w.isAlive() || (w.tickCount > 10 && !w.owns(getUUID()))) {
            if (++orphan > 40) discard();
        } else orphan = 0;
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity e) {}
    @Override public boolean causeFallDamage(float a, float b, DamageSource s) { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean canChangeDimensions(Level a, Level b) { return false; }
    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); if (head != null) t.putUUID("head", head); t.putInt("idx", index); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); if (t.hasUUID("head")) head = t.getUUID("head"); index = t.getInt("idx"); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
