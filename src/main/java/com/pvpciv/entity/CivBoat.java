package com.pvpciv.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Лодка с 3D-моделью: тир 2 — Инеевая лодка, тир 3 — Неоновое такси. Физика — ванильной лодки. */
public class CivBoat extends Boat implements GeoEntity {
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(CivBoat.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), MOVE = RawAnimation.begin().thenLoop("walk");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CivBoat(EntityType<? extends Boat> type, Level level) { super(type, level); }

    public int tier() { return entityData.get(TIER); }
    public void setTier(int t) { entityData.set(TIER, t); }
    public String model() { return tier() >= 3 ? "neon_car" : "rime_boat"; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(TIER, 2); }
    @Override protected void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putInt("civTier", tier()); }
    @Override protected void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); setTier(Math.max(2, t.getInt("civTier"))); }
    @Override public Item getDropItem() { return Items.AIR; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "main", 4, st -> st.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.002 ? MOVE : IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
