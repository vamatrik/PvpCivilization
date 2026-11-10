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
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void zombie(EntityRenderersEvent.RegisterRenderers e, net.minecraft.world.entity.EntityType<? extends Zombie> t, String n) {
        ResourceLocation r = tex(n);
        e.registerEntityRenderer((net.minecraft.world.entity.EntityType) t, ctx -> new ZombieRenderer(ctx) {
            @Override public ResourceLocation getTextureLocation(Zombie z) { return r; }
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
