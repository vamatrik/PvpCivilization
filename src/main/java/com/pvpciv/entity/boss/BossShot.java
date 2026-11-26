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
import net.minecraft.world.phys.Vec3;

/** 1.0.1 — снаряд босса без гравитации: на клиенте обычный снежок с картинкой предмета, на сервере — урон и след частиц. */
public class BossShot extends Snowball {
    private final float dmg;
    private int life = 80;
    /** 1.1.2: доп. эффект при попадании (замедление, яд и т.п.) и свой след частиц. */
    public java.util.function.Consumer<ServerPlayer> onHit;
    public net.minecraft.core.particles.ParticleOptions trail = ParticleTypes.REVERSE_PORTAL;
    /** 1.2: самонаведение — цель и доля поворота за тик (0.04 — едва заметно, 0.15 — цепко). */
    public ServerPlayer homing; public double turn;

    /**
     * 1.2: до 1.2 купол запертой арены гасил ВСЕ снаряды боссов в момент выстрела (баг с xo/zo) — их урон ни разу не проверялся в бою.
     * Теперь они долетают, поэтому общий множитель ×0.65.
     */
    public static final float SCALE = 0.65f;
    public BossShot(Level l, LivingEntity owner, float dmg, ItemStack look) { super(l, owner); this.dmg = dmg * SCALE; setItem(look); setNoGravity(true); }

    @Override protected double getDefaultGravity() { return 0; }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel l) {
            if (trail != null) l.sendParticles(trail, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.01);
            if (homing != null && homing.isAlive() && !homing.isSpectator()) {
                Vec3 v = getDeltaMovement();
                double sp = v.length();
                Vec3 want = homing.position().add(0, homing.getBbHeight() * 0.6, 0).subtract(position());
                if (sp > 1e-3 && want.lengthSqr() > 1) setDeltaMovement(v.normalize().lerp(want.normalize(), turn).normalize().scale(sp));
            }
            if (--life <= 0) discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (r.getEntity() instanceof ServerPlayer p && getOwner() instanceof LivingEntity o) {
            p.invulnerableTime = 0;
            p.hurt(damageSources().mobProjectile(this, o), dmg);
            if (onHit != null) onHit.accept(p);
        }
    }

    /** Сквозь своих (босса, его сегменты и миньонов) — не задевать. */
    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity e) { return e instanceof net.minecraft.world.entity.player.Player && super.canHitEntity(e); }

    @Override
    protected void onHit(HitResult r) {
        super.onHit(r);
        if (level() instanceof ServerLevel l) l.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.02);
    }
}
