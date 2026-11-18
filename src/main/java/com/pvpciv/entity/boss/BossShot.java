package com.pvpciv.entity.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** 1.0.1 — снаряд босса без гравитации: на клиенте обычный снежок с картинкой предмета, на сервере — урон и след частиц. */
public class BossShot extends Snowball {
    private final float dmg;
    private int life = 80;

    public BossShot(Level l, LivingEntity owner, float dmg, ItemStack look) { super(l, owner); this.dmg = dmg; setItem(look); setNoGravity(true); }

    @Override protected double getDefaultGravity() { return 0; }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel l) {
            l.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.01);
            if (--life <= 0) discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (r.getEntity() instanceof ServerPlayer p && getOwner() instanceof LivingEntity o) { p.invulnerableTime = 0; p.hurt(damageSources().mobProjectile(this, o), dmg); }
    }

    @Override
    protected void onHit(HitResult r) {
        super.onHit(r);
        if (level() instanceof ServerLevel l) l.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.02);
    }
}
