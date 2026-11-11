package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.block.ShowcaseBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Трофеи внутри витрины: до 4 штук, медленно вращаются. */
public class ShowcaseRenderer implements BlockEntityRenderer<ShowcaseBlockEntity> {
    @Override
    public void render(ShowcaseBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        if (be.bits == 0 || be.getLevel() == null) return;
        float t = be.getLevel().getGameTime() + pt;
        double[][] spots = {{0.32, 0.32}, {0.68, 0.32}, {0.32, 0.68}, {0.68, 0.68}};
        for (int i = 0; i < 4; i++) {
            if ((be.bits & (1 << i)) == 0) continue;
            pose.pushPose();
            pose.translate(spots[i][0], 0.5 + Math.sin(t / 20f + i) * 0.03, spots[i][1]);
            pose.mulPose(Axis.YP.rotationDegrees(t * 1.5f + i * 90));
            pose.scale(0.4f, 0.4f, 0.4f);
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ShowcaseBlockEntity.trophy(i)), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buf, be.getLevel(), 0);
            pose.popPose();
        }
    }
}
