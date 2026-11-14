package com.pvpciv.entity.boss;

import com.pvpciv.net.Network;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.*;

/**
 * Доктор Ноэль, создатель Лианы (финальный босс этажа 4).
 *  • Неуязвим, пока жива Лиана; лечит её зелёным лучом.
 *  • Плазменная пушка: фиолетовый лазер-прицел ~1 с, затем выстрел по линии (уходи вбок).
 *  • Мини-чёрные дыры: 5 с тянут игроков к центру, в центре — урон. Боссов и проглоченного не тянут.
 *  • Пока Лиана кого-то жуёт — не стреляет.
 *  • Лиана погибла — ярость: дыры чаще, двойной выстрел.
 */
public class NoelBoss extends CivBoss {
    private static final Vector3f PLASMA = new Vector3f(0.75f, 0.3f, 1f), VOID = new Vector3f(0.35f, 0.1f, 0.6f);
    private final List<Hole> holes = new ArrayList<>();
    private static final class Hole { final Vec3 c; int t; Hole(Vec3 c, int t) { this.c = c; this.t = t; } }
    private int healCd = 40, noPet;
    private boolean petDead;

    public NoelBoss(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override public String defId() { return "noel"; }
    @Override public String model() { return "noel"; }
    @Override protected String[] actions() { return new String[]{"attack", "aim", "shoot", "cast", "heal"}; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        goalSelector.addGoal(3, new Keep(this));
        goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 20f));
        targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Держит дистанцию 7–13 блоков, кружит. Если Лиана мертва и цель рядом — бьёт рукой. */
    private static final class Keep extends Goal {
        final NoelBoss n; int strafe;
        Keep(NoelBoss n) { this.n = n; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() { return n.getTarget() != null && n.castTicks <= 0; }
        @Override public void tick() {
            var t = n.getTarget();
            if (t == null) return;
            double d = n.distanceTo(t);
            if (n.home != null && n.position().distanceTo(n.home) > 13) { n.getNavigation().moveTo(n.home.x, n.home.y, n.home.z, 1.2); return; }   // не уходит с арены
            if (n.petDead && d < 3) { n.getNavigation().stop(); if (n.tickCount % 20 == 0) n.doHurtTarget(t); return; }
            if (d > 13) n.getNavigation().moveTo(t, 1.0);
            else if (d < 7) { Vec3 away = n.position().subtract(t.position()).normalize().scale(5); n.getNavigation().moveTo(n.getX() + away.x, n.getY(), n.getZ() + away.z, 1.15); }
            else { if (--strafe <= 0) strafe = 40 + n.random.nextInt(30); n.getMoveControl().strafe(0, strafe > 20 ? 0.5f : -0.5f); }
        }
    }

    public LianaBoss pet(ServerLevel lvl) {
        List<LianaBoss> l = lvl.getEntitiesOfClass(LianaBoss.class, new AABB(blockPosition()).inflate(96), LianaBoss::isAlive);
        return l.isEmpty() ? null : l.get(0);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!level().isClientSide && level() instanceof ServerLevel lvl && pet(lvl) != null && src.getEntity() instanceof Player) {
            if (src.getEntity() instanceof ServerPlayer p && tickCount % 4 == 0)
                com.pvpciv.combat.DamageNumbers.show(lvl, this, Component.literal("ЩИТ ЛИАНЫ").withStyle(x -> x.withColor(0x6AD040).withBold(true)));
            lvl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.2, getZ(), 6, 0.4, 0.6, 0.4, 0);
            return false;
        }
        return super.hurt(src, amount);
    }

    @Override
    protected void ability(ServerLevel lvl, ServerPlayer target) {
        LianaBoss pet = pet(lvl);
        if (pet != null && pet.chewing()) { cooldown = 10; return; }   // пока Лиана жуёт — не стреляет
        int r = random.nextInt(10);
        if (r < 6) plasma(lvl, target, petDead);
        else holes(lvl, petDead ? 3 : 2);
        cooldown = petDead ? 40 + random.nextInt(15) : 70 + random.nextInt(25);
    }

    private void plasma(ServerLevel lvl, ServerPlayer t, boolean twice) {
        faceTo(t.position());
        play("aim", 24);
        lvl.playSound(null, blockPosition(), com.pvpciv.registry.ModRegistry.FX_AIM.get(), SoundSource.HOSTILE, 1.4f, 0.6f);
        Vec3[] aim = {t.position().add(0, 1, 0)};
        for (int i = 0; i < 20; i += 2) {
            final int ii = i;
            later(i, () -> {
                if (!isAlive()) return;
                if (t.isAlive() && ii < 14) aim[0] = aim[0].add(t.position().add(0, 1, 0).subtract(aim[0]).scale(0.3));   // прицел догоняет до последних 0.3 с
                Vec3 from = muzzle(), dir = aim[0].subtract(from).normalize();
                line(lvl, from, from.add(dir.scale(30)), 0xC040FF, 0.04f, 140, true);
            });
        }
        later(22, () -> {
            if (!isAlive()) return;
            triggerAnim("main", "shoot");
            Vec3 from = muzzle(), dir = aim[0].subtract(from).normalize(), to = from.add(dir.scale(30));
            line(lvl, from, to, 0xE090FF, 0.35f, 300, false);
            lvl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.5f, 1.6f);
            lvl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.8f);
            for (ServerPlayer p : victims(lvl, 40)) {
                if (LianaBoss.grabbed(p)) continue;
                if (p.getBoundingBox().inflate(0.6).clip(from, to).isPresent()) {
                    p.invulnerableTime = 0;
                    p.hurt(damageSources().indirectMagic(this, this), dmg() * 2.2f);
                    knock(p, from, 0.8, 0.35);
                }
            }
            for (int i = 0; i < 30; i += 2) { Vec3 q = from.add(dir.scale(i)); lvl.sendParticles(ParticleTypes.WITCH, q.x, q.y, q.z, 3, 0.15, 0.15, 0.15, 0.02); }
        });
        if (twice) later(34, () -> { if (isAlive() && t.isAlive()) plasma(lvl, t, false); });
    }

    private Vec3 muzzle() { return getEyePosition().add(getLookAngle().scale(0.8)).add(0, -0.35, 0); }

    private void holes(ServerLevel lvl, int n) {
        play("cast", 26);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1f, 1.6f);
        List<ServerPlayer> vs = victims(lvl, 26);
        Vec3 c = home != null ? home : position();
        for (int i = 0; i < n; i++) {
            Vec3 at;
            if (!vs.isEmpty()) { ServerPlayer p = vs.get(random.nextInt(vs.size())); at = p.position().add((random.nextDouble() - 0.5) * 6, 0, (random.nextDouble() - 0.5) * 6); }
            else at = c.add((random.nextDouble() - 0.5) * 16, 0, (random.nextDouble() - 0.5) * 16);
            Vec3 g = safeSpot(lvl, at, c);
            Vec3 hc = new Vec3(g.x, g.y + 1.2, g.z);
            later(10 + i * 6, () -> {
                holes.add(new Hole(hc, 100));
                com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.ZONE, new Vec3(hc.x, g.y, hc.z), 4.5f, 100, com.pvpciv.entity.FxEntity.rgb(VOID), light(PLASMA));
            });
        }
    }

    @Override
    protected void passive(ServerLevel lvl) {
        LianaBoss pet = pet(lvl);
        if (pet == null) noPet++; else noPet = 0;
        if (noPet > 60 && !petDead) {
            petDead = true;
            enraged = true;
            play("cast", 30);
            lvl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2f, 1.5f);
            for (ServerPlayer p : victims(lvl, 48))
                p.sendSystemMessage(Component.literal("☠ Ноэль: «ЛИАНА?! Ты… ты убил её! Я распылю тебя на атомы!» — щит спал.").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        // лечит Лиану лучом
        if (pet != null && --healCd <= 0 && castTicks <= 0) {
            healCd = 50;
            if (pet.getHealth() < pet.getMaxHealth()) {
                triggerAnim("main", "heal");
                pet.heal(pet.getMaxHealth() * 0.015f);
                line(lvl, getEyePosition().add(0, -0.4, 0), pet.position().add(0, 4, 0), 0x6AF040, 0.08f, 900, false);
                lvl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pet.getX(), pet.getY() + 4, pet.getZ(), 12, 0.8, 0.8, 0.8, 0);
            }
        }
        // чёрные дыры
        for (Iterator<Hole> it = holes.iterator(); it.hasNext(); ) {
            Hole h = it.next();
            if (--h.t <= 0) { it.remove(); lvl.sendParticles(ParticleTypes.REVERSE_PORTAL, h.c.x, h.c.y, h.c.z, 60, 0.5, 0.5, 0.5, 0.6); continue; }
            lvl.sendParticles(ParticleTypes.PORTAL, h.c.x, h.c.y, h.c.z, 8, 1.4, 1.0, 1.4, 1.2);
            if (h.t % 4 == 0) lvl.sendParticles(ParticleTypes.SQUID_INK, h.c.x, h.c.y, h.c.z, 2, 0.15, 0.15, 0.15, 0);
            for (ServerPlayer p : victims(lvl, 48)) {
                if (LianaBoss.grabbed(p)) continue;   // проглоченного не тянет
                Vec3 to = h.c.subtract(p.position().add(0, 0.9, 0));
                double d = to.length();
                if (d > 7 || d < 0.1) continue;
                double pull = 0.06 * (1 - d / 7) + 0.025;
                p.setDeltaMovement(p.getDeltaMovement().add(to.normalize().scale(pull).multiply(1, 0.3, 1)));
                p.hurtMarked = true;
                if (d < 1.3 && h.t % 10 == 0) { p.invulnerableTime = 0; p.hurt(damageSources().magic(), dmg() * 0.6f); }
            }
        }
    }

    private void line(ServerLevel lvl, Vec3 a, Vec3 b, int color, float w, int life, boolean laser) {
        String js = String.format(Locale.ROOT, "{\"a\":[%.3f,%.3f,%.3f],\"b\":[%.3f,%.3f,%.3f],\"c\":%d,\"w\":%.3f,\"l\":%d,\"z\":%b}",
                a.x, a.y, a.z, b.x, b.y, b.z, color, w, life, laser);
        PacketDistributor.sendToPlayersTrackingEntity(this, new Network.PhoneData(16, js));
    }

    @Override protected void onEnrage(ServerLevel lvl) {}
}
