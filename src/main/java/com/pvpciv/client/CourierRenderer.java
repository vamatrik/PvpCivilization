package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvpciv.PvpCiv;
import com.pvpciv.entity.CityNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;

/** Курьер «Шпиль.Еды»: обычная модель игрока + жёлтый рюкзак-куб на спине. */
public class CourierRenderer extends NpcRenderer<CityNpcEntity> {
    public CourierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, false, PvpCiv.id("textures/entity/courier.png"));
        addLayer(new Backpack(this));
    }

    private static final class Backpack extends RenderLayer<CityNpcEntity, PlayerModel<CityNpcEntity>> {
        Backpack(RenderLayerParent<CityNpcEntity, PlayerModel<CityNpcEntity>> p) { super(p); }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, CityNpcEntity e, float limb, float limbAmt, float pt, float age, float yaw, float pitch) {
            var br = Minecraft.getInstance().getBlockRenderer();
            ps.pushPose();
            getParentModel().body.translateAndRotate(ps);
            // корпус рюкзака
            ps.pushPose();
            ps.translate(-0.30, -0.02, 0.14);
            ps.scale(0.60f, 0.66f, 0.40f);
            br.renderSingleBlock(Blocks.YELLOW_CONCRETE.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
            // тёмная полоса-клапан
            ps.pushPose();
            ps.translate(-0.31, 0.10, 0.13);
            ps.scale(0.62f, 0.07f, 0.42f);
            br.renderSingleBlock(Blocks.BLACK_CONCRETE.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
            ps.popPose();
        }
    }
}
