package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.PvpCiv;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 1.0 — голова и сегменты Галактического червя: модель наклоняется по направлению полёта (pitch). */
public class WormRenderer<T extends Mob & GeoEntity> extends GeoEntityRenderer<T> {
    public WormRenderer(EntityRendererProvider.Context ctx, String model, float scale) {
        super(ctx, new GeoModel<T>() {
            @Override public ResourceLocation getModelResource(T e) { return PvpCiv.id("geo/" + model + ".geo.json"); }
            @Override public ResourceLocation getTextureResource(T e) { return PvpCiv.id("textures/entity/" + model + ".png"); }
            @Override public ResourceLocation getAnimationResource(T e) { return PvpCiv.id("animations/worm.animation.json"); }
        });
        withScale(scale);
        this.shadowRadius = 0f;
    }

    @Override
    protected void applyRotations(T e, PoseStack ps, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(e, ps, ageInTicks, rotationYaw, partialTick, nativeScale);
        float pitch = Mth.lerp(partialTick, e.xRotO, e.getXRot());
        ps.translate(0, 0.6 * nativeScale, 0);
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        ps.translate(0, -0.6 * nativeScale, 0);
    }
}
