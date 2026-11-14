package com.pvpciv.entity.mob;

import com.pvpciv.net.Network;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;

/**
 * 0.10 — автоматчик с АК: красный лазер-подсказка ведёт игрока ~0.6 с, затем очередь (обычный — 12 патронов, элитный — 31),
 * скорострельность — пуля за 2 тика (10 в секунду). Прицел во время очереди догоняет игрока медленно — можно уйти вбок.
 */
public class CivGunner extends CivSkeleton {
    public CivGunner(EntityType<? extends net.minecraft.world.entity.monster.Skeleton> type, Level level) { super(type, level); }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.PILLAGER_AMBIENT; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) { return SoundEvents.PILLAGER_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.PILLAGER_DEATH; }
    @Override protected net.minecraft.sounds.SoundEvent getStepSound() { return SoundEvents.ARMOR_EQUIP_IRON.value(); }

    /** Вместо лука/меча скелета — своя цель «стрельба очередями» (добавляется один раз; вызывается ещё из конструктора скелета). */
    @Override
    public void reassessWeaponGoal() {
        if (level() == null || level().isClientSide) return;
        for (var w : goalSelector.getAvailableGoals()) if (w.getGoal() instanceof GunGoal) return;
        goalSelector.addGoal(4, new GunGoal(this));
    }

    @Override public void performRangedAttack(LivingEntity target, float power) {}

    /** 0.11: цвета лазера-подсказки и трассера у мобов этажа 4. */
    int laser() { var t = getType(); return t == com.pvpciv.registry.ModMobs.LAB_TECH.get() ? 0xC040FF : t == com.pvpciv.registry.ModMobs.SENTRY.get() ? 0x30C8FF : 0xFF2030; }
    int tracer() { var t = getType(); return t == com.pvpciv.registry.ModMobs.LAB_TECH.get() ? 0xE090FF : t == com.pvpciv.registry.ModMobs.SENTRY.get() ? 0x90E8FF : 0xFFD060; }
    @Override public boolean isPushable() { return getType() != com.pvpciv.registry.ModMobs.TURRET.get() && super.isPushable(); }

    static boolean exo(Player p) {
        for (var sl : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET})
            if (!(p.getItemBySlot(sl).getItem() instanceof com.pvpciv.item.CivArmorItem a) || !a.set.startsWith("Экзокостюм")) return false;
        return true;
    }

    public boolean elite() { return com.pvpciv.combat.Mobs.lootKind(this).endsWith("_elite"); }

    static final class GunGoal extends Goal {
        private final CivGunner g;
        private int state, timer, shots, strafe, fireTick;
        private Vec3 aim;
        GunGoal(CivGunner g) { this.g = g; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() { LivingEntity t = g.getTarget(); return t != null && t.isAlive(); }
        @Override public boolean canContinueToUse() { return canUse() || state == 2; }
        @Override public void start() { state = 0; timer = 20 + g.random.nextInt(20); g.setAggressive(true); }
        @Override public void stop() { state = 0; g.setAggressive(false); g.getNavigation().stop(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = g.getTarget();
            if (!(g.level() instanceof ServerLevel lvl)) return;
            Vec3 eye = g.getEyePosition();
            if (t != null) {
                double d = g.distanceTo(t);
                boolean see = g.getSensing().hasLineOfSight(t);
                g.getLookControl().setLookAt(t, 40, 40);
                if (state == 0) {   // перезарядка: держим дистанцию 9–15 блоков, обходим боком
                    if (!see || d > 16) g.getNavigation().moveTo(t, 1.0);
                    else if (d < 8) { Vec3 away = g.position().subtract(t.position()).normalize().scale(4); g.getNavigation().moveTo(g.getX() + away.x, g.getY(), g.getZ() + away.z, 1.1); }
                    else { if (--strafe <= 0) strafe = 30 + g.random.nextInt(30); g.getMoveControl().strafe(0, strafe > 15 ? 0.6f : -0.6f); }
                    if (--timer <= 0 && see && d < 24) { state = 1; timer = 12; aim = target(t).subtract(eye).normalize(); g.getNavigation().stop();
                        lvl.playSound(null, g.blockPosition(), ModRegistry.FX_AIM.get(), SoundSource.HOSTILE, 1f, 1f); }
                } else if (state == 1) {   // лазер ведёт игрока
                    g.getNavigation().stop();
                    aim = turn(aim, target(t).subtract(eye).normalize(), 0.35);
                    if (timer % 2 == 0) line(lvl, eye.add(aim.scale(0.7)).add(0, -0.15, 0), endOf(lvl, eye.add(0, -0.15, 0), aim), g.laser(), 0.03f, 140, true);
                    if (--timer <= 0) { state = 2; shots = g.elite() ? 31 : 12; }
                }
            }
            if (state == 2) {   // очередь
                g.getNavigation().stop();
                if (t != null) aim = turn(aim, target(t).subtract(eye).normalize(), g.elite() ? 0.05 : 0.035);
                if ((++fireTick & 1) == 0) return;   // бета 0.10: пуля раз в 2 тика (≈600 в минуту)
                fire(lvl, eye);
                if (--shots <= 0) { state = 0; timer = (g.elite() ? 50 : 40) + g.random.nextInt(25); }
            }
        }

        private Vec3 target(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.55, 0); }

        /** Поворот вектора к цели не больше чем на долю k (≈ ограничение скорости прицела). */
        private static Vec3 turn(Vec3 cur, Vec3 want, double k) { return cur.add(want.subtract(cur).scale(k)).normalize(); }

        private Vec3 endOf(ServerLevel lvl, Vec3 from, Vec3 dir) {
            Vec3 to = from.add(dir.scale(32));
            var hit = lvl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, g));
            return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        }

        private void fire(ServerLevel lvl, Vec3 eye) {
            float spread = 0.035f;
            Vec3 dir = aim.add((g.random.nextFloat() - 0.5f) * spread, (g.random.nextFloat() - 0.5f) * spread, (g.random.nextFloat() - 0.5f) * spread).normalize();
            Vec3 from = eye.add(dir.scale(0.7)).add(0, -0.15, 0);
            Vec3 end = endOf(lvl, from, dir);
            // попадание по игроку на отрезке
            Player hitP = null; double best = 1e9;
            for (Player p : lvl.getEntitiesOfClass(Player.class, new AABB(from, end).inflate(1))) {
                var o = p.getBoundingBox().inflate(0.15).clip(from, end);
                if (o.isPresent() && o.get().distanceToSqr(from) < best) { best = o.get().distanceToSqr(from); hitP = p; end = o.get(); }
            }
            if (hitP != null && !hitP.isCreative() && !hitP.isSpectator()) {
                hitP.invulnerableTime = 0;
                double dmg = g.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * (g.elite() ? 0.11 : 0.15);
                if (exo(hitP)) dmg *= 0.9;   // 0.11: экзокостюм держит пули
                com.pvpciv.combat.Balance.PEN = 0.5;   // 0.10.2: пули пробивают половину брони
                try { hitP.hurt(g.damageSources().mobAttack(g), (float) dmg); } finally { com.pvpciv.combat.Balance.PEN = 0; }
                hitP.invulnerableTime = 0;
            }
            line(lvl, from, end, g.tracer(), g.getType() == com.pvpciv.registry.ModMobs.LAB_TECH.get() ? 0.05f : 0.025f, 90, false);
            lvl.playSound(null, g.getX(), g.getEyeY(), g.getZ(), ModRegistry.FX_SHOT.get(), SoundSource.HOSTILE, 1.1f, 0.92f + g.random.nextFloat() * 0.16f);
            lvl.sendParticles(ParticleTypes.SMALL_FLAME, from.x, from.y, from.z, 1, 0.02, 0.02, 0.02, 0.01);
            if (g.random.nextInt(3) == 0) lvl.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 1, 0.05, 0.05, 0.05, 0.01);
            lvl.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 2, 0.05, 0.05, 0.05, 0.1);
        }

        private void line(ServerLevel lvl, Vec3 a, Vec3 b, int color, float w, int life, boolean laser) {
            String js = String.format(java.util.Locale.ROOT, "{\"a\":[%.3f,%.3f,%.3f],\"b\":[%.3f,%.3f,%.3f],\"c\":%d,\"w\":%.3f,\"l\":%d,\"z\":%b}",
                    a.x, a.y, a.z, b.x, b.y, b.z, color, w, life, laser);
            PacketDistributor.sendToPlayersTrackingEntity(g, new Network.PhoneData(16, js));
        }
    }

    @SuppressWarnings("unused") private static final Class<?> U = ServerPlayer.class;
    @SuppressWarnings("unused") private static final float M = Mth.PI;
}
