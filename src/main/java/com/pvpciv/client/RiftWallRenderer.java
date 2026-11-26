package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvpciv.entity.RiftWallEntity;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** 1.5.3: столбик едущей стены — рисуем неоновые блоки (цельные, без теней), позиция плавная. */
public class RiftWallRenderer extends EntityRenderer<RiftWallEntity> {
    public RiftWallRenderer(EntityRendererProvider.Context c) { super(c); shadowRadius = 0; }

    @Override
    public void render(RiftWallEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        BlockState st = switch (e.kind()) {
            case 1 -> ModRegistry.RIFT_WALL_WEAK.get().defaultBlockState();
            case 2 -> ModRegistry.RIFT_STEP.get().defaultBlockState();
            default -> ModRegistry.RIFT_WALL.get().defaultBlockState();
        };
        var br = Minecraft.getInstance().getBlockRenderer();
        for (int i = 0; i < e.len(); i++) {
            ps.pushPose();
            ps.translate(-0.5, i, -0.5);
            br.renderSingleBlock(st, ps, buf, 0xF000F0, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
    }

    @Override public boolean shouldRender(RiftWallEntity e, Frustum f, double x, double y, double z) { return f.isVisible(e.getBoundingBox().inflate(0.5)); }
    @Override public ResourceLocation getTextureLocation(RiftWallEntity e) { return TextureAtlas.LOCATION_BLOCKS; }
}
