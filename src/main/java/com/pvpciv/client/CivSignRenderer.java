package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.block.CivSignBlock;
import com.pvpciv.block.CivSignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;

/** Текст стилизованной таблички: светится, строки сжимаются по ширине. */
public class CivSignRenderer implements BlockEntityRenderer<CivSignBlockEntity> {
    @Override
    public void render(CivSignBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        var st = be.getBlockState();
        if (!(st.getBlock() instanceof CivSignBlock)) return;
        Direction f = st.getValue(CivSignBlock.FACING);
        boolean standing = st.getValue(CivSignBlock.STANDING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-f.toYRot() + 180));
        // локально: лицевая сторона смотрит в −z
        if (standing) {
            draw(be, pose, buf, -0.0675, 0.125);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            draw(be, pose, buf, -0.0675, 0.125);
        } else draw(be, pose, buf, 0.37, 0.0);
        pose.popPose();
    }

    private static void draw(CivSignBlockEntity be, PoseStack pose, MultiBufferSource buf, double z, double dy) {
        Font font = Minecraft.getInstance().font;
        pose.pushPose();
        pose.translate(0, dy, z - 0.006);
        float s = 0.0125f;
        pose.scale(-s, -s, s);
        int n = 0;
        for (String l : be.lines) if (!l.isEmpty()) n++;
        int row = 0;
        for (int i = 0; i < 4; i++) {
            String l = be.lines[i];
            if (l.isEmpty() && n < 4) continue;
            int w = font.width(l);
            float k = w > 76 ? 76f / w : 1f;
            pose.pushPose();
            float y = (row - (Math.max(n, 1) - 1) / 2f) * 10f;
            pose.translate(0, y, 0);
            pose.scale(k, k, 1);
            font.drawInBatch(l, -w / 2f, -4, 0xFF000000 | be.color, false, pose.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, 0xF000F0);
            pose.popPose();
            row++;
        }
        pose.popPose();
    }
}
