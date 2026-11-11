package com.pvpciv.client;

import com.pvpciv.PvpCiv;
import com.pvpciv.entity.boss.CivBoss;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Рендер боссов на GeckoLib: geo/&lt;model&gt;.geo.json, textures/entity/&lt;model&gt;.png, animations/&lt;anims&gt;.animation.json. */
public class BossRenderer<T extends CivBoss> extends GeoEntityRenderer<T> {
    public BossRenderer(EntityRendererProvider.Context ctx, String anims, float scale, float shadow) {
        super(ctx, new GeoModel<T>() {
            @Override public ResourceLocation getModelResource(T e) { return PvpCiv.id("geo/" + e.model() + ".geo.json"); }
            @Override public ResourceLocation getTextureResource(T e) { return PvpCiv.id("textures/entity/" + e.model() + ".png"); }
            @Override public ResourceLocation getAnimationResource(T e) { return PvpCiv.id("animations/" + anims + ".animation.json"); }
        });
        withScale(scale);
        this.shadowRadius = shadow;
    }
}
