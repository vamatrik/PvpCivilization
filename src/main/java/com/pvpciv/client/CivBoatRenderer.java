package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.PvpCiv;
import com.pvpciv.entity.CivBoat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 3D-лодки (Инеевая, Неоновое такси). Поворот — как у ванильной лодки. */
public class CivBoatRenderer extends GeoEntityRenderer<CivBoat> {
    public CivBoatRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<CivBoat>() {
            @Override public ResourceLocation getModelResource(CivBoat e) { return PvpCiv.id("geo/" + e.model() + ".geo.json"); }
            @Override public ResourceLocation getTextureResource(CivBoat e) { return PvpCiv.id("textures/entity/" + e.model() + ".png"); }
            @Override public ResourceLocation getAnimationResource(CivBoat e) { return PvpCiv.id("animations/civ_boat.animation.json"); }
        });
        this.shadowRadius = 0.7f;
    }

    @Override
    protected void applyRotations(CivBoat e, PoseStack pose, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        pose.mulPose(Axis.YP.rotationDegrees(180f - net.minecraft.util.Mth.rotLerp(partialTick, e.yRotO, e.getYRot())));
    }
}
