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
}
