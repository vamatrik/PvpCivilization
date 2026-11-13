package com.pvpciv.entity;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Цельный неоновый эффект (0.9.0: вместо россыпи частиц у боссов, элиток и алтаря).
 * Геометрия строится на клиенте (FxRenderer) — гладкие градиентные кольца, диски, столбы и лучи.
 *  ZONE  — телеграф: контур + заполняющийся диск (r, life = время до удара; half > 0 — конус по yaw)
 *  WAVE  — ударная волна: расширяющаяся светящаяся стенка (r = максимум, растёт за life)
 *  BURST — вспышка удара: кольцо-взрыв + столб
 *  PILLAR — вертикальный луч (появление, призыв)
 *  BEAM  — луч к точке (dx, dy, dz) — для алтаря
 */
public class FxEntity extends Entity {
    public static final int ZONE = 0, WAVE = 1, BURST = 2, PILLAR = 3, BEAM = 4;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR2 = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HALF = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<org.joml.Vector3f> VEC = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.VECTOR3);

    public FxEntity(EntityType<? extends FxEntity> t, Level l) { super(t, l); noPhysics = true; setNoGravity(true); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, 0).define(COLOR, 0xFF3040).define(COLOR2, 0xFFE080).define(RADIUS, 2f).define(LIFE, 20).define(HALF, 0f).define(VEC, new org.joml.Vector3f());
    }

    public int kind() { return entityData.get(KIND); }
    public int color() { return entityData.get(COLOR); }
    public int color2() { return entityData.get(COLOR2); }
    public float radius() { return entityData.get(RADIUS); }
    public int life() { return entityData.get(LIFE); }
    public float half() { return entityData.get(HALF); }
    public org.joml.Vector3f vec() { return entityData.get(VEC); }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > life() + 2) discard();
    }

    @Override protected void readAdditionalSaveData(CompoundTag t) { discard(); }   // эффекты не переживают перезапуск
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() { var v = vec(); return getBoundingBox().inflate(Math.max(radius() + 1.5, v.length() + 4)).expandTowards(0, 9, 0); }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }

    // ------------------------------------------------------------------ создание
    public static FxEntity spawn(ServerLevel l, int kind, Vec3 at, float r, int life, int color, int color2) {
        FxEntity e = ModRegistry.FX.get().create(l);
        if (e == null) return null;
        e.moveTo(at.x, at.y, at.z, 0, 0);
        e.entityData.set(KIND, kind);
        e.entityData.set(RADIUS, r);
        e.entityData.set(LIFE, Math.max(1, life));
        e.entityData.set(COLOR, color);
        e.entityData.set(COLOR2, color2);
        l.addFreshEntity(e);
        var snd = kind == ZONE ? ModRegistry.FX_ZONE : kind == WAVE ? ModRegistry.FX_WAVE : kind == BURST ? ModRegistry.FX_IMPACT : null;
        if (snd != null) l.playSound(null, at.x, at.y, at.z, snd.get(), net.minecraft.sounds.SoundSource.HOSTILE, kind == ZONE ? 0.7f : 1.1f, 0.9f + l.random.nextFloat() * 0.2f);
        return e;
    }

    public FxEntity cone(float yawRad, float halfRad) { entityData.set(HALF, halfRad); setYRot((float) Math.toDegrees(yawRad)); entityData.set(VEC, new org.joml.Vector3f((float) Math.cos(yawRad), 0, (float) Math.sin(yawRad))); return this; }
    public FxEntity to(Vec3 target) { entityData.set(VEC, new org.joml.Vector3f((float) (target.x - getX()), (float) (target.y - getY()), (float) (target.z - getZ()))); return this; }

    public static int rgb(org.joml.Vector3f c) { return ((int) (c.x * 255) << 16) | ((int) (c.y * 255) << 8) | (int) (c.z * 255); }
}
