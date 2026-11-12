package com.pvpciv.client;

import com.pvpciv.PvpCiv;
import com.pvpciv.entity.beast.CivBeast;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Звери на GeckoLib (крыса, дворняга): общие анимации beast. */
public class BeastRenderer<T extends CivBeast> extends GeoEntityRenderer<T> {
    public BeastRenderer(EntityRendererProvider.Context ctx, float shadow) {
        super(ctx, new GeoModel<T>() {
            @Override public ResourceLocation getModelResource(T e) { return PvpCiv.id("geo/" + e.model() + ".geo.json"); }
            @Override public ResourceLocation getTextureResource(T e) { return PvpCiv.id("textures/entity/" + e.model() + ".png"); }
            @Override public ResourceLocation getAnimationResource(T e) { return PvpCiv.id("animations/beast.animation.json"); }
        });
        this.shadowRadius = shadow;
    }

    /** Боевая крыса крупнее (под увеличенный хитбокс), ручная — как была. */
    @Override
    public void preRender(com.mojang.blaze3d.vertex.PoseStack ps, T e, software.bernie.geckolib.cache.object.BakedGeoModel m, net.minecraft.client.renderer.MultiBufferSource buf,
                          com.mojang.blaze3d.vertex.VertexConsumer vc, boolean reRender, float pt, int light, int overlay, int color) {
        if (e instanceof com.pvpciv.entity.beast.CivRat && !reRender) ps.scale(1.4f, 1.4f, 1.4f);
        if (e instanceof com.pvpciv.entity.beast.CivDog && !reRender) ps.scale(1.15f, 1.15f, 1.15f);
        super.preRender(ps, e, m, buf, vc, reRender, pt, light, overlay, color);
    }
}
