package com.pvpciv.entity.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Босс на GeckoLib. Умения — с предупреждением: на земле рисуется зона (контур + заполняющийся круг),
 * по истечении задержки по всем игрокам внутри наносится удар. Ударная волна — расширяющееся кольцо,
 * которое бьёт стоящих на земле (перепрыгни!). Пока босс кастует (castTicks > 0) — стоит и не бьёт рукой.
 * Спавн, полоса здоровья, вклад урона и лут — в {@link com.pvpciv.floor.BossManager}.
 */
public abstract class CivBoss extends Monster implements GeoEntity {
    public static final Vector3f RED = new Vector3f(1f, 0.15f, 0.1f), GREEN = new Vector3f(0.45f, 0.85f, 0.15f),
            CYAN = new Vector3f(0.45f, 0.85f, 1f), WHITE = new Vector3f(0.92f, 0.97f, 1f);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), WALK = RawAnimation.begin().thenLoop("walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected int castTicks, cooldown = 60;
    protected boolean enraged;
    protected float castYaw = Float.NaN;
    protected Vec3 home;
    private int lastPlayerHit;
    private final List<Zone> zones = new ArrayList<>();
    private final List<Wave> waves = new ArrayList<>();
    private final List<Later> later = new ArrayList<>();

    protected CivBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes(double hp, double dmg, double speed) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, hp).add(Attributes.ATTACK_DAMAGE, dmg)
                .add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 2);
    }

    /** id в BossManager: brute, keeper. */
    public abstract String defId();
    public abstract String model();
    public float renderScale() { return 1f; }
    /** Разовые анимации (запускаются сервером через triggerAnim). */
    protected abstract String[] actions();
    /** Выбрать и начать умение (цель — игрок в радиусе). */
    protected abstract void ability(ServerLevel lvl, ServerPlayer target);
    protected void onEnrage(ServerLevel lvl) {}
    protected void passive(ServerLevel lvl) {}

    public void setHome(Vec3 h) { home = h; }
    public boolean enraged() { return enraged; }
    protected float dmg() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override public boolean canUse() { return castTicks <= 0 && super.canUse(); }
            @Override public boolean canContinueToUse() { return castTicks <= 0 && super.canContinueToUse(); }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public void setTarget(LivingEntity t) { if (t == null || t instanceof Player) super.setTarget(t); }

    @Override
    public boolean doHurtTarget(Entity e) {
        if (castTicks > 0) return false;
        triggerAnim("main", "attack");
        return super.doHurtTarget(e);
    }

    // ------------------------------------------------------------------ ход сервера
    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel lvl)) return;
        for (int i = later.size() - 1; i >= 0; i--) if (--later.get(i).t <= 0) later.remove(i).r.run();
        for (int i = zones.size() - 1; i >= 0; i--) {
            Zone z = zones.get(i);
            z.t--;
            if (z.t <= 0) { zones.remove(i); impact(lvl, z); }
        }
        for (int i = waves.size() - 1; i >= 0; i--) if (tickWave(lvl, waves.get(i))) waves.remove(i);
        if (castTicks > 0) {
            castTicks--;
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (!Float.isNaN(castYaw)) { setYRot(castYaw); yBodyRot = castYaw; yHeadRot = castYaw; }
            if (castTicks == 0) castYaw = Float.NaN;
        }
        if (!enraged && getHealth() < getMaxHealth() * 0.5f) { enraged = true; onEnrage(lvl); }
        passive(lvl);
        // бета: никто не бьёт минуту — босс восстанавливается (2% в секунду)
        if (tickCount - lastPlayerHit > 1200 && getHealth() < getMaxHealth() && tickCount % 20 == 0) {
            heal(getMaxHealth() * 0.02f);
            if (tickCount % 100 == 0) lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 3, 0.5, 0.3, 0.5, 0);
        }
        if (--cooldown <= 0 && castTicks <= 0 && getTarget() instanceof ServerPlayer p && p.isAlive() && distanceTo(p) < 20) ability(lvl, p);
    }

    /** Смотреть на точку весь каст. */
    protected void faceTo(Vec3 at) {
        castYaw = (float) (Mth.atan2(at.z - getZ(), at.x - getX()) * Mth.RAD_TO_DEG) - 90f;
    }

    protected void play(String anim, int cast) {
        castTicks = cast;
        triggerAnim("main", anim);
    }

    protected void later(int ticks, Runnable r) { later.add(new Later(ticks, r)); }

    /** Игроки, по которым можно попасть. */
    protected List<ServerPlayer> victims(ServerLevel lvl, double range) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : lvl.players())
            if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceTo(this) < range) out.add(p);
        return out;
    }

    /** Высота пола под точкой (до 6 блоков вниз). */
    protected double ground(ServerLevel lvl, Vec3 at) {
        BlockPos b = BlockPos.containing(at.x, at.y + 0.5, at.z);
        for (int i = 0; i < 8; i++, b = b.below())
            if (!lvl.getBlockState(b.below()).getCollisionShape(lvl, b.below()).isEmpty()) return b.getY();
        return at.y;
    }

    // ------------------------------------------------------------------ зоны
    public static final class Zone {
        Vec3 c; double r; int t; final int total; Vector3f color; Consumer<ServerPlayer> hit; Consumer<Vec3> fx; com.pvpciv.entity.FxEntity ent;
        double dirYaw = Double.NaN, half;   // конус: направление (рад) и полуугол
        Zone(Vec3 c, double r, int delay, Vector3f color) { this.c = c; this.r = r; this.t = delay; this.total = delay; this.color = color; }
        public Zone onHit(Consumer<ServerPlayer> h) { hit = h; return this; }
        public Zone fx(Consumer<Vec3> f) { fx = f; return this; }
        public Zone cone(double yawRad, double halfRad) { dirYaw = yawRad; half = halfRad; if (ent != null) ent.cone((float) yawRad, (float) halfRad); return this; }
        boolean inside(Vec3 p) {
            double dx = p.x - c.x, dz = p.z - c.z;
            if (dx * dx + dz * dz > r * r || Math.abs(p.y - c.y) > 3) return false;
            if (Double.isNaN(dirYaw)) return true;
            double a = Math.atan2(dz, dx), d = Math.abs(Mth.wrapDegrees(Math.toDegrees(a - dirYaw)));
            return Math.toRadians(d) <= half || dx * dx + dz * dz < 1.2;
        }
    }

    protected Zone zone(ServerLevel lvl, Vec3 at, double r, int delay, Vector3f color) {
        Zone z = new Zone(new Vec3(at.x, ground(lvl, at), at.z), r, delay, color);
        zones.add(z);
        // 0.9.0: цельный неоновый телеграф вместо частиц
        z.ent = com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.ZONE, z.c, (float) r, delay, com.pvpciv.entity.FxEntity.rgb(color), light(color));
        return z;
    }

    /** Светлый вариант цвета — для градиента. */
    public static int light(Vector3f c) { return com.pvpciv.entity.FxEntity.rgb(new Vector3f(c).lerp(new Vector3f(1, 1, 1), 0.55f)); }

    private void impact(ServerLevel lvl, Zone z) {
        com.pvpciv.entity.FxEntity f = com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.BURST, z.c, (float) z.r, 12, com.pvpciv.entity.FxEntity.rgb(z.color), light(z.color));
        if (f != null && !Double.isNaN(z.dirYaw)) f.cone((float) z.dirYaw, (float) z.half);
        if (z.fx != null) z.fx.accept(z.c);
        if (!isAlive()) return;
        for (ServerPlayer p : victims(lvl, 64)) if (z.inside(p.position()) && z.hit != null) { z.hit.accept(p); com.pvpciv.game.Marks.onZoneHit(p); }
    }

    // ------------------------------------------------------------------ ударные волны
    private static final class Wave {
        final Vec3 c; double r; final double max, speed; final float dmg; final ParticleOptions part; final Set<UUID> hit = new HashSet<>();
        Wave(Vec3 c, double max, double speed, float dmg, ParticleOptions part) { this.c = c; this.max = max; this.speed = speed; this.dmg = dmg; this.part = part; r = 1; }
    }

    /** Кольцо от центра наружу: бьёт игроков на земле, когда проходит под ними. */
    protected void wave(ServerLevel lvl, Vec3 c, double max, double speed, float dmg, Vector3f color) {
        Vec3 at = new Vec3(c.x, ground(lvl, c), c.z);
        waves.add(new Wave(at, max, speed, dmg, new DustParticleOptions(color, 2.0f)));
        com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.WAVE, at, (float) max, (int) Math.ceil((max - 1) / speed), com.pvpciv.entity.FxEntity.rgb(color), light(color));
    }

    private boolean tickWave(ServerLevel lvl, Wave w) {
        w.r += w.speed;
        if (isAlive()) for (ServerPlayer p : victims(lvl, 64)) {
            double d = Math.hypot(p.getX() - w.c.x, p.getZ() - w.c.z);
            if (Math.abs(d - w.r) < 0.8 && p.onGround() && Math.abs(p.getY() - w.c.y) < 1.5 && w.hit.add(p.getUUID())) {
                p.hurt(damageSources().mobAttack(this), w.dmg);
                com.pvpciv.game.Marks.onZoneHit(p);
                Vec3 push = p.position().subtract(w.c).multiply(1, 0, 1).normalize().scale(0.7);
                p.setDeltaMovement(push.x, 0.45, push.z);
                p.hurtMarked = true;
            }
        }
        return w.r >= w.max;
    }

    /** Куда можно прыгнуть/телепортироваться: твёрдый пол рядом с want, не ниже/выше центра логова больше чем на 3 блока; иначе — центр. */
    protected Vec3 safeSpot(ServerLevel lvl, Vec3 want, Vec3 center) {
        Vec3 s = com.pvpciv.game.Taxi.safe(lvl, BlockPos.containing(want.x, center.y, want.z));
        return Math.abs(s.y - center.y) <= 3 ? s : center;
    }

    private static final java.util.Map<UUID, Long> KNOCKED = new java.util.HashMap<>();

    /** Отбрасывание. 0.9.5: не чаще раза в 20 тиков на игрока — иначе цепочка шипов Королевы подбрасывала
     *  прыгнувшего игрока каждые 2 тика всё выше («на сотню блоков»). Вверх — не сильнее, если уже летит вверх. */
    protected static void knock(ServerPlayer p, Vec3 from, double h, double up) {
        long now = p.serverLevel().getGameTime();
        Long last = KNOCKED.get(p.getUUID());
        if (last != null && now - last < 20 && now >= last) return;
        KNOCKED.put(p.getUUID(), now);
        if (!p.onGround()) up = Math.min(up, Math.max(0.25, up - Math.max(0, p.getDeltaMovement().y)));
        Vec3 push = p.position().subtract(from).multiply(1, 0, 1);
        push = push.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : push.normalize().scale(h);
        p.setDeltaMovement(push.x, up, push.z);
        p.hurtMarked = true;
    }

    // ------------------------------------------------------------------ прочее
    @Override public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public void checkDespawn() {}
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean shouldDropExperience() { return false; }
    @Override public boolean canChangeDimensions(Level from, Level to) { return false; }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || src.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) return false;
        if (src.getEntity() instanceof net.minecraft.world.entity.Mob m && com.pvpciv.combat.Mobs.level(m) > 0) return false;   // мобы не ранят босса (питомцы игроков — могут)
        if (src.getEntity() instanceof Player) lastPlayerHit = tickCount;
        return super.hurt(src, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putBoolean("enraged", enraged);
        if (home != null) { t.putDouble("hx", home.x); t.putDouble("hy", home.y); t.putDouble("hz", home.z); }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        enraged = t.getBoolean("enraged");
        if (t.contains("hx")) home = new Vec3(t.getDouble("hx"), t.getDouble("hy"), t.getDouble("hz"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        AnimationController<CivBoss> ctl = new AnimationController<>(this, "main", 3, st ->
                st.setAndContinue(st.isMoving() ? WALK : IDLE));
        for (String a : actions()) ctl.triggerableAnim(a, RawAnimation.begin().thenPlay(a));
        c.add(ctl);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    private static final class Later { int t; final Runnable r; Later(int t, Runnable r) { this.t = t; this.r = r; } }

    @SuppressWarnings("unused") private static final PlayState PS = PlayState.CONTINUE;
}
