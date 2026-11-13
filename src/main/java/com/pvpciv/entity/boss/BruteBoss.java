package com.pvpciv.entity.boss;

import com.pvpciv.combat.Mobs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Гнилой Громила (ур. 5). Умения:
 *  • Дробитель — красный круг R5 вокруг себя (1.1 с), удар подбрасывает; затем ударная волна до 14 блоков (перепрыгни).
 *  • Размах тесаком — красный конус 120° перед собой (0.65 с), сильно отбрасывает.
 *  • Гнилой топот — зелёные круги под каждым игроком (1.5 с): урон + отравление.
 *  • На 50% HP — рёв: зовёт 3 пепельных зомби, ускоряется, Дробитель выпускает две волны.
 */
public class BruteBoss extends CivBoss {
    public BruteBoss(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override public String defId() { return "brute"; }
    @Override public String model() { return "rotten_brute_boss"; }
    @Override protected String[] actions() { return new String[]{"attack", "slam", "swipe", "stomp", "roar"}; }

    @Override
    protected void ability(ServerLevel lvl, ServerPlayer target) {
        double d = distanceTo(target);
        int roll = random.nextInt(10);
        if (d < 5.5 && roll < 4) swipe(lvl, target);
        else if (d < 9 && roll < 8) slam(lvl);
        else stomp(lvl);
        cooldown = enraged ? 60 + random.nextInt(30) : 100 + random.nextInt(40);
    }

    private void slam(ServerLevel lvl) {
        play("slam", 34);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 0.7f);
        Vec3 c = position();
        zone(lvl, c, 5.0, 22, RED).onHit(p -> {
            p.hurt(damageSources().mobAttack(this), dmg() * 1.5f);
            knock(p, c, 0.9, 0.8);
        }).fx(at -> {
            lvl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6f, 0.6f);
            lvl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 2, 1.5, 0.2, 1.5, 0);
            lvl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.2, at.z, 40, 3, 0.1, 3, 0.02);
            wave(lvl, at, 14, 0.45, dmg() * 0.8f, RED);
            if (enraged) later(12, () -> wave(lvl, at, 14, 0.5, dmg() * 0.8f, RED));
        });
    }

    private void swipe(ServerLevel lvl, ServerPlayer target) {
        play("swipe", 22);
        faceTo(target.position());
        double yaw = Math.atan2(target.getZ() - getZ(), target.getX() - getX());
        Vec3 c = position();
        lvl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2f, 0.5f);
        zone(lvl, c, 6.0, 13, RED).cone(yaw, Math.toRadians(60)).onHit(p -> {
            p.hurt(damageSources().mobAttack(this), dmg() * 1.3f);
            knock(p, c, 1.5, 0.45);
        }).fx(at -> {
            lvl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1f, 0.5f);
            for (int i = -3; i <= 3; i++) {
                double a = yaw + i * Math.toRadians(18);
                lvl.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x + Math.cos(a) * 3, at.y + 1.2, at.z + Math.sin(a) * 3, 1, 0, 0, 0, 0);
            }
        });
    }

    private void stomp(ServerLevel lvl) {
        play("stomp", 26);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2f, 0.5f);
        int n = 0;
        for (ServerPlayer p : victims(lvl, 22)) {
            if (n++ >= 6) break;
            rot(lvl, p.position().add(p.getDeltaMovement().scale(6)));
        }
        int extra = enraged ? 4 : 2;
        for (int i = 0; i < extra; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = 3 + random.nextDouble() * 7;
            rot(lvl, position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
        }
    }

    private void rot(ServerLevel lvl, Vec3 at) {
        zone(lvl, at, 2.6, 30, GREEN).onHit(p -> {
            p.hurt(damageSources().mobAttack(this), dmg());
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
            knock(p, at, 0.2, 0.55);
        }).fx(c -> {
            lvl.playSound(null, c.x, c.y, c.z, SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.5f, 0.5f);
            lvl.sendParticles(ParticleTypes.ITEM_SLIME, c.x, c.y + 0.4, c.z, 30, 1.2, 0.5, 1.2, 0.15);
            lvl.sendParticles(ParticleTypes.SNEEZE, c.x, c.y + 0.6, c.z, 25, 1.0, 0.8, 1.0, 0.05);
            lvl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 0.2, c.z, 8, 0.8, 0.1, 0.8, 0.03);
        });
    }

    @Override
    protected void onEnrage(ServerLevel lvl) {
        play("roar", 44);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3f, 0.5f);
        lvl.playSound(null, blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2f, 0.6f);
        lvl.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 3, getZ(), 6, 1.2, 0.6, 1.2, 0);
        com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.PILLAR, new Vec3(getX(), getY(), getZ()), 1.6f, 30, 0xFF3A20, 0xFFC060);
        Vec3 c = home != null ? home : position();
        for (int i = 0; i < 3; i++) {
            double a = i * 2.1;
            Mobs.spawn(lvl, Mobs.Kind.ASH_ZOMBIE, 4, false, c.add(Math.cos(a) * 6, 0, Math.sin(a) * 6), "floor1_mob", "boss_minion");
        }
        var sp = getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.addOrReplacePermanentModifier(new AttributeModifier(com.pvpciv.PvpCiv.id("brute_rage"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        cooldown = 50;
        for (ServerPlayer p : victims(lvl, 40))
            p.sendSystemMessage(Component.literal("☠ Громила в ярости! Зовёт подмогу, бьёт чаще — прыгай через волны.").withStyle(ChatFormatting.RED));
    }

    @SuppressWarnings("unused") private static float f(double v) { return (float) Mth.clamp(v, 0, 1); }
}
