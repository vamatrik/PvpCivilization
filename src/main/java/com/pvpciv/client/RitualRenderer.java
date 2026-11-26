package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pvpciv.PvpCiv;
import com.pvpciv.block.RitualBlock;
import com.pvpciv.block.RitualBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Ядро: оружие парит и поднимается во время ритуала. Инжектор: тёмный шар на острие, когда заряжен.
 * 1.4 — 3D-ритуал: объёмные лучи от инжекторов, руна на полу, три кольца вокруг оружия сжимаются,
 * столб света и ударная волна на взрыве. Потом итог лежит на ядре 30 с: кольцо-таймер и имя владельца.
 */
public class RitualRenderer implements BlockEntityRenderer<RitualBlockEntity> {
    private static ItemStack ORB;
    private static final ResourceLocation BEAM = PvpCiv.id("textures/misc/rift_beam.png"), LINE = PvpCiv.id("textures/misc/rift_line.png");
    private static final int RISE = 90, BOOM = 120, GROW = 150, BLAST = 180, LIE = 600;

    @Override public AABB getRenderBoundingBox(RitualBlockEntity be) { return new AABB(be.getBlockPos()).inflate(6, 6, 6); }
    @Override public boolean shouldRenderOffScreen(RitualBlockEntity be) { return true; }

    @Override
    public void render(RitualBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null || !(be.getBlockState().getBlock() instanceof RitualBlock rb)) return;
        float t = be.getLevel().getGameTime() + pt;
        var ir = Minecraft.getInstance().getItemRenderer();
        if (rb.core) {
            float an = be.animNow(pt);
            if (an > 0) ritualFx(be, an, t, pose, buf);
            if (be.item.isEmpty() || (an >= BOOM && an < BLAST + 6)) return;   // предмет внутри тьмы — не видно до взрыва
            float a = an <= 0 ? 0f : an < BLAST ? Math.min(1f, an / 90f) : Math.max(0f, 1f - (an - BLAST - 6) / 40f);   // после взрыва итог плавно опускается на ядро
            float wait = be.waitNow(pt);
            pose.pushPose();
            pose.translate(0.5, 1.35 + a * 1.2 + Math.sin(t / 16f) * 0.05, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(t * (2f + a * 14f)));
            float sc = 0.9f + (an > 100 && an < BOOM ? Math.min(1, (an - 100) / 20f) * 0.3f : 0);
            pose.scale(sc, sc, sc);
            ir.renderStatic(be.item, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buf, be.getLevel(), 0);
            pose.popPose();
            if (an <= 0 && wait > 0) lying(be, wait, t, pose, buf);
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

    // ------------------------------------------------------------------ 3D-ритуал
    private static void ritualFx(RitualBlockEntity be, float an, float t, PoseStack pose, MultiBufferSource buf) {
        Matrix4f m = pose.last().pose();
        BlockPos c = be.getBlockPos();
        float rise = Math.min(1f, an / RISE);
        Vec3 mid = new Vec3(0.5, 1.6 + rise * 1.2, 0.5);
        VertexConsumer vb = buf.getBuffer(RenderType.eyes(BEAM));
        float scroll = t * 0.12f;
        if (an < BOOM) {
            // лучи от зажжённых инжекторов к оружию
            for (Direction d : Direction.Plane.HORIZONTAL) {
                Direction along = d.getClockWise();
                for (int k = -1; k <= 1; k += 2) for (int h = 0; h <= 2; h += 2) {
                    BlockPos ip = c.relative(d, 4).relative(along, k).above(h);
                    if (!(be.getLevel().getBlockEntity(ip) instanceof RitualBlockEntity ib) || !ib.lit) continue;
                    Vec3 tip = Vec3.atCenterOf(ip.subtract(c)).add(Vec3.atLowerCornerOf(d.getOpposite().getNormal()).scale(0.6));
                    float w = 0.03f + 0.05f * Math.min(1f, an / 60f);
                    float fl = 0.75f + 0.25f * (float) Math.sin(t * 0.7 + ip.hashCode());
                    RiftFx.box(vb, m, mid, tip, w * 2.6f, 0.18f * fl, 0.04f * fl, 0.3f * fl, -scroll);
                    RiftFx.box(vb, m, mid, tip, w, 0.75f * fl, 0.25f * fl, 1f * fl, -scroll);
                    RiftFx.box(vb, m, mid, tip, w * 0.35f, 0.7f, 0.6f, 0.8f, -scroll * 1.5f);
                }
            }
            // три кольца вокруг оружия — сжимаются к взрыву
            float rr = 1.5f - 1.15f * Math.min(1f, an / BOOM);
            for (int i = 0; i < 3; i++) {
                double tilt = Math.toRadians(55 + i * 20), spin = t * (0.05 + i * 0.03) + i * 2.1;
                Vec3 ax1 = new Vec3(Math.cos(spin), 0, Math.sin(spin)), up = new Vec3(0, 1, 0);
                Vec3 ax2 = up.scale(Math.cos(tilt)).add(ax1.cross(up).scale(Math.sin(tilt)));
                int n = 28;
                for (int s = 0; s < n; s++) {
                    if (s % 7 == 6) continue;   // разрывы — как руны
                    double a0 = Math.PI * 2 * s / n + t * 0.08 * (i % 2 == 0 ? 1 : -1), a1 = a0 + Math.PI * 2 / n;
                    Vec3 p0 = mid.add(ax1.scale(Math.cos(a0) * rr)).add(ax2.scale(Math.sin(a0) * rr));
                    Vec3 p1 = mid.add(ax1.scale(Math.cos(a1) * rr)).add(ax2.scale(Math.sin(a1) * rr));
                    float k = 0.5f + rise * 0.5f;
                    RiftFx.box(vb, m, p0, p1, 0.025f, (i == 1 ? 1f : 0.75f) * k, (i == 1 ? 0.55f : 0.2f) * k, 1f * k, scroll);
                }
            }
            // столб света после подъёма
            if (an > RISE) {
                float q = (an - RISE) / (BOOM - RISE);
                RiftFx.box(vb, m, new Vec3(0.5, 0.05, 0.5), new Vec3(0.5, 7.5, 0.5), 0.05f + q * 0.25f, 0.5f * q, 0.15f * q, 0.9f * q, scroll * 2);
            }
        } else if (an < BLAST + 16) {
            darkBlast(pose, buf, an, mid, t);
        }
        // руна на полу
        VertexConsumer vl = buf.getBuffer(RenderType.eyes(LINE));
        float glow = an < BOOM ? Math.min(1f, an / 50f) : an < BLAST ? 0.25f + 0.75f * (an - BOOM) / (BLAST - BOOM) * ((an - BOOM) / (BLAST - BOOM)) : Math.max(0, 1 - (an - BLAST) / 30f);
        float[] pur = {0.75f, 0.25f, 1f}, gold = {1f, 0.75f, 0.3f};
        ring(vl, m, 4.0, 72, 0.06, pur, glow * 0.9f, t * 0.01, 0);
        ring(vl, m, 2.6, 52, 0.05, pur, glow * 0.8f, -t * 0.015, 0);
        ring(vl, m, 1.2, 30, 0.045, gold, glow * 0.7f, t * 0.03, 0);
        for (int i = 0; i < 24; i++) {   // засечки-руны между кольцами
            double a = Math.PI * 2 * i / 24 + t * 0.01;
            double r0 = i % 3 == 0 ? 2.75 : 3.1, r1 = 3.85;
            RiftFx.strip(vl, m, 0.5 + Math.cos(a) * r0, 0.5 + Math.sin(a) * r0, 0.5 + Math.cos(a) * r1, 0.5 + Math.sin(a) * r1, 0.02, 0.05, pur, glow * 0.7f);
        }
        for (int i = 0; i < 6; i++) {   // гексаграмма
            double a0 = Math.PI * 2 * i / 6 - t * 0.015, a1 = a0 + Math.PI * 2 / 6 * 2;
            RiftFx.strip(vl, m, 0.5 + Math.cos(a0) * 2.6, 0.5 + Math.sin(a0) * 2.6, 0.5 + Math.cos(a1) * 2.6, 0.5 + Math.sin(a1) * 2.6, 0.021, 0.045, gold, glow * 0.55f);
        }
        // ударная волна
        if (an >= BLAST && an < BLAST + 30) {
            float f = (an - BLAST) / 30f;
            ring(vl, m, 0.5 + f * 8, 90, 0.25 * (1 - f) + 0.05, new float[]{1f, 0.7f, 1f}, (1 - f) * 1.6f, 0, 0.03);
            ring(vl, m, 0.3 + f * 5, 70, 0.15 * (1 - f) + 0.04, pur, (1 - f) * 1.2f, 0, 0.035);
        }
    }

    /** Тёмный взрыв: чёрная сфера раздувается, дрожит и схлопывается; по краю — фиолетовая кайма. */
    private static void darkBlast(PoseStack pose, MultiBufferSource buf, float e, Vec3 c, float t) {
        // e — тик ритуала: пауза (сгусток дрожит) → рост → взрыв (сфера лопается и тает)
        float r, alpha;
        if (e < GROW) { r = 0.32f + 0.04f * (float) Math.sin(t * 3.1) + Math.min(1f, (e - BOOM) / 4f) * 0.05f; alpha = 0.95f; }
        else if (e < BLAST) { float q = (e - GROW) / (BLAST - GROW); r = 0.36f + 3.3f * q * q + 0.08f * (float) Math.sin(t * 2.3); alpha = 0.92f; }
        else { float q = (e - BLAST) / 16f; r = 3.7f + 4.5f * q; alpha = 0.92f * (1 - q) * (1 - q); }
        if (r <= 0.02f) return;
        Matrix4f m = pose.last().pose();
        int lat = 14, lon = 28;
        VertexConsumer vd = buf.getBuffer(RenderType.debugQuads());
        for (int pass = 0; pass < 2; pass++) {   // внутренняя (плотная) и внешняя (дымка)
            float rr = pass == 0 ? r * 0.82f : r, aa = pass == 0 ? alpha : alpha * 0.45f;
            for (int i = 0; i < lat; i++) for (int j = 0; j < lon; j++) {
                Vec3 p00 = sph(c, rr, i, j, lat, lon, t), p10 = sph(c, rr, i + 1, j, lat, lon, t), p11 = sph(c, rr, i + 1, j + 1, lat, lon, t), p01 = sph(c, rr, i, j + 1, lat, lon, t);
                for (Vec3 q : new Vec3[]{p00, p10, p11, p01}) vd.addVertex(m, (float) q.x, (float) q.y, (float) q.z).setColor(0.03f, 0f, 0.06f, aa);
            }
        }
        VertexConsumer vb = buf.getBuffer(RenderType.eyes(BEAM));
        float k = alpha;
        for (int ring = 0; ring < 3; ring++) {   // кайма: три наклонных кольца по поверхности
            double tilt = ring * Math.PI / 3 + t * 0.05;
            Vec3 ax1 = new Vec3(Math.cos(tilt), 0, Math.sin(tilt)), ax2 = new Vec3(0, 1, 0).scale(Math.cos(ring * 0.7)).add(ax1.cross(new Vec3(0, 1, 0)).scale(Math.sin(ring * 0.7)));
            for (int s = 0; s < 40; s++) {
                double a0 = Math.PI * 2 * s / 40, a1 = Math.PI * 2 * (s + 1) / 40;
                Vec3 p0 = c.add(ax1.scale(Math.cos(a0) * r * 1.01)).add(ax2.scale(Math.sin(a0) * r * 1.01)), p1 = c.add(ax1.scale(Math.cos(a1) * r * 1.01)).add(ax2.scale(Math.sin(a1) * r * 1.01));
                RiftFx.box(vb, m, p0, p1, 0.05f, 0.55f * k, 0.1f * k, 0.9f * k, t * 0.2f);
            }
        }
    }

    private static Vec3 sph(Vec3 c, float r, int i, int j, int lat, int lon, float t) {
        double th = Math.PI * i / lat, ph = Math.PI * 2 * j / lon;
        double wob = 1 + 0.06 * Math.sin(ph * 3 + t * 0.9) * Math.sin(th * 4 + t * 0.7);
        return c.add(Math.sin(th) * Math.cos(ph) * r * wob, Math.cos(th) * r * wob, Math.sin(th) * Math.sin(ph) * r * wob);
    }

    private static void ring(VertexConsumer vc, Matrix4f m, double r, int n, double w, float[] c, float k, double rot, double y) {
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n + rot, a1 = Math.PI * 2 * (i + 1) / n + rot;
            RiftFx.strip(vc, m, 0.5 + Math.cos(a0) * r, 0.5 + Math.sin(a0) * r, 0.5 + Math.cos(a1) * r, 0.5 + Math.sin(a1) * r, 0.02 + y, w, c, k);
        }
    }

    /** Итог лежит на ядре: кольцо-таймер на полу и подпись над оружием. */
    private static void lying(RitualBlockEntity be, float wait, float t, PoseStack pose, MultiBufferSource buf) {
        Matrix4f m = pose.last().pose();
        VertexConsumer vl = buf.getBuffer(RenderType.eyes(LINE));
        float frac = wait / LIE;
        int n = 60, on = Math.round(n * frac);
        float pulse = 0.75f + 0.25f * (float) Math.sin(t * 0.2);
        for (int i = 0; i < n; i++) {
            double a0 = -Math.PI / 2 + Math.PI * 2 * i / n, a1 = a0 + Math.PI * 2 / n;
            boolean lit = i < on;
            float[] c = lit ? new float[]{1f, 0.78f, 0.3f} : new float[]{0.35f, 0.15f, 0.5f};
            RiftFx.strip(vl, m, 0.5 + Math.cos(a0) * 1.05, 0.5 + Math.sin(a0) * 1.05, 0.5 + Math.cos(a1) * 1.05, 0.5 + Math.sin(a1) * 1.05, 1.02, 0.07, c, lit ? pulse : 0.35f);
        }
        // подпись
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        String s = "☾ " + be.ownerName + " · " + (int) Math.ceil(wait / 20f) + " с";
        pose.pushPose();
        pose.translate(0.5, 2.55, 0.5);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.022f, -0.022f, 0.022f);
        font.drawInBatch(s, -font.width(s) / 2f, 0, 0xFFE0B0FF, false, pose.last().pose(), buf, Font.DisplayMode.SEE_THROUGH, 0x60000000, 0xF000F0);
        pose.popPose();
    }
}
