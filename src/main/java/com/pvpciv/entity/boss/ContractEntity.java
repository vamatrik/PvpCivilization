package com.pvpciv.entity.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 1.0 — «Подпиши здесь!»: летящий контракт Дипломата. Медленно летит к цели; 3 удара — сбит; долетел — подписан. */
public class ContractEntity extends Mob implements ItemSupplier {
    private UUID owner, target;
    private int hits, life = 220;

    public ContractEntity(EntityType<? extends Mob> t, Level l) { super(t, l); setNoGravity(true); setPersistenceRequired(); }

    public static AttributeSupplier.Builder attributes() { return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1000).add(Attributes.MOVEMENT_SPEED, 0.1); }

    public void setup(DiplomatBoss d, ServerPlayer t) { owner = d.getUUID(); target = t.getUUID(); }

    @Override public ItemStack getItem() { return new ItemStack(com.pvpciv.registry.ModF5.CONTRACT.get()); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean causeFallDamage(float a, float b, DamageSource s) { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (level().isClientSide || !(src.getEntity() instanceof Player)) return false;
        hits++;
        playSound(SoundEvents.BOOK_PAGE_TURN, 1.5f, 1.4f);
        if (level() instanceof ServerLevel l) l.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.2);
        if (hits >= 3) {
            if (level() instanceof ServerLevel l) l.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0.05);
            playSound(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1f, 1.8f);
            discard();
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel l)) return;
        if (--life <= 0 || target == null) { discard(); return; }
        ServerPlayer t = l.getServer().getPlayerList().getPlayer(target);
        if (t == null || !t.isAlive() || t.level() != l) { discard(); return; }
        Vec3 to = t.getEyePosition().add(0, -0.3, 0).subtract(position());
        double d = to.length();
        if (d < 1.1) {
            if (l.getEntity(owner) instanceof DiplomatBoss b && b.isAlive()) b.signed(t);
            discard();
            return;
        }
        Vec3 v = to.normalize().scale(0.16 + Math.min(0.06, tickCount * 0.0004));
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        setPos(position().add(v));
        setYRot((float) (Math.atan2(to.z, to.x) * 180 / Math.PI) - 90f);
        if (tickCount % 3 == 0) l.sendParticles(ParticleTypes.ENCHANT, getX(), getY(), getZ(), 3, 0.2, 0.2, 0.2, 0.2);
    }

    @Override protected void doPush(net.minecraft.world.entity.Entity e) {}
    @Override public boolean isNoGravity() { return true; }
}
