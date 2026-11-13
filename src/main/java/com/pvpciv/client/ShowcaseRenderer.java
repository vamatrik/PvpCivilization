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
        double[][] spots = {{0.25, 0.32}, {0.5, 0.32}, {0.75, 0.32}, {0.25, 0.68}, {0.5, 0.68}, {0.75, 0.68}};
        int[] order = {0, 1, 3, 4, 2, 5};   // старые 4 трофея — по углам, как раньше
        for (int i = 0; i < 6; i++) {
            if ((be.bits & (1 << i)) == 0) continue;
            pose.pushPose();
            pose.translate(spots[order[i]][0], 0.5 + Math.sin(t / 20f + i) * 0.03, spots[order[i]][1]);
            pose.mulPose(Axis.YP.rotationDegrees(t * 1.5f + i * 90));
            pose.scale(0.32f, 0.32f, 0.32f);
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ShowcaseBlockEntity.trophy(i)), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buf, be.getLevel(), 0);
            pose.popPose();
        }
    }
}
