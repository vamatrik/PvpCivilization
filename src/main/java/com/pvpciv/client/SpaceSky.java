package com.pvpciv.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.pvpciv.PvpCiv;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 0.12 — небо этажа 5 (тип измерения pvpciv:space): чёрный космос, плотное звёздное поле разных цветов,
 * туманность, далёкое солнце и огромная планета внизу-сбоку (мир Шпиля). Облаков и тумана нет.
 */
public class SpaceSky extends DimensionSpecialEffects {
    public static final ResourceLocation ID = PvpCiv.id("space");
    private static final ResourceLocation PLANET = PvpCiv.id("textures/environment/planet.png"),
            NEBULA = PvpCiv.id("textures/environment/nebula.png"), SUN = PvpCiv.id("textures/environment/space_sun.png");
    private VertexBuffer stars;

    public SpaceSky() { super(Float.NaN, false, SkyType.NONE, false, false); }

    @Override public Vec3 getBrightnessDependentFogColor(Vec3 c, float b) { return Vec3.ZERO; }
    @Override public boolean isFoggyAt(int x, int y) { return false; }
    @Override public boolean renderClouds(ClientLevel l, int t, float pt, PoseStack ps, double x, double y, double z, Matrix4f mv, Matrix4f proj) { return true; }
    @Override public boolean renderSnowAndRain(ClientLevel l, int t, float pt, net.minecraft.client.renderer.LightTexture lt, double x, double y, double z) { return true; }

    private void buildStars() {
        stars = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        RandomSource r = RandomSource.create(5050L);
        for (int i = 0; i < 4500; i++) {
            double x = r.nextFloat() * 2 - 1, y = r.nextFloat() * 2 - 1, z = r.nextFloat() * 2 - 1;
            double len = x * x + y * y + z * z;
            if (len < 0.01 || len > 1) continue;
            len = 1 / Math.sqrt(len);
            x *= len; y *= len; z *= len;
            double size = 0.12 + r.nextFloat() * 0.18 + (r.nextFloat() < 0.03 ? 0.25 : 0);
            float br = 0.45f + r.nextFloat() * 0.55f;
            int kind = r.nextInt(10);   // цвет звезды: белые, голубые, жёлтые, красноватые
            float cr = kind < 5 ? br : kind < 7 ? br * 0.75f : kind < 9 ? br : br;
            float cg = kind < 5 ? br : kind < 7 ? br * 0.85f : kind < 9 ? br * 0.9f : br * 0.6f;
            float cb = kind < 5 ? br : kind < 7 ? br : kind < 9 ? br * 0.65f : br * 0.5f;
            double px = x * 100, py = y * 100, pz = z * 100;
            double yaw = Math.atan2(x, z), sy = Math.sin(yaw), cy = Math.cos(yaw);
            double pitch = Math.atan2(Math.sqrt(x * x + z * z), y), sp = Math.sin(pitch), cp = Math.cos(pitch);
            double rot = r.nextDouble() * Math.PI * 2, sr = Math.sin(rot), cr2 = Math.cos(rot);
            for (int k = 0; k < 4; k++) {
                double a = ((k & 2) - 1) * size, c = ((k + 1 & 2) - 1) * size;
                double d1 = a * cr2 - c * sr, d2 = c * cr2 + a * sr;
                double vy = d1 * sp, ox = -d1 * cp;
                double vx = ox * sy - d2 * cy, vz = d2 * sy + ox * cy;
                b.addVertex((float) (px + vx), (float) (py + vy), (float) (pz + vz)).setColor(cr, cg, cb, 1f);
            }
        }
        stars.bind();
        stars.upload(b.buildOrThrow());
        VertexBuffer.unbind();
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float pt, Matrix4f modelView, Camera cam, Matrix4f proj, boolean foggy, Runnable setupFog) {
        if (stars == null) buildStars();
        PoseStack ps = new PoseStack();
        ps.mulPose(modelView);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        // чёрный фон-коробка (перекрывает цвет неба биома)
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bg = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = ps.last().pose();
        float s = 150;
        float[][] f = {{-s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s}, {-s, s, -s, -s, s, s, s, s, s, s, s, -s}, {-s, -s, -s, -s, s, -s, s, s, -s, s, -s, -s},
                {-s, -s, s, s, -s, s, s, s, s, -s, s, s}, {-s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s}, {s, -s, -s, s, s, -s, s, s, s, s, -s, s}};
        for (float[] q : f) for (int i = 0; i < 4; i++) bg.addVertex(m, q[i * 3], q[i * 3 + 1], q[i * 3 + 2]).setColor(0.008f, 0.008f, 0.02f, 1f);
        BufferUploader.drawWithShader(bg.buildOrThrow());
        // звёзды — медленно плывут
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        float t = (level.getGameTime() + pt) / 20f;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(t * 0.15f));
        ps.mulPose(Axis.XP.rotationDegrees(23));
        stars.bind();
        stars.drawWithShader(ps.last().pose(), proj, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
        ps.popPose();
        // туманность (аддитивно)
        quad(ps, NEBULA, 0, 35, 0, 95, 70, 0.9f);
        quad(ps, NEBULA, 130, -10, 200, 90, 55, 0.6f);
        // солнце
        quad(ps, SUN, -60, 40, 0, 100, 12, 1f);
        // планета внизу (обычное смешивание — закрывает звёзды)
        RenderSystem.defaultBlendFunc();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(t * 0.02f));
        quad(ps, PLANET, 0, -62, 0, 95, 70, 1f);
        ps.popPose();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        return true;
    }

    /** Квадрат-спрайт на небесной сфере: yaw/pitch направления (градусы), dist, size, прозрачность. */
    private static void quad(PoseStack ps, ResourceLocation tex, float yaw, float pitch, float roll, float dist, float size, float alpha) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(roll));
        Matrix4f m = ps.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(m, -size, -size, dist).setUv(0, 1);
        b.addVertex(m, size, -size, dist).setUv(1, 1);
        b.addVertex(m, size, size, dist).setUv(1, 0);
        b.addVertex(m, -size, size, dist).setUv(0, 0);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        ps.popPose();
    }
}
