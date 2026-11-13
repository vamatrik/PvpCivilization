package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.PvpCiv;
import com.pvpciv.entity.TraderEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Сорока (бета 0.8.7: «немного 3D, лицо оставить»): скин тот же, сверху — объёмные детали
 * в сорочьих цветах: пушистый белый воротник, фалды-«хвост» с бирюзовыми кончиками,
 * перо в волосах, сумка-кошель с золотой застёжкой на перевязи.
 */
public class SorokaRenderer extends NpcRenderer<TraderEntity> {
    public SorokaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, true, PvpCiv.id("textures/entity/trader.png"));
        addLayer(new Details(this));
    }

    private static final class Details extends RenderLayer<TraderEntity, PlayerModel<TraderEntity>> {
        Details(RenderLayerParent<TraderEntity, PlayerModel<TraderEntity>> p) { super(p); }

        private static void box(PoseStack ps, MultiBufferSource buf, int light, Block b, double x, double y, double z, float sx, float sy, float sz) {
            ps.pushPose();
            ps.translate(x, y, z);
            ps.scale(sx, sy, sz);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(b.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, TraderEntity e, float limb, float limbAmt, float pt, float age, float yaw, float pitch) {
            PlayerModel<TraderEntity> m = getParentModel();
            // ---- корпус
            ps.pushPose();
            m.body.translateAndRotate(ps);
            // воротник
            box(ps, buf, light, Blocks.WHITE_WOOL, -0.28, -0.05, -0.155, 0.56f, 0.10f, 0.31f);
            box(ps, buf, light, Blocks.WHITE_WOOL, -0.20, 0.04, -0.16, 0.40f, 0.05f, 0.04f);
            // перевязь через грудь
            ps.pushPose();
            ps.translate(-0.20, 0.02, -0.14);
            ps.mulPose(Axis.ZP.rotationDegrees(-34));
            box(ps, buf, light, Blocks.BROWN_TERRACOTTA, 0, 0, 0, 0.05f, 0.80f, 0.02f);
            ps.popPose();
            // сумка-кошель на бедре + застёжка
            box(ps, buf, light, Blocks.BROWN_TERRACOTTA, 0.06, 0.50, -0.20, 0.19f, 0.17f, 0.075f);
            box(ps, buf, light, Blocks.DARK_OAK_PLANKS, 0.055, 0.49, -0.205, 0.20f, 0.06f, 0.08f);
            box(ps, buf, light, Blocks.GOLD_BLOCK, 0.135, 0.53, -0.212, 0.04f, 0.04f, 0.01f);
            // фалды — сорочий хвост, чуть в стороны и назад, качаются при ходьбе
            float sway = (float) Math.sin(limb * 0.6) * limbAmt * 8f;
            for (int s = -1; s <= 1; s += 2) {
                ps.pushPose();
                ps.translate(s * 0.07, 0.62, 0.115);
                ps.mulPose(Axis.XP.rotationDegrees(14 + sway * s));
                ps.mulPose(Axis.ZP.rotationDegrees(s * 6));
                box(ps, buf, light, Blocks.BLACK_CONCRETE, s < 0 ? -0.16 : 0, 0, 0, 0.16f, 0.40f, 0.03f);
                box(ps, buf, light, Blocks.WHITE_CONCRETE, s < 0 ? -0.16 : 0, 0.26, -0.002, 0.16f, 0.08f, 0.034f);
                box(ps, buf, light, Blocks.CYAN_CONCRETE, s < 0 ? -0.16 : 0, 0.36, -0.003, 0.16f, 0.06f, 0.036f);
                ps.popPose();
            }
            ps.popPose();
            // ---- голова: перо за ухом
            ps.pushPose();
            m.head.translateAndRotate(ps);
            ps.translate(0.20, -0.50, 0.10);
            ps.mulPose(Axis.ZP.rotationDegrees(-18));
            ps.mulPose(Axis.XP.rotationDegrees(-20));
            box(ps, buf, light, Blocks.BLACK_CONCRETE, 0, -0.22, 0, 0.025f, 0.24f, 0.07f);
            box(ps, buf, light, Blocks.CYAN_CONCRETE, 0, -0.30, 0, 0.026f, 0.09f, 0.071f);
            box(ps, buf, light, Blocks.GOLD_BLOCK, -0.01, -0.02, 0.015, 0.045f, 0.04f, 0.04f);
            ps.popPose();
        }
    }
}
