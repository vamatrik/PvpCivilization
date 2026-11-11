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
            if (z.t % 2 == 0 && z.t > 0) drawZone(lvl, z);
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
        Vec3 c; double r; int t; final int total; Vector3f color; Consumer<ServerPlayer> hit; Consumer<Vec3> fx;
        double dirYaw = Double.NaN, half;   // конус: направление (рад) и полуугол
        Zone(Vec3 c, double r, int delay, Vector3f color) { this.c = c; this.r = r; this.t = delay; this.total = delay; this.color = color; }
        public Zone onHit(Consumer<ServerPlayer> h) { hit = h; return this; }
        public Zone fx(Consumer<Vec3> f) { fx = f; return this; }
        public Zone cone(double yawRad, double halfRad) { dirYaw = yawRad; half = halfRad; return this; }
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
        drawZone(lvl, z);
        return z;
    }

    private void drawZone(ServerLevel lvl, Zone z) {
        double prog = 1 - z.t / (double) z.total;
        DustParticleOptions edge = new DustParticleOptions(z.color, 1.6f), fill = new DustParticleOptions(z.color, 1.0f);
        int n = Math.max(12, (int) (z.r * 8));
        boolean cone = !Double.isNaN(z.dirYaw);
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            if (cone && Math.abs(Mth.wrapDegrees(Math.toDegrees(a - z.dirYaw))) > Math.toDegrees(z.half)) continue;
            lvl.sendParticles(edge, z.c.x + Math.cos(a) * z.r, z.c.y + 0.12, z.c.z + Math.sin(a) * z.r, 1, 0, 0, 0, 0);
        }
        if (cone) for (double s : new double[]{-z.half, z.half})
            for (double d = 0.8; d < z.r; d += 0.7)
                lvl.sendParticles(edge, z.c.x + Math.cos(z.dirYaw + s) * d, z.c.y + 0.12, z.c.z + Math.sin(z.dirYaw + s) * d, 1, 0, 0, 0, 0);
        double ri = z.r * prog;   // заполняющийся круг: когда дойдёт до контура — удар
        int m = Math.max(6, (int) (ri * 7));
        for (int i = 0; i < m; i++) {
            double a = 2 * Math.PI * i / m;
            if (cone && Math.abs(Mth.wrapDegrees(Math.toDegrees(a - z.dirYaw))) > Math.toDegrees(z.half)) continue;
            lvl.sendParticles(fill, z.c.x + Math.cos(a) * ri, z.c.y + 0.1, z.c.z + Math.sin(a) * ri, 1, 0, 0, 0, 0);
        }
    }

    private void impact(ServerLevel lvl, Zone z) {
        if (z.fx != null) z.fx.accept(z.c);
        if (!isAlive()) return;
        for (ServerPlayer p : victims(lvl, 64)) if (z.inside(p.position()) && z.hit != null) z.hit.accept(p);
    }

    // ------------------------------------------------------------------ ударные волны
    private static final class Wave {
        final Vec3 c; double r; final double max, speed; final float dmg; final ParticleOptions part; final Set<UUID> hit = new HashSet<>();
        Wave(Vec3 c, double max, double speed, float dmg, ParticleOptions part) { this.c = c; this.max = max; this.speed = speed; this.dmg = dmg; this.part = part; r = 1; }
    }

    /** Кольцо от центра наружу: бьёт игроков на земле, когда проходит под ними. */
    protected void wave(ServerLevel lvl, Vec3 c, double max, double speed, float dmg, Vector3f color) {
        waves.add(new Wave(new Vec3(c.x, ground(lvl, c), c.z), max, speed, dmg, new DustParticleOptions(color, 2.0f)));
    }

    private boolean tickWave(ServerLevel lvl, Wave w) {
        w.r += w.speed;
        int n = Math.max(16, (int) (w.r * 7));
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n + w.r;
            lvl.sendParticles(w.part, w.c.x + Math.cos(a) * w.r, w.c.y + 0.25, w.c.z + Math.sin(a) * w.r, 1, 0, 0.1, 0, 0);
            if (i % 4 == 0) lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, w.c.x + Math.cos(a) * w.r, w.c.y + 0.2, w.c.z + Math.sin(a) * w.r, 1, 0, 0.05, 0, 0.01);
        }
        if (isAlive()) for (ServerPlayer p : victims(lvl, 64)) {
            double d = Math.hypot(p.getX() - w.c.x, p.getZ() - w.c.z);
            if (Math.abs(d - w.r) < 0.8 && p.onGround() && Math.abs(p.getY() - w.c.y) < 1.5 && w.hit.add(p.getUUID())) {
                p.hurt(damageSources().mobAttack(this), w.dmg);
                Vec3 push = p.position().subtract(w.c).multiply(1, 0, 1).normalize().scale(0.7);
                p.setDeltaMovement(push.x, 0.45, push.z);
                p.hurtMarked = true;
            }
        }
        return w.r >= w.max;
    }

    protected static void knock(ServerPlayer p, Vec3 from, double h, double up) {
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
