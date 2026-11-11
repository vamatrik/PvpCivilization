package com.pvpciv.client;

import com.pvpciv.PvpCiv;
import com.pvpciv.entity.LiftCabin;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Кабина лифта: полупрозрачные стёкла. */
public class LiftCabinRenderer extends GeoEntityRenderer<LiftCabin> {
    public LiftCabinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<LiftCabin>() {
            @Override public ResourceLocation getModelResource(LiftCabin e) { return PvpCiv.id("geo/lift_cabin.geo.json"); }
            @Override public ResourceLocation getTextureResource(LiftCabin e) { return PvpCiv.id("textures/entity/lift_cabin.png"); }
            @Override public ResourceLocation getAnimationResource(LiftCabin e) { return PvpCiv.id("animations/lift_cabin.animation.json"); }
        });
        this.shadowRadius = 1.2f;
    }

    @Override
    public RenderType getRenderType(LiftCabin e, ResourceLocation tex, MultiBufferSource buf, float pt) {
        return RenderType.entityTranslucent(tex);
    }
}
