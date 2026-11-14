package com.pvpciv.client;

import com.pvpciv.PvpCiv;
import com.pvpciv.registry.ModMobs;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Рендер мобов цивилизации: ванильные модели, свои HD-скины (textures/entity/mob/*.png). */
public final class MobRenderers {
    private static ResourceLocation tex(String n) { return PvpCiv.id("textures/entity/mob/" + n + ".png"); }

    static void register(EntityRenderersEvent.RegisterRenderers e) {
        zombie(e, ModMobs.ASH_ZOMBIE.get(), "ash_zombie");
        zombie(e, ModMobs.WITHERED.get(), "withered");
        zombie(e, ModMobs.BRUTE.get(), "rotten_brute");
        zombie(e, ModMobs.FROST_ZOMBIE.get(), "frost_zombie");
        skeleton(e, ModMobs.ASH_ARCHER.get(), "ash_archer");
        skeleton(e, ModMobs.FROST_ARCHER.get(), "frost_archer");
        spider(e, ModMobs.ASH_SPIDER.get(), "ash_spider");
        spider(e, ModMobs.FROST_SPIDER.get(), "frost_spider");
        zombie(e, ModMobs.POLICE.get(), "police");
        zombie(e, ModMobs.THUG.get(), "thug");
        zombie(e, ModMobs.GHOUL.get(), "sewer_ghoul");
        // 0.9.9: мобы этажа 3 — гуманоидная модель 64×64 + объёмные детали (Mob3DLayer)
        zombie3d(e, ModMobs.SAMURAI.get(), "samurai", "samurai", Mob3DParts.samurai(), 1f);
        zombie3d(e, ModMobs.NINJA.get(), "ninja", "ninja", Mob3DParts.ninja(), 1f);
        zombie3d(e, ModMobs.WEREWOLF.get(), "werewolf", "werewolf", Mob3DParts.werewolf(), 1.1f);
        archer3d(e, ModMobs.ASHIGARU.get(), "ashigaru", "ashigaru", Mob3DParts.ashigaru());
        archer3d(e, ModMobs.KITSUNE_ARCHER.get(), "kitsune_archer", "kitsune", Mob3DParts.kitsune());
        e.registerEntityRenderer(ModMobs.FOX.get(), ctx -> new BeastRenderer<>(ctx, 0.4f));
        e.registerEntityRenderer(ModMobs.KAPPA_BOSS.get(), ctx -> new BossRenderer<>(ctx, "kappa", 1.25f, 0.6f));
        e.registerEntityRenderer(ModMobs.KUMIHO_BOSS.get(), ctx -> new BossRenderer<>(ctx, "kumiho", 1.2f, 0.6f));
        e.registerEntityRenderer(ModMobs.NOEL_BOSS.get(), ctx -> new BossRenderer<>(ctx, "noel", 1.1f, 0.6f));
        e.registerEntityRenderer(ModMobs.LIANA_BOSS.get(), ctx -> new BossRenderer<>(ctx, "liana", 2.0f, 1.8f));
        ResourceLocation gt = tex("gunner");
        e.registerEntityRenderer(ModMobs.GUNNER.get(), ctx -> new HumanoidMobRenderer<net.minecraft.world.entity.monster.Skeleton, net.minecraft.client.model.HumanoidModel<net.minecraft.world.entity.monster.Skeleton>>(
                ctx, new net.minecraft.client.model.HumanoidModel<>(ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE)) {
                    @Override public void prepareMobModel(net.minecraft.world.entity.monster.Skeleton s, float a, float b, float pt) {
                        rightArmPose = s.isAggressive() ? ArmPose.CROSSBOW_HOLD : ArmPose.ITEM;   // 0.10: автомат двумя руками
                        leftArmPose = ArmPose.EMPTY;
                        super.prepareMobModel(s, a, b, pt);
                    }
                }, 0.5f) {
            @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Skeleton s) { return gt; }
        });
        // 0.11: этаж 4 — НТЦ «NaZa»
        zombie3d(e, ModMobs.BOTANIST.get(), "botanist", "botanist", Mob3DParts.botanist(), 1f);
        zombie3d(e, ModMobs.THAWED.get(), "thawed", "thawed", Mob3DParts.thawed(), 1f);
        zombie3d(e, ModMobs.ROBOT.get(), "tech_robot", "robot", Mob3DParts.robot(), 1f);
        zombie3d(e, ModMobs.CHIMERA.get(), "chimera", "chimera", Mob3DParts.chimera(), 1f);
        spider(e, ModMobs.SPORELING.get(), "sporeling");
        gunner3d(e, ModMobs.SENTRY.get(), "naza_guard", "guard", Mob3DParts.guard(), true);
        gunner3d(e, ModMobs.LAB_TECH.get(), "lab_tech", "labtech", Mob3DParts.labtech(), true);
        gunner3d(e, ModMobs.TURRET.get(), "turret", "turret", Mob3DParts.turret(), false);
        e.registerEntityRenderer(ModMobs.RAT.get(), ctx -> new BeastRenderer<>(ctx, 0.3f));
        e.registerEntityRenderer(ModMobs.PET_RAT.get(), ctx -> new BeastRenderer<>(ctx, 0.3f));
        e.registerEntityRenderer(ModMobs.STREET_DOG.get(), ctx -> new BeastRenderer<>(ctx, 0.45f));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void zombie(EntityRenderersEvent.RegisterRenderers e, net.minecraft.world.entity.EntityType<? extends Zombie> t, String n) {
        ResourceLocation r = tex(n);
        e.registerEntityRenderer((net.minecraft.world.entity.EntityType) t, ctx -> new ZombieRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Zombie z) { return r; }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void zombie3d(EntityRenderersEvent.RegisterRenderers e, net.minecraft.world.entity.EntityType<? extends Zombie> t, String n, String parts,
                                 java.util.List<Mob3DLayer.Cube> cubes, float scale) {
        ResourceLocation r = tex(n);
        boolean claws = n.equals("werewolf");
        e.registerEntityRenderer((net.minecraft.world.entity.EntityType) t, ctx -> {
            HumanoidMobRenderer zr = new HumanoidMobRenderer(ctx, new FightModel<>(ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE), claws), 0.5f) {
                @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.Entity z) { return r; }
                @Override protected void scale(net.minecraft.world.entity.LivingEntity z, com.mojang.blaze3d.vertex.PoseStack ps, float pt) { super.scale(z, ps, pt); if (scale != 1f) ps.scale(scale, scale, scale); }
            };
            zr.addLayer(new Mob3DLayer(zr, parts, cubes));
            return zr;
        });
    }

    /** Лучники 64×64 на гуманоидной модели (на скелетной 64×32 у них пропадали ноги и «уезжало» лицо), с позой лука. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void archer3d(EntityRenderersEvent.RegisterRenderers e, net.minecraft.world.entity.EntityType<? extends AbstractSkeleton> t, String n, String parts,
                                 java.util.List<Mob3DLayer.Cube> cubes) {
        ResourceLocation r = tex(n);
        e.registerEntityRenderer((net.minecraft.world.entity.EntityType) t, ctx -> {
            var model = new net.minecraft.client.model.HumanoidModel<AbstractSkeleton>(ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE)) {
                @Override
                public void prepareMobModel(AbstractSkeleton s, float a, float b, float pt) {
                    rightArmPose = ArmPose.EMPTY; leftArmPose = ArmPose.EMPTY;
                    var st = s.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
                    if (st.is(net.minecraft.world.item.Items.BOW) && s.isAggressive()) {
                        if (s.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) rightArmPose = ArmPose.BOW_AND_ARROW; else leftArmPose = ArmPose.BOW_AND_ARROW;
                    }
                    super.prepareMobModel(s, a, b, pt);
                }
            };
            HumanoidMobRenderer hr = new HumanoidMobRenderer(ctx, model, 0.5f) {
                @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.Entity s) { return r; }
            };
            hr.addLayer(new Mob3DLayer(hr, parts, cubes));
            return hr;
        });
    }

    /** 0.11: стрелки этажа 4 (оружие двумя руками) с объёмными деталями; turret — только детали (скин прозрачный). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void gunner3d(EntityRenderersEvent.RegisterRenderers e, net.minecraft.world.entity.EntityType<? extends AbstractSkeleton> t, String n, String parts,
                                 java.util.List<Mob3DLayer.Cube> cubes, boolean arms) {
        ResourceLocation r = tex(n);
        e.registerEntityRenderer((net.minecraft.world.entity.EntityType) t, ctx -> {
            var model = new net.minecraft.client.model.HumanoidModel<AbstractSkeleton>(ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE)) {
                @Override public void prepareMobModel(AbstractSkeleton s, float a, float b, float pt) {
                    rightArmPose = arms ? (s.isAggressive() ? ArmPose.CROSSBOW_HOLD : ArmPose.ITEM) : ArmPose.EMPTY;
                    leftArmPose = ArmPose.EMPTY;
                    super.prepareMobModel(s, a, b, pt);
                }
                @Override public void setupAnim(AbstractSkeleton s, float a, float b, float c, float yaw, float pitch) {
                    super.setupAnim(s, arms ? a : 0, arms ? b : 0, c, yaw, pitch);
                    if (!arms) { rightArm.xRot = 0; leftArm.xRot = 0; rightArm.zRot = 0; leftArm.zRot = 0; rightLeg.xRot = 0; leftLeg.xRot = 0; }
                }
            };
            HumanoidMobRenderer hr = new HumanoidMobRenderer(ctx, model, arms ? 0.5f : 0.7f) {
                @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.Entity s) { return r; }
            };
            hr.addLayer(new Mob3DLayer(hr, parts, cubes));
            return hr;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void skeleton(EntityRenderersEvent.RegisterRenderers e, net.minecraft.world.entity.EntityType<? extends AbstractSkeleton> t, String n) {
        ResourceLocation r = tex(n);
        e.registerEntityRenderer((net.minecraft.world.entity.EntityType) t, ctx -> new SkeletonRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(AbstractSkeleton s) { return r; }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void spider(EntityRenderersEvent.RegisterRenderers e, net.minecraft.world.entity.EntityType<? extends Spider> t, String n) {
        ResourceLocation r = tex(n);
        e.registerEntityRenderer((net.minecraft.world.entity.EntityType) t, ctx -> new SpiderRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.Entity s) { return r; }
        });
    }
}
