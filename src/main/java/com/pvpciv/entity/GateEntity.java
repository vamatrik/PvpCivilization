package com.pvpciv.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Врата телепортации (3D, GeckoLib). Одни и те же врата у каждого игрока свои: сломаны или работают —
 * клиент решает сам (ClientState.gatesFixed), поэтому анимация разная у разных игроков.
 */
public class GateEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> ID = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.STRING);
    private static final RawAnimation ACTIVE = RawAnimation.begin().thenLoop("active"), BROKEN = RawAnimation.begin().thenLoop("broken");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GateEntity(EntityType<?> type, Level level) { super(type, level); noPhysics = true; setNoGravity(true); }

    public String gateId() { return entityData.get(ID); }
    public void gateId(String id) { entityData.set(ID, id); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(ID, ""); }
    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (p instanceof ServerPlayer sp) com.pvpciv.game.Gates.use(sp, gateId());
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "main", 10, st -> st.setAndContinue(
                level().isClientSide && (gateId().equals("hub") || com.pvpciv.client.ClientState.gatesFixed.contains(gateId())) ? ACTIVE : BROKEN)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
