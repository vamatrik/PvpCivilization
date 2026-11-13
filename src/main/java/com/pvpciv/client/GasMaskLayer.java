package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Противогаз ликвидатора в 3D (бета 0.9.6: «шлем как ледяной»): поверх головы игрока — маска с двумя круглыми
 * стёклами, патрубком и фильтром-банкой спереди, ремни на затылке.
 */
public class GasMaskLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public GasMaskLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> p) { super(p); }

    private static void box(PoseStack ps, MultiBufferSource buf, int light, Block b, double x, double y, double z, float sx, float sy, float sz) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.scale(sx, sy, sz);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(b.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limb, float limbAmt, float pt, float age, float yaw, float pitch) {
        if (!p.getItemBySlot(EquipmentSlot.HEAD).is(ModRegistry.HAZMAT_HELMET.get()) || p.isInvisible()) return;
        ps.pushPose();
        getParentModel().head.translateAndRotate(ps);
        // голова: x −0.25..0.25, y −0.5..0, лицо — z = −0.25 (минус — вперёд)
        box(ps, buf, light, Blocks.BLACK_CONCRETE, -0.27, -0.22, -0.30, 0.54f, 0.24f, 0.06f);       // резиновая маска (нижняя половина лица)
        box(ps, buf, light, Blocks.GRAY_CONCRETE, -0.27, -0.40, -0.285, 0.54f, 0.18f, 0.04f);      // лобная часть
        for (int s = -1; s <= 1; s += 2) {                                                          // круглые стёкла
            box(ps, buf, light, Blocks.GRAY_CONCRETE, s * 0.125 - 0.075, -0.38, -0.32, 0.15f, 0.15f, 0.04f);
            box(ps, buf, light, Blocks.LIGHT_BLUE_STAINED_GLASS, s * 0.125 - 0.055, -0.36, -0.33, 0.11f, 0.11f, 0.02f);
        }
        box(ps, buf, light, Blocks.GRAY_CONCRETE, -0.06, -0.15, -0.38, 0.12f, 0.10f, 0.09f);       // патрубок
        box(ps, buf, light, Blocks.GREEN_TERRACOTTA, -0.08, -0.17, -0.50, 0.16f, 0.14f, 0.13f);    // фильтр-банка
        box(ps, buf, light, Blocks.IRON_BLOCK, -0.085, -0.175, -0.51, 0.17f, 0.03f, 0.14f);         // ободок фильтра
        box(ps, buf, light, Blocks.BLACK_CONCRETE, -0.27, -0.33, 0.24, 0.54f, 0.04f, 0.04f);        // ремень на затылке
        box(ps, buf, light, Blocks.BLACK_CONCRETE, -0.27, -0.16, 0.24, 0.54f, 0.04f, 0.04f);
        ps.popPose();
    }
}
