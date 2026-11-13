package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pvpciv.entity.FxEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Неоновые эффекты одним куском геометрии: аддитивный свет (как у молнии), градиенты по радиусу и высоте. */
public class FxRenderer extends EntityRenderer<FxEntity> {
    public FxRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override public ResourceLocation getTextureLocation(FxEntity e) { return ResourceLocation.withDefaultNamespace("textures/misc/white.png"); }

    private static final int SEG = 64;

    @Override
    public void render(FxEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt, life = e.life(), p = Mth.clamp(age / life, 0, 1);
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        int c1 = e.color(), c2 = e.color2();
        float r = e.radius();
        float a0 = -Mth.PI, a1 = Mth.PI;
        if (e.half() > 0) { float dir = (float) Math.atan2(e.vec().z, e.vec().x); a0 = dir - e.half(); a1 = dir + e.half(); }
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.6f);
        switch (e.kind()) {
            case FxEntity.ZONE -> {
                float y = 0.06f;
                // мягкая подложка всей зоны
                band(vc, m, a0, a1, 0, r, y, c2, 0.10f, c1, 0.26f);
                // заполнение до удара: градиент к растущему краю
                float rr = r * p;
                band(vc, m, a0, a1, 0, rr, y + 0.005f, c2, 0.22f, c1, 0.65f * pulse);
                band(vc, m, a0, a1, Math.max(0, rr - 0.18f), rr, y + 0.01f, c1, 0.2f, c2, 0.95f);
                // неоновый контур
                band(vc, m, a0, a1, r - 0.14f, r, y + 0.012f, c1, 0.95f * pulse, c1, 0.95f * pulse);
                band(vc, m, a0, a1, r, r + 0.25f, y + 0.012f, c1, 0.35f * pulse, c1, 0f);
                if (e.half() > 0) for (float a : new float[]{a0, a1}) spoke(vc, m, a, r, y + 0.013f, c1, 0.9f * pulse);
            }
            case FxEntity.WAVE -> {
                float rr = 1 + (r - 1) * p, fade = 1 - p * p;
                wall(vc, m, rr, 0.02f, 1.1f, c1, 0.85f * fade, c2, 0f);
                band(vc, m, a0, a1, Math.max(0, rr - 0.6f), rr, 0.05f, c2, 0f, c1, 0.6f * fade);
                band(vc, m, a0, a1, rr, rr + 0.35f, 0.05f, c1, 0.7f * fade, c2, 0f);
            }
            case FxEntity.BURST -> {
                float rr = r * easeOut(p), fade = 1 - p;
                band(vc, m, a0, a1, rr * 0.6f, rr, 0.08f, c2, 0f, c1, 0.9f * fade);
                band(vc, m, a0, a1, rr, rr * 1.15f + 0.1f, 0.08f, c1, 0.9f * fade, c2, 0f);
                column(vc, m, 0.25f + r * 0.12f * (1 - p), 4.5f * (1 - p * 0.6f), c1, 0.9f * fade, c2, 0f);
                wall(vc, m, rr * 0.8f, 0.05f, 0.9f * fade, c2, 0.5f * fade, c1, 0f);
            }
            case FxEntity.PILLAR -> {
                float fade = Mth.sin(p * Mth.PI);
                column(vc, m, r * (1.05f - 0.4f * p), 7f, c1, 0.8f * fade, c2, 0f);
                column(vc, m, r * 0.45f, 9f, c2, 0.9f * fade, c1, 0f);
                band(vc, m, a0, a1, 0, r * 1.6f, 0.06f, c1, 0.5f * fade, c2, 0f);
            }
            case FxEntity.BEAM -> {
                float fade = Mth.sin(p * Mth.PI);
                var v = e.vec();
                beam(vc, m, v.x, v.y, v.z, 0.06f + 0.05f * pulse, c1, 0.9f * fade, c2, 0.9f * fade);
            }
            default -> {}
        }
        super.render(e, yaw, pt, ps, buf, light);
    }

    private static float easeOut(float t) { return 1 - (1 - t) * (1 - t); }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, int c, float a) {
        vc.addVertex(m, x, y, z).setColor(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, Mth.clamp(a, 0, 1));
    }

    /** Плоское кольцо (диск при rIn=0) с градиентом от внутреннего края к внешнему; рисуется с обеих сторон. */
    private static void band(VertexConsumer vc, Matrix4f m, float a0, float a1, float rIn, float rOut, float y, int cIn, float aIn, int cOut, float aOut) {
        if (rOut <= rIn) return;
        int n = Math.max(6, (int) (SEG * (a1 - a0) / (2 * Mth.PI)));
        for (int i = 0; i < n; i++) {
            float t0 = a0 + (a1 - a0) * i / n, t1 = a0 + (a1 - a0) * (i + 1) / n;
            float x0 = Mth.cos(t0), z0 = Mth.sin(t0), x1 = Mth.cos(t1), z1 = Mth.sin(t1);
            v(vc, m, x0 * rIn, y, z0 * rIn, cIn, aIn); v(vc, m, x0 * rOut, y, z0 * rOut, cOut, aOut);
            v(vc, m, x1 * rOut, y, z1 * rOut, cOut, aOut); v(vc, m, x1 * rIn, y, z1 * rIn, cIn, aIn);
            v(vc, m, x1 * rIn, y, z1 * rIn, cIn, aIn); v(vc, m, x1 * rOut, y, z1 * rOut, cOut, aOut);
            v(vc, m, x0 * rOut, y, z0 * rOut, cOut, aOut); v(vc, m, x0 * rIn, y, z0 * rIn, cIn, aIn);
        }
    }

    /** Вертикальная светящаяся стенка по окружности: снизу ярко, кверху гаснет. */
    private static void wall(VertexConsumer vc, Matrix4f m, float r, float y0, float h, int cB, float aB, int cT, float aT) {
        for (int i = 0; i < SEG; i++) {
            float t0 = 2 * Mth.PI * i / SEG, t1 = 2 * Mth.PI * (i + 1) / SEG;
            float x0 = Mth.cos(t0) * r, z0 = Mth.sin(t0) * r, x1 = Mth.cos(t1) * r, z1 = Mth.sin(t1) * r;
            v(vc, m, x0, y0, z0, cB, aB); v(vc, m, x0, y0 + h, z0, cT, aT); v(vc, m, x1, y0 + h, z1, cT, aT); v(vc, m, x1, y0, z1, cB, aB);
            v(vc, m, x1, y0, z1, cB, aB); v(vc, m, x1, y0 + h, z1, cT, aT); v(vc, m, x0, y0 + h, z0, cT, aT); v(vc, m, x0, y0, z0, cB, aB);
        }
    }

    private static void column(VertexConsumer vc, Matrix4f m, float r, float h, int cB, float aB, int cT, float aT) {
        for (int i = 0; i < 16; i++) {
            float t0 = 2 * Mth.PI * i / 16, t1 = 2 * Mth.PI * (i + 1) / 16;
            float x0 = Mth.cos(t0) * r, z0 = Mth.sin(t0) * r, x1 = Mth.cos(t1) * r, z1 = Mth.sin(t1) * r;
            v(vc, m, x0, 0, z0, cB, aB); v(vc, m, x0, h, z0, cT, aT); v(vc, m, x1, h, z1, cT, aT); v(vc, m, x1, 0, z1, cB, aB);
            v(vc, m, x1, 0, z1, cB, aB); v(vc, m, x1, h, z1, cT, aT); v(vc, m, x0, h, z0, cT, aT); v(vc, m, x0, 0, z0, cB, aB);
        }
    }

    private static void spoke(VertexConsumer vc, Matrix4f m, float a, float r, float y, int c, float al) {
        float dx = Mth.cos(a), dz = Mth.sin(a), nx = -dz * 0.07f, nz = dx * 0.07f;
        v(vc, m, nx, y, nz, c, al); v(vc, m, dx * r + nx, y, dz * r + nz, c, al); v(vc, m, dx * r - nx, y, dz * r - nz, c, al); v(vc, m, -nx, y, -nz, c, al);
        v(vc, m, -nx, y, -nz, c, al); v(vc, m, dx * r - nx, y, dz * r - nz, c, al); v(vc, m, dx * r + nx, y, dz * r + nz, c, al); v(vc, m, nx, y, nz, c, al);
    }

    /** Луч крестом из двух лент — виден с любой стороны. */
    private static void beam(VertexConsumer vc, Matrix4f m, float tx, float ty, float tz, float w, int c1, float a1, int c2, float a2) {
        float[][] sides = {{w, 0, 0}, {0, w, 0}, {0, 0, w}};
        for (float[] s : sides) {
            v(vc, m, -s[0], -s[1], -s[2], c1, a1); v(vc, m, s[0], s[1], s[2], c1, a1); v(vc, m, tx + s[0], ty + s[1], tz + s[2], c2, a2); v(vc, m, tx - s[0], ty - s[1], tz - s[2], c2, a2);
            v(vc, m, tx - s[0], ty - s[1], tz - s[2], c2, a2); v(vc, m, tx + s[0], ty + s[1], tz + s[2], c2, a2); v(vc, m, s[0], s[1], s[2], c1, a1); v(vc, m, -s[0], -s[1], -s[2], c1, a1);
        }
    }
}
