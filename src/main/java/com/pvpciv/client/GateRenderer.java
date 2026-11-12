package com.pvpciv.client;

import com.pvpciv.PvpCiv;
import com.pvpciv.entity.GateEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Врата: камень + кристалл; цвет кристалла и рун по этажу (иней / пепел / неон), свечение — только у работающих. */
public class GateRenderer extends GeoEntityRenderer<GateEntity> {
    public GateRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<GateEntity>() {
            @Override public ResourceLocation getModelResource(GateEntity e) { return PvpCiv.id("geo/gate.geo.json"); }
            @Override public ResourceLocation getTextureResource(GateEntity e) { return PvpCiv.id("textures/entity/gate_" + com.pvpciv.game.Gates.style(e.gateId()) + ".png"); }
            @Override public ResourceLocation getAnimationResource(GateEntity e) { return PvpCiv.id("animations/gate.animation.json"); }
        });
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(com.mojang.blaze3d.vertex.PoseStack ps, GateEntity e, software.bernie.geckolib.cache.object.BakedGeoModel m, net.minecraft.client.renderer.RenderType rt,
                               net.minecraft.client.renderer.MultiBufferSource buf, com.mojang.blaze3d.vertex.VertexConsumer vc, float pt, int light, int overlay) {
                if (e.gateId().equals("hub") || ClientState.gatesFixed.contains(e.gateId())) super.render(ps, e, m, rt, buf, vc, pt, light, overlay);
            }
        });
        this.shadowRadius = 1.1f;
        withScale(1.3f);
    }

    /** Врата общежития — большие, в атриуме. */
    @Override
    public void preRender(com.mojang.blaze3d.vertex.PoseStack ps, GateEntity e, software.bernie.geckolib.cache.object.BakedGeoModel m, net.minecraft.client.renderer.MultiBufferSource buf,
                          com.mojang.blaze3d.vertex.VertexConsumer vc, boolean reRender, float pt, int light, int overlay, int color) {
        if (!reRender && e.gateId().equals("hub")) ps.scale(1.35f, 1.35f, 1.35f);
        super.preRender(ps, e, m, buf, vc, reRender, pt, light, overlay, color);
    }
}
