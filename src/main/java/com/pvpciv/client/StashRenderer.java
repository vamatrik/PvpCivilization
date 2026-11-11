package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.PvpCiv;
import com.pvpciv.block.StashBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Крышка тайника: закрыта, если тайник для тебя готов; откинута, если ты его уже обчистил. */
public class StashRenderer implements BlockEntityRenderer<StashBlockEntity> {
    public static final ModelResourceLocation LID = ModelResourceLocation.standalone(PvpCiv.id("block/stash_lid"));

    @Override
    public void render(StashBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        BakedModel lid = Minecraft.getInstance().getModelManager().getModel(LID);
        boolean open = ClientState.stashLooted(be.getBlockPos());
        float yaw = switch (be.getBlockState().getValue(HorizontalDirectionalBlock.FACING)) {
            case EAST -> -90; case SOUTH -> 180; case WEST -> 90; default -> 0;
        };
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5, 0, -0.5);
        if (open) {   // петли у задней стенки: z = 14.5/16, y = 10/16
            pose.translate(0, 10 / 16f, 14.5f / 16f);
            pose.mulPose(Axis.XP.rotationDegrees(105));
            pose.translate(0, -10 / 16f, -14.5f / 16f);
        }
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buf.getBuffer(RenderType.cutout()),
                be.getBlockState(), lid, 1f, 1f, 1f, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }
}
