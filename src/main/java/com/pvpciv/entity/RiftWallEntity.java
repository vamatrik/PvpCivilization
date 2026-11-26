package com.pvpciv.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 1.5.3 — «блок» едущей стены разлома (как контрапции Create): столбик из len клеток одного вида, едет плавно.
 * Позиция — центр низа столбика. Сталкивается с игроком только на клиенте (там же игрока выталкивает и везёт —
 * {@link com.pvpciv.client.RiftFx}); на сервере столкновений нет, чтобы движение игрока не откатывалось.
 * kind: 0 — стена, 1 — хрупкий (ломается ударом), 2 — ступень.
 */
public class RiftWallEntity extends Entity {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(RiftWallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEN = SynchedEntityData.defineId(RiftWallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VX = SynchedEntityData.defineId(RiftWallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VZ = SynchedEntityData.defineId(RiftWallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> WALL = SynchedEntityData.defineId(RiftWallEntity.class, EntityDataSerializers.INT);

    public RiftWallEntity(EntityType<?> t, Level l) { super(t, l); noPhysics = true; setNoGravity(true); }

    public void setup(int kind, int len, double vx, double vz, int wallId) {
        entityData.set(KIND, kind); entityData.set(LEN, len); entityData.set(VX, (float) vx); entityData.set(VZ, (float) vz); entityData.set(WALL, wallId);
        setBoundingBox(makeBoundingBox());
    }

    public int kind() { return entityData.get(KIND); }
    public int len() { return entityData.get(LEN); }
    public double vx() { return entityData.get(VX); }
    public double vz() { return entityData.get(VZ); }
    public int wallId() { return entityData.get(WALL); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, 0); b.define(LEN, 1); b.define(VX, 0f); b.define(VZ, 0f); b.define(WALL, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (LEN.equals(key)) setBoundingBox(makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        int n = entityData == null ? 1 : Math.max(1, len());
        return new AABB(getX() - 0.5, getY(), getZ() - 0.5, getX() + 0.5, getY() + n, getZ() + 0.5);
    }

    @Override
    public void tick() {
        // равномерное движение — одинаково на сервере и клиенте (клиент не ждёт пакетов: плавно)
        setPos(getX() + vx(), getY(), getZ() + vz());
        xo = getX() - vx(); zo = getZ() - vz(); yo = getY();
        if (!level().isClientSide && tickCount > 20 * 60) discard();   // страховка
    }

    /** Сервер присылает позицию изредка: поправляем, только если клиент заметно разошёлся. */
    @Override
    public void lerpTo(double x, double y, double z, float yr, float xr, int steps) {
        if (Math.abs(x - getX()) + Math.abs(z - getZ()) + Math.abs(y - getY()) > 0.6) setPos(x, y, z);
    }

    @Override public boolean canBeCollidedWith() { return level().isClientSide; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isAttackable() { return true; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isIgnoringBlockTriggers() { return true; }

    /** Удар по стене: хрупкий блок ломается, остальное — глухо. */
    @Override
    public boolean skipAttackInteraction(Entity by) {
        if (level() instanceof ServerLevel l) {
            if (kind() == 1 && by instanceof net.minecraft.server.level.ServerPlayer) {
                l.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 14, 0.3, 0.3, 0.3, 0.08);
                l.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.5, getZ(), 10, 0.4, 0.4, 0.4, 0.05);
                l.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1f, 1.3f);
                discard();
            } else l.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS, 0.5f, 0.6f);
        }
        return true;
    }

    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
}
