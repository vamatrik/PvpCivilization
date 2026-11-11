package com.pvpciv.entity.beast;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Зверь-моб на GeckoLib (крыса, дворняга): idle/walk + разовый attack. Цель — только игроки. */
public class CivBeast extends Monster implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), WALK = RawAnimation.begin().thenLoop("walk");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String model;

    public CivBeast(EntityType<? extends Monster> type, Level level, String model) {
        super(type, level);
        this.model = model;
    }

    public String model() { return model; }

    public static AttributeSupplier.Builder attributes(double speed) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 12).add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.35f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public void setTarget(LivingEntity t) { if (t == null || t instanceof Player) super.setTarget(t); }

    @Override
    public boolean doHurtTarget(Entity e) {
        triggerAnim("main", "attack");
        return super.doHurtTarget(e);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "main", 3, st -> st.setAndContinue(st.isMoving() ? WALK : IDLE))
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("attack")));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
