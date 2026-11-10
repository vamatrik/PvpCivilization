package com.pvpciv.client;

import com.pvpciv.PvpCiv;
import com.pvpciv.entity.GeoNpc;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Рендер HD-NPC (Луна, Кармилла, Элеонора) на GeckoLib. */
public class GeoNpcRenderer<T extends GeoNpc> extends GeoEntityRenderer<T> {
    public GeoNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<T>() {
            @Override public ResourceLocation getModelResource(T e) { return PvpCiv.id("geo/" + e.model() + ".geo.json"); }
            @Override public ResourceLocation getTextureResource(T e) { return PvpCiv.id("textures/entity/" + e.texture() + ".png"); }
            @Override public ResourceLocation getAnimationResource(T e) { return PvpCiv.id("animations/civ_npc.animation.json"); }
        });
        this.shadowRadius = 0.45f;
    }
}
