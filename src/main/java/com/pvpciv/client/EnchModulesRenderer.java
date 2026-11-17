package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 0.11.2 — модули молекулярного стола видны в мире, но только их владельцу: 8 гнёзд по кругу у стола,
 * в каждом — модуль своего уровня (парит и медленно вращается). Данные — из синхронизации стола (PhoneData 19/21).
 */
public final class EnchModulesRenderer {
    private EnchModulesRenderer() {}
    public static int[] MODS = new int[8];
    private static ItemStack[] STACKS;

    private static ItemStack stack(int tier) {
        if (STACKS == null) STACKS = new ItemStack[]{ItemStack.EMPTY, new ItemStack(com.pvpciv.registry.ModF4.MODULE_BASIC.get()),
                new ItemStack(com.pvpciv.registry.ModF4.MODULE_IMPROVED.get()), new ItemStack(com.pvpciv.registry.ModF4.MODULE_ADVANCED.get())};
        return STACKS[Math.max(0, Math.min(3, tier))];
    }

    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.level.dimension().location().getPath().equals("world1_f4")) return;
        var t = com.pvpciv.floor.Floor4.ENCH_TABLE;
        Vec3 c = new Vec3(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
        if (mc.player.position().distanceTo(c) > 32) return;
        boolean any = false;
        for (int m : MODS) if (m > 0) any = true;
        if (!any) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        var buf = mc.renderBuffers().bufferSource();
        float time = (mc.level.getGameTime() + e.getPartialTick().getGameTimeDeltaPartialTick(false)) / 20f;
        for (int i = 0; i < 8; i++) {
            if (MODS[i] <= 0) continue;
            double a = Math.PI * 2 * i / 8;
            double x = t.getX() + Math.round(Math.cos(a) * 3) + 0.5, z = t.getZ() + Math.round(Math.sin(a) * 3) + 0.5;
            double y = t.getY() + 0.55 + Math.sin(time * 2 + i) * 0.06;
            ps.pushPose();
            ps.translate(x - cam.x, y - cam.y, z - cam.z);
            ps.mulPose(Axis.YP.rotationDegrees(time * 40 + i * 45));
            ps.scale(0.8f, 0.8f, 0.8f);
            mc.getItemRenderer().renderStatic(stack(MODS[i]), ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ps, buf, mc.level, i);
            ps.popPose();
        }
        buf.endBatch();
    }
}
