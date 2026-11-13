package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.PvpCiv;
import com.pvpciv.entity.CityNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** NPC этажа 3: скин + объёмные детали. Юки — катана за спиной, высокий хвост, алый шарф; Сёдо — шапка-эбоси и свиток за спиной. */
public class Floor3NpcRenderer extends NpcRenderer<CityNpcEntity> {
    public Floor3NpcRenderer(EntityRendererProvider.Context ctx, boolean assassin) {
        super(ctx, assassin, PvpCiv.id(assassin ? "textures/entity/assassin.png" : "textures/entity/librarian.png"));
        addLayer(new Details(this, assassin));
    }

    private static final class Details extends RenderLayer<CityNpcEntity, PlayerModel<CityNpcEntity>> {
        final boolean assassin;
        Details(RenderLayerParent<CityNpcEntity, PlayerModel<CityNpcEntity>> p, boolean a) { super(p); assassin = a; }

        static void box(PoseStack ps, MultiBufferSource buf, int light, Block b, double x, double y, double z, float sx, float sy, float sz) {
            ps.pushPose();
            ps.translate(x, y, z);
            ps.scale(sx, sy, sz);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(b.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, CityNpcEntity e, float limb, float limbAmt, float pt, float age, float yaw, float pitch) {
            PlayerModel<CityNpcEntity> m = getParentModel();
            if (assassin) {
                // катана за спиной, по диагонали: ножны, цуба, рукоять с красной обмоткой
                ps.pushPose();
                m.body.translateAndRotate(ps);
                ps.translate(0, 0.38, 0.15);
                ps.mulPose(Axis.ZP.rotationDegrees(42));
                box(ps, buf, light, Blocks.BLACK_CONCRETE, -0.03, -0.45, 0, 0.06f, 0.62f, 0.05f);
                box(ps, buf, light, Blocks.GOLD_BLOCK, -0.06, -0.48, -0.015, 0.12f, 0.03f, 0.08f);
                box(ps, buf, light, Blocks.RED_CONCRETE, -0.025, -0.70, 0.005, 0.05f, 0.22f, 0.04f);
                ps.popPose();
                // алый шарф: узел на шее и два хвоста, качаются
                ps.pushPose();
                m.body.translateAndRotate(ps);
                box(ps, buf, light, Blocks.RED_WOOL, -0.26, -0.03, -0.15, 0.52f, 0.08f, 0.30f);
                float sway = (float) Math.sin(age * 0.12) * 6 + limbAmt * 20;
                for (int s = -1; s <= 1; s += 2) {
                    ps.pushPose();
                    ps.translate(s * 0.06, 0.03, 0.15);
                    ps.mulPose(Axis.XP.rotationDegrees(18 + sway));
                    box(ps, buf, light, Blocks.RED_WOOL, -0.04, 0, 0, 0.08f, 0.34f, 0.02f);
                    ps.popPose();
                }
                ps.popPose();
                // высокий хвост
                ps.pushPose();
                m.head.translateAndRotate(ps);
                box(ps, buf, light, Blocks.RED_CONCRETE, -0.05, -0.47, 0.24, 0.10f, 0.05f, 0.05f);
                ps.translate(0, -0.44, 0.27);
                ps.mulPose(Axis.XP.rotationDegrees(25 + (float) Math.sin(age * 0.15) * 5));
                box(ps, buf, light, Blocks.BLACK_WOOL, -0.07, 0, 0, 0.14f, 0.42f, 0.10f);
                ps.popPose();
            } else {
                // эбоси — высокая чёрная шапка
                ps.pushPose();
                m.head.translateAndRotate(ps);
                box(ps, buf, light, Blocks.BLACK_CONCRETE, -0.17, -0.70, -0.14, 0.34f, 0.22f, 0.30f);
                box(ps, buf, light, Blocks.BLACK_CONCRETE, -0.12, -0.86, -0.06, 0.24f, 0.18f, 0.22f);
                box(ps, buf, light, Blocks.WHITE_CONCRETE, -0.175, -0.50, -0.145, 0.35f, 0.02f, 0.31f);
                // круглые очки
                box(ps, buf, light, Blocks.GOLD_BLOCK, -0.20, -0.28, -0.255, 0.16f, 0.02f, 0.01f);
                box(ps, buf, light, Blocks.GOLD_BLOCK, 0.04, -0.28, -0.255, 0.16f, 0.02f, 0.01f);
                ps.popPose();
                // свиток за спиной
                ps.pushPose();
                m.body.translateAndRotate(ps);
                ps.translate(0, 0.3, 0.14);
                ps.mulPose(Axis.ZP.rotationDegrees(-70));
                box(ps, buf, light, Blocks.BIRCH_PLANKS, -0.05, -0.28, 0, 0.10f, 0.56f, 0.10f);
                box(ps, buf, light, Blocks.RED_CONCRETE, -0.055, -0.03, -0.005, 0.11f, 0.06f, 0.11f);
                box(ps, buf, light, Blocks.DARK_OAK_PLANKS, -0.06, -0.32, -0.01, 0.12f, 0.04f, 0.12f);
                box(ps, buf, light, Blocks.DARK_OAK_PLANKS, -0.06, 0.28, -0.01, 0.12f, 0.04f, 0.12f);
                ps.popPose();
            }
        }
    }
}
