package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.block.CivStationBlock;
import com.pvpciv.block.PedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Предмет, парящий и вращающийся над пьедесталом/алтарём инфузии. */
public class PedestalRenderer implements BlockEntityRenderer<PedestalBlockEntity> {
    @Override
    public void render(PedestalBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        ItemStack st = be.item();
        if (st.isEmpty() || be.getLevel() == null) return;
        boolean altar = be.getBlockState().getBlock() instanceof CivStationBlock b && b.kind == CivStationBlock.Kind.ALTAR;
        float t = (be.getLevel().getGameTime() + pt) + be.getBlockPos().asLong() % 100;
        pose.pushPose();
        pose.translate(0.5, (altar ? 1.35 : 1.15) + Math.sin(t / 12f) * 0.06, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(t * 2.5f % 360));
        float sc = altar ? 0.75f : 0.55f;
        pose.scale(sc, sc, sc);
        Minecraft.getInstance().getItemRenderer().renderStatic(st, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buf, be.getLevel(), 0);
        pose.popPose();
    }
}
