package com.pvpciv.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * Кабина Небесного лифта (3D, GeckoLib). Станционная — стоит у монорельса, ПКМ — кнопка внутри (табло этажей).
 * Поездочная — едет по монорельсу по точкам пути с пассажиром внутри, в конце вызывает onEnd.
 */
public class LiftCabin extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Boolean> RIDE = SynchedEntityData.defineId(LiftCabin.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), MOVE = RawAnimation.begin().thenLoop("walk");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private List<Vec3> path;
    private int t, dur;
    private boolean easeIn, easeOut;
    private Consumer<LiftCabin> onEnd;
    /** Пока едем — выйти нельзя (Shift не работает). */
    public boolean locked = true;

    public LiftCabin(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public boolean ride() { return entityData.get(RIDE); }

    /** Запустить поездку по ломаной (точки пути), за dur тиков. */
    public void start(List<Vec3> path, int dur, boolean easeIn, boolean easeOut, Consumer<LiftCabin> onEnd) {
        entityData.set(RIDE, true);
        this.path = path; this.dur = dur; this.easeIn = easeIn; this.easeOut = easeOut; this.onEnd = onEnd; t = 0;
        Vec3 p = path.get(0);
        setPos(p.x, p.y, p.z);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(RIDE, false); }
    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return !ride(); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity e, EntityDimensions d, float scale) { return new Vec3(0, 0.2, 0.35); }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (p instanceof ServerPlayer sp && !ride()) com.pvpciv.game.SkyLift.use(sp);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !ride() || path == null) return;
        t++;
        double f = Math.min(1, t / (double) dur);
        if (easeIn && easeOut) f = f * f * (3 - 2 * f);
        else if (easeIn) f = f * f;
        else if (easeOut) f = 1 - (1 - f) * (1 - f);
        double pos = f * (path.size() - 1);
        int i = Math.min(path.size() - 2, (int) pos);
        Vec3 p = path.get(i).lerp(path.get(i + 1), pos - i);
        setDeltaMovement(p.subtract(position()));
        setPos(p.x, p.y, p.z);
        if (t >= dur) {
            locked = false;
            Consumer<LiftCabin> end = onEnd;
            onEnd = null;
            path = null;
            if (end != null) end.accept(this);
            discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "main", 4, st -> st.setAndContinue(ride() ? MOVE : IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
