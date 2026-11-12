package com.pvpciv.entity.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;

/** Автоматчик канализации: очередь из 3 «пуль» (быстрые стрелы без гравитации). */
public class CivGunner extends CivSkeleton {
    public CivGunner(EntityType<? extends net.minecraft.world.entity.monster.Skeleton> type, Level level) { super(type, level); }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(level() instanceof ServerLevel lvl)) return;
        for (int i = 0; i < 3; i++) {
            Arrow a = new Arrow(lvl, this, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
            double dx = target.getX() - getX(), dy = target.getY(0.5) - a.getY(), dz = target.getZ() - getZ();
            a.shoot(dx, dy, dz, 3.4f, 4f + i * 2f);
            a.setNoGravity(true);
            a.setBaseDamage(getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.45 / 3.4);   // пуля ≈ 0.45 удара, очередь ≈ 1.35
            a.pickup = AbstractArrow.Pickup.DISALLOWED;
            lvl.addFreshEntity(a);
        }
        lvl.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.4f, 1.6f);
        lvl.sendParticles(ParticleTypes.SMOKE, getX(), getEyeY(), getZ(), 6, 0.1, 0.1, 0.1, 0.02);
    }
}
