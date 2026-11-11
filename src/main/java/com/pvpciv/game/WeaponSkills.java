package com.pvpciv.game;

import com.pvpciv.combat.Mobs;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Навыки оружия: ПКМ с оружием в основной руке. КД — ванильная перезарядка предмета (видна на иконке и на HUD).
 * Навык есть только у особого оружия (в первую очередь с боссов).
 */
public final class WeaponSkills {
    public enum Skill {
        QUAKE("Землетряс", "Удар тесаком о землю: урон ×1.5 и замедление всем врагам в радиусе 4", 15),
        FROST_LUNGE("Ледяной выпад", "Рывок на 6 блоков вперёд: урон и 2 стака холода всем на пути", 14),
        STUN_STRIKE("Оглушающий удар", "Следующий удар в течение 5 с оглушает цель на 1 с", 12),
        RAT_CALL("Зов крыс", "Призывает 2 крыс-союзниц на 20 с", 30);
        public final String title, desc; public final int cd;
        Skill(String t, String d, int cd) { title = t; desc = d; this.cd = cd; }
    }

    public static void cast(ServerPlayer p, ItemStack st, CivWeaponItem w) {
        Skill s = w.skill;
        if (s == null || p.getCooldowns().isOnCooldown(w)) return;
        ServerLevel lvl = p.serverLevel();
        double dmg = w.damage(st);
        switch (s) {
            case QUAKE -> {
                lvl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1f, 0.7f);
                lvl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()), p.getX(), p.getY() + 0.2, p.getZ(), 80, 2.2, 0.2, 2.2, 0.2);
                lvl.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.3, p.getZ(), 3, 1.2, 0.1, 1.2, 0);
                for (LivingEntity e : enemies(p, p.getBoundingBox().inflate(4, 1.5, 4))) {
                    e.hurt(p.damageSources().playerAttack(p), (float) (dmg * 1.5));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.4, 0));
                }
            }
            case FROST_LUNGE -> {
                Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 from = p.position();
                p.setDeltaMovement(dir.x * 1.9, 0.15, dir.z * 1.9);
                p.hurtMarked = true;
                lvl.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1f, 1.3f);
                for (int i = 0; i < 12; i++) {
                    Vec3 q = from.add(dir.scale(i * 0.5));
                    lvl.sendParticles(ParticleTypes.SNOWFLAKE, q.x, q.y + 1, q.z, 3, 0.2, 0.3, 0.2, 0.01);
                }
                AABB box = new AABB(from, from.add(dir.scale(6))).inflate(1.2, 1.5, 1.2);
                for (LivingEntity e : enemies(p, box)) {
                    e.hurt(p.damageSources().playerAttack(p), (float) (dmg * 1.2));
                    if (e instanceof ServerPlayer v) { com.pvpciv.combat.CombatEvents.frost(v, lvl); com.pvpciv.combat.CombatEvents.frost(v, lvl); }
                    else e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                }
            }
            case STUN_STRIKE -> {
                p.getPersistentData().putLong("stunNext", lvl.getGameTime() + 100);
                lvl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6f, 1.8f);
                lvl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
                p.displayClientMessage(Component.literal("⚡ Следующий удар оглушит").withStyle(ChatFormatting.YELLOW), true);
            }
            case RAT_CALL -> {
                lvl.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.2f, 1.4f);
                for (int i = 0; i < 2; i++) {
                    var rat = com.pvpciv.registry.ModMobs.PET_RAT.get().create(lvl);
                    if (rat == null) continue;
                    double a = i * Math.PI + p.getYRot();
                    rat.moveTo(p.getX() + Math.cos(a), p.getY(), p.getZ() + Math.sin(a), p.getYRot(), 0);
                    rat.setOwner(p, 400, dmg * 0.35);
                    lvl.addFreshEntity(rat);
                    lvl.sendParticles(ParticleTypes.POOF, rat.getX(), rat.getY() + 0.3, rat.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
                }
            }
        }
        p.getCooldowns().addCooldown(w, s.cd * 20);
    }

    /** Следующий удар после «Оглушающего удара». Вызывается из CombatEvents при ударе игрока. */
    public static void onHit(ServerPlayer p, LivingEntity target) {
        long until = p.getPersistentData().getLong("stunNext");
        if (until <= 0 || p.serverLevel().getGameTime() > until) return;
        p.getPersistentData().remove("stunNext");
        stun(target, 20);
    }

    public static void stun(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(ModRegistry.STUN, ticks, 0));
        t.level().playSound(null, t.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.6f);
        if (t instanceof ServerPlayer v) v.displayClientMessage(Component.literal("✦ ОГЛУШЁН").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), true);
    }

    private static java.util.List<LivingEntity> enemies(ServerPlayer p, AABB box) {
        return p.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive()
                && (e instanceof Mob m ? Mobs.level(m) > 0 || e instanceof com.pvpciv.entity.boss.CivBoss
                : e instanceof ServerPlayer o && !o.isCreative() && com.pvpciv.floor.Zones.at(o).type() == com.pvpciv.floor.Zones.Type.PVP
                  && com.pvpciv.floor.Zones.at(p).type() == com.pvpciv.floor.Zones.Type.PVP));
    }
}
