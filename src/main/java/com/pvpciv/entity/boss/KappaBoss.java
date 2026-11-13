package com.pvpciv.entity.boss;

import com.pvpciv.entity.FxEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Братья-каппы (этаж 3, пруд в деревне самураев) — четыре босса одного логова, урон считается суммарно.
 *  • Весна — парные кама: рывок сквозь игрока (линия) и быстрые удары.
 *  • Лето — сумо: прыжок-шлепок с ударной волной, толчок.
 *  • Осень — нагината: широкий конус, вихрь вокруг себя.
 *  • Зима — посох: ледяные круги под игроками (замедление), прикрывает братьев щитом.
 * Общая механика — блюдце: с полным блюдцем каппа бьёт сильнее (+40%). Крит или удар в спину расплёскивают воду —
 * 5 с каппа беззащитен (+50% урона по нему), потом ныряет в пруд и снова заряжается.
 * Каждый павший брат злит остальных (+15% урона).
 */
public class KappaBoss extends CivBoss {
    public static final String[] SEASONS = {"spring", "summer", "autumn", "winter"};
    public static final String[] NAMES = {"Весенний брат", "Летний брат", "Осенний брат", "Зимний брат"};
    private static final int[] COLORS = {0xFF8AC0, 0xF4F0E0, 0xE06A20, 0xA8E4FF};
    private static final EntityDataAccessor<Integer> SEASON = SynchedEntityData.defineId(KappaBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> WET = SynchedEntityData.defineId(KappaBoss.class, EntityDataSerializers.BOOLEAN);
    private int dryTicks, fury;
    public Vec3 pond;

    public KappaBoss(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(SEASON, 0).define(WET, true); }

    public int season() { return entityData.get(SEASON); }
    public void season(int s) { entityData.set(SEASON, s); }
    public boolean wet() { return entityData.get(WET); }

    @Override public String defId() { return "kappa"; }
    @Override public String model() { return "kappa_" + SEASONS[Math.floorMod(season(), 4)]; }
    @Override protected String[] actions() { return new String[]{"attack", "dash", "slam", "sweep", "cast", "bow"}; }
    private Vector3f col() { int c = COLORS[season()]; return new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f); }

    @Override protected float dmg() { return super.dmg() * (wet() ? 1.4f : 1f) * (1 + 0.15f * fury); }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!wet()) amount *= 1.5f;
        boolean r = super.hurt(src, amount);
        // расплескать блюдце: крит Юки (больший удар) или удар в спину
        if (r && wet() && src.getEntity() instanceof Player p && level() instanceof ServerLevel lvl) {
            Vec3 look = getLookAngle(), to = p.position().subtract(position()).normalize();
            boolean back = look.x * to.x + look.z * to.z < -0.3;
            if (back || random.nextFloat() < 0.06f) spill(lvl, p);
        }
        return r;
    }

    /** Крит Юки по капле — блюдце расплёскивается наверняка. */
    public void critHit(Player p) { if (wet() && level() instanceof ServerLevel lvl) spill(lvl, p); }

    private void spill(ServerLevel lvl, Player by) {
        entityData.set(WET, false);
        dryTicks = 100;
        triggerAnim("main", "bow");
        castTicks = 20;
        lvl.playSound(null, blockPosition(), com.pvpciv.registry.ModRegistry.FX_SPLASH.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
        lvl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 2.2, getZ(), 40, 0.4, 0.2, 0.4, 0.2);
        FxEntity.spawn(lvl, FxEntity.BURST, position(), 2.2f, 14, 0x5AAAFF, 0xE0F4FF);
        by.displayClientMessage(Component.literal("💧 Блюдце расплескалось! " + NAMES[season()] + " беззащитен 5 с").withStyle(ChatFormatting.AQUA), true);
    }

    @Override
    protected void passive(ServerLevel lvl) {
        if (!wet() && --dryTicks <= 0) {
            // нырок в пруд и обратно — снова с водой
            Vec3 p = pond != null ? pond : home;
            if (p != null) {
                FxEntity.spawn(lvl, FxEntity.PILLAR, position(), 0.6f, 16, 0x5AAAFF, 0xE0F4FF);
                Vec3 back = position();
                teleportTo(p.x, p.y, p.z);
                lvl.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 0.5, p.z, 60, 0.8, 0.3, 0.8, 0.3);
                later(30, () -> { Vec3 s = safeSpot(lvl, back, home != null ? home : back); teleportTo(s.x, s.y, s.z); FxEntity.spawn(lvl, FxEntity.PILLAR, s, 0.6f, 16, 0x5AAAFF, 0xE0F4FF); });
            }
            entityData.set(WET, true);
        }
        if (wet() && tickCount % 30 == 0) lvl.sendParticles(ParticleTypes.DRIPPING_WATER, getX(), getY() + getBbHeight() * renderScale() + 0.2, getZ(), 2, 0.2, 0, 0.2, 0);
    }

    /** Брат пал — остальные злятся. */
    public void brotherDown(ServerLevel lvl) {
        fury++;
        cooldown = Math.min(cooldown, 20);
        FxEntity.spawn(lvl, FxEntity.PILLAR, position(), 0.9f, 24, 0xFF3040, COLORS[season()]);
    }

    @Override
    protected void ability(ServerLevel lvl, ServerPlayer target) {
        switch (season()) {
            case 0 -> { if (random.nextBoolean()) dash(lvl, target); else slashes(lvl, target); }
            case 1 -> { if (random.nextInt(3) > 0) slam(lvl, target); else shove(lvl); }
            case 2 -> { if (random.nextBoolean()) cone(lvl, target); else whirl(lvl); }
            default -> { if (random.nextInt(3) > 0) frost(lvl); else shield(lvl); }
        }
        cooldown = (enraged ? 55 : 80) + random.nextInt(30) - fury * 8;
    }

    // ---- весна
    private void dash(ServerLevel lvl, ServerPlayer t) {
        Vec3 from = position(), dir = t.position().subtract(from).multiply(1, 0, 1).normalize();
        Vec3 to = safeSpot(lvl, from.add(dir.scale(Math.min(10, t.distanceTo(this) + 3))), home != null ? home : from);
        faceTo(to);
        play("dash", 26);
        for (double d = 1; d < from.distanceTo(to); d += 1.6)
            zone(lvl, from.add(dir.scale(d)), 1.3, 16, col()).onHit(v -> v.hurt(damageSources().mobAttack(this), dmg()));
        later(16, () -> { teleportTo(to.x, to.y, to.z); lvl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5f, 1.5f); });
    }

    private void slashes(ServerLevel lvl, ServerPlayer t) {
        play("attack", 30);
        for (int i = 0; i < 3; i++) {
            final int k = i;
            later(i * 8, () -> zone(lvl, position().add(getLookAngle().scale(1.5)), 2.4, 10, col()).onHit(v -> v.hurt(damageSources().mobAttack(this), dmg() * 0.7f))
                    .cone(Math.atan2(t.getZ() - getZ(), t.getX() - getX()), Math.toRadians(55 + k * 10)));
        }
    }

    // ---- лето
    private void slam(ServerLevel lvl, ServerPlayer t) {
        Vec3 at = safeSpot(lvl, t.position(), home != null ? home : position());
        play("slam", 30);
        zone(lvl, at, 3.2, 22, col()).onHit(v -> { v.hurt(damageSources().mobAttack(this), dmg() * 1.4f); knock(v, at, 0.8, 0.7); })
                .fx(c -> { teleportTo(c.x, c.y, c.z); wave(lvl, c, 9, 0.45, dmg() * 0.7f, col()); lvl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 0.7f); });
    }

    private void shove(ServerLevel lvl) {
        play("attack", 18);
        Vec3 c = position();
        zone(lvl, c, 4.0, 12, col()).onHit(v -> { v.hurt(damageSources().mobAttack(this), dmg()); knock(v, c, 1.8, 0.5); });
    }

    // ---- осень
    private void cone(ServerLevel lvl, ServerPlayer t) {
        faceTo(t.position());
        play("attack", 24);
        zone(lvl, position(), 7.5, 20, col()).cone(Math.atan2(t.getZ() - getZ(), t.getX() - getX()), Math.toRadians(35))
                .onHit(v -> v.hurt(damageSources().mobAttack(this), dmg() * 1.3f));
    }

    private void whirl(ServerLevel lvl) {
        play("sweep", 22);
        Vec3 c = position();
        zone(lvl, c, 5.0, 16, col()).onHit(v -> { v.hurt(damageSources().mobAttack(this), dmg() * 1.1f); knock(v, c, 1.0, 0.4); })
                .fx(at -> lvl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, at.x, at.y + 1, at.z, 40, 3, 0.6, 3, 0));
    }

    // ---- зима
    private void frost(ServerLevel lvl) {
        play("cast", 28);
        castYaw = getYRot();
        int n = 0;
        for (ServerPlayer p : victims(lvl, 22)) {
            if (n++ >= 4) break;
            zone(lvl, p.position().add(p.getDeltaMovement().scale(5)), 2.8, 26, col())
                    .onHit(v -> { v.hurt(damageSources().mobAttack(this), dmg() * 1.1f); v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)); })
                    .fx(c -> lvl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 0.6, c.z, 40, 1.4, 0.4, 1.4, 0.02));
        }
    }

    private void shield(ServerLevel lvl) {
        play("cast", 30);
        castYaw = getYRot();
        for (KappaBoss b : lvl.getEntitiesOfClass(KappaBoss.class, getBoundingBox().inflate(24), KappaBoss::isAlive)) {
            b.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
            FxEntity.spawn(lvl, FxEntity.PILLAR, b.position(), 0.9f, 30, 0xA8E4FF, 0xFFFFFF);
        }
        lvl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.8f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("season", season()); t.putInt("fury", fury);
        if (pond != null) { t.putDouble("px", pond.x); t.putDouble("py", pond.y); t.putDouble("pz", pond.z); }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        season(t.getInt("season")); fury = t.getInt("fury");
        if (t.contains("px")) pond = new Vec3(t.getDouble("px"), t.getDouble("py"), t.getDouble("pz"));
    }
}
