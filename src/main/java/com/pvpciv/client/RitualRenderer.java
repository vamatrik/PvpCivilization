package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.block.RitualBlock;
import com.pvpciv.block.RitualBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Ядро: оружие парит и поднимается во время ритуала. Инжектор: тёмный шар на острие, когда заряжен. */
public class RitualRenderer implements BlockEntityRenderer<RitualBlockEntity> {
    private static ItemStack ORB;

    @Override
    public void render(RitualBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null || !(be.getBlockState().getBlock() instanceof RitualBlock rb)) return;
        float t = be.getLevel().getGameTime() + pt;
        var ir = Minecraft.getInstance().getItemRenderer();
        if (rb.core) {
            if (be.item.isEmpty()) return;
            float a = be.anim > 0 ? Math.min(1f, (be.anim + pt) / 90f) : 0f;
            pose.pushPose();
            pose.translate(0.5, 1.35 + a * 1.2 + Math.sin(t / 16f) * 0.05, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(t * (2f + a * 14f)));
            float sc = 0.9f + (be.anim > 100 ? (be.anim - 100) / 20f * 0.3f : 0);
            pose.scale(sc, sc, sc);
            ir.renderStatic(be.item, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buf, be.getLevel(), 0);
            pose.popPose();
            return;
        }
        if (!be.lit) return;
        if (ORB == null) ORB = new ItemStack(com.pvpciv.registry.ModF5.DARK_STAR.get());
        Direction f = be.getBlockState().getValue(RitualBlock.FACING);
        pose.pushPose();
        pose.translate(0.5 + f.getStepX() * 0.62, 0.5 + Math.sin(t / 10f + be.getBlockPos().hashCode()) * 0.03, 0.5 + f.getStepZ() * 0.62);   // 1.2: инжектор лежит на боку, остриё — к ядру
        pose.mulPose(Axis.YP.rotationDegrees(t * 6f));
        pose.scale(0.32f, 0.32f, 0.32f);
        ir.renderStatic(ORB, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buf, be.getLevel(), 0);
        pose.popPose();
    }
}
