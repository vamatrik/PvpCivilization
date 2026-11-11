package com.pvpciv.entity.beast;

import com.pvpciv.combat.Mobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.UUID;

/** Крыса-союзница (навык «Зов крыс»): бьёт мобов рядом с хозяином и его цель, исчезает через 20 с. */
public class PetRat extends CivBeast {
    private UUID owner;
    private int life = 400;

    public PetRat(EntityType<? extends Monster> type, Level level) { super(type, level, "rat"); }

    public void setOwner(ServerPlayer p, int ticks, double dmg) {
        owner = p.getUUID(); life = ticks;
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(1, dmg));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
        setHealth(20);
        setCustomName(net.minecraft.network.chat.Component.literal("Крыса " + p.getGameProfile().getName()).withStyle(x -> x.withColor(0xB0E0A0)));
        setCustomNameVisible(false);
    }

    private ServerPlayer ownerPlayer() {
        return owner != null && level() instanceof ServerLevel l ? (ServerPlayer) l.getPlayerByUUID(owner) : null;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(3, new Goal() {   // держаться у хозяина
            { setFlags(EnumSet.of(Flag.MOVE)); }
            @Override public boolean canUse() { ServerPlayer o = ownerPlayer(); return o != null && getTarget() == null && distanceTo(o) > 4; }
            @Override public void tick() { ServerPlayer o = ownerPlayer(); if (o != null) getNavigation().moveTo(o, 1.3); }
        });
        targetSelector.addGoal(1, new TargetGoal(this, false) {
            @Override public boolean canUse() {
                ServerPlayer o = ownerPlayer();
                if (o == null) return false;
                LivingEntity t = o.getLastHurtMob();
                if (t == null || !t.isAlive() || t == PetRat.this) {
                    var list = level().getEntitiesOfClass(Mob.class, new AABB(o.blockPosition()).inflate(10), m -> m.isAlive() && (Mobs.level(m) > 0 || m instanceof com.pvpciv.entity.boss.CivBoss));
                    t = list.isEmpty() ? null : list.get(0);
                }
                if (t == null) return false;
                targetMob = t;
                return true;
            }
            @Override public void start() { mob.setTarget(targetMob); super.start(); }
        });
    }

    @Override public void setTarget(LivingEntity t) { if (t == null || !(t instanceof net.minecraft.world.entity.player.Player)) super.setTarget(t); }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected boolean shouldDropLoot() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (--life <= 0 || ownerPlayer() == null) {
            ((ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            discard();
        }
    }
}
