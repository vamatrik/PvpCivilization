package com.pvpciv.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Сюжетный NPC с HD-моделью GeckoLib (geo/&lt;model&gt;.geo.json, textures/entity/&lt;texture&gt;.png, общие анимации civ_npc).
 * Анимация задаётся сервером строкой (setAnim("cross")), пустая — авто: walk при движении, иначе idleAnim().
 * Анимации: idle walk run sneak stun down cast kneel crawl climb slash pierce cross drag bite sit.
 */
public abstract class GeoNpc extends StoryNpc implements GeoEntity {
    private static final EntityDataAccessor<String> ANIM = SynchedEntityData.defineId(GeoNpc.class, EntityDataSerializers.STRING);
    private static final Map<String, RawAnimation> RAW = new HashMap<>();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected GeoNpc(EntityType<? extends PathfinderMob> type, Level level, String name, int color) {
        super(type, level, name, color);
    }

    public abstract String model();
    public String texture() { return model(); }
    protected String idleAnim() { return "idle"; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ANIM, "");
    }

    public String anim() { return entityData.get(ANIM); }
    public void setAnim(String a) { if (!entityData.get(ANIM).equals(a)) entityData.set(ANIM, a); }

    private static RawAnimation raw(String n) {
        return RAW.computeIfAbsent(n, k -> k.equals("slash") || k.equals("pierce") ? RawAnimation.begin().thenPlay(k) : RawAnimation.begin().thenLoop(k));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "main", 5, state -> {
            String a = anim();
            if (a.isEmpty()) a = state.isMoving() ? "walk" : idleAnim();
            return state.setAndContinue(raw(a));
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
