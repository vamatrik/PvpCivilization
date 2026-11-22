package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.block.TrophyShelfBlock;
import com.pvpciv.block.TrophyShelfBlockEntity;
import com.pvpciv.game.Showcases;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Трофей в ячейке витрины: парит и медленно вращается. */
public class TrophyShelfRenderer implements BlockEntityRenderer<TrophyShelfBlockEntity> {
    @Override
    public void render(TrophyShelfBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        if (!be.shown || be.getLevel() == null) return;
        Item it = Showcases.item(be.bit);
        if (it == null) return;
        float t = be.getLevel().getGameTime() + pt;
        var f = be.getBlockState().getValue(TrophyShelfBlock.FACING);
        pose.pushPose();
        // центр ячейки, чуть ближе к стенке
        pose.translate(0.5 - f.getStepX() * 0.06, 0.48 + Math.sin(t / 18f + be.bit) * 0.03, 0.5 - f.getStepZ() * 0.06);
        pose.mulPose(Axis.YP.rotationDegrees(t * 1.2f + be.bit * 37));
        pose.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(it), ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buf, be.getLevel(), 0);
        pose.popPose();
    }
}
