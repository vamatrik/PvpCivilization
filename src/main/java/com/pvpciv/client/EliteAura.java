package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

/**
 * 1.2: 3D-аура элитных мобов вместо ванильного свечения: золотое кольцо у ног (пульсирует),
 * второе — пунктирное, вращается навстречу; три ромба-искры кружат на уровне груди.
 */
public final class EliteAura {
    private EliteAura() {}

    /** Элитка: моб мода с флагом свечения (сервер ставит его элиткам). */
    public static boolean isElite(Entity e) {
        return e instanceof Mob && !(e instanceof com.pvpciv.entity.boss.CivBoss) && e.isCurrentlyGlowing()
                && e.getType().builtInRegistryHolder().key().location().getNamespace().equals("pvpciv");
    }

    public static void onRender(RenderLivingEvent.Post<?, ?> e) {
        LivingEntity en = e.getEntity();
        if (!isElite(en) || en.isInvisible()) return;
        float t = (en.tickCount + e.getPartialTick()) / 20f;
        PoseStack ps = e.getPoseStack();
        VertexConsumer vc = e.getMultiBufferSource().getBuffer(RenderType.lightning());
        float w = en.getBbWidth(), h = en.getBbHeight();
        float r0 = w * 0.55f + 0.35f;
        float pulse = 0.55f + 0.25f * (float) Math.sin(t * 3);
        ps.pushPose();
        ps.translate(0, 0.04, 0);
        ps.mulPose(Axis.YP.rotation(t * 0.8f));
        ring(ps.last().pose(), vc, r0, r0 + 0.10f, 40, 1f, 0.78f, 0.25f, pulse, false);
        ps.popPose();
        ps.pushPose();
        ps.translate(0, 0.08, 0);
        ps.mulPose(Axis.YP.rotation(-t * 1.6f));
        ring(ps.last().pose(), vc, r0 + 0.18f, r0 + 0.24f, 28, 1f, 0.55f, 0.15f, pulse * 0.8f, true);
        ps.popPose();
        // ромбы-искры
        for (int i = 0; i < 3; i++) {
            double a = t * 1.9 + i * Math.PI * 2 / 3;
            float y = h * 0.55f + 0.15f * (float) Math.sin(t * 2.5 + i);
            ps.pushPose();
            ps.translate(Math.cos(a) * (r0 + 0.1), y, Math.sin(a) * (r0 + 0.1));
            ps.mulPose(Axis.YP.rotation(t * 4 + i));
            diamond(ps.last().pose(), vc, 0.09f, 1f, 0.85f, 0.35f, 0.85f);
            ps.popPose();
        }
    }

    private static void ring(Matrix4f m, VertexConsumer vc, float r0, float r1, int n, float r, float g, float b, float a, boolean dashed) {
        for (int i = 0; i < n; i++) {
            if (dashed && i % 2 == 1) continue;
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            // верх и низ (видно с обеих сторон)
            quad(vc, m, r0 * c0, r0 * s0, r1 * c0, r1 * s0, r1 * c1, r1 * s1, r0 * c1, r0 * s1, r, g, b, a);
            quad(vc, m, r0 * c1, r0 * s1, r1 * c1, r1 * s1, r1 * c0, r1 * s0, r0 * c0, r0 * s0, r, g, b, a);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float z0, float x1, float z1, float x2, float z2, float x3, float z3, float r, float g, float b, float a) {
        vc.addVertex(m, x0, 0, z0).setColor(r, g, b, a);
        vc.addVertex(m, x1, 0, z1).setColor(r, g, b, a * 0.2f);
        vc.addVertex(m, x2, 0, z2).setColor(r, g, b, a * 0.2f);
        vc.addVertex(m, x3, 0, z3).setColor(r, g, b, a);
    }

    private static void diamond(Matrix4f m, VertexConsumer vc, float s, float r, float g, float b, float a) {
        float[][] eq = {{s, 0, 0}, {0, 0, s}, {-s, 0, 0}, {0, 0, -s}};
        for (int i = 0; i < 4; i++) {
            float[] p = eq[i], q = eq[(i + 1) % 4];
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                vc.addVertex(m, 0, sgn * s * 1.6f, 0).setColor(r, g, b, a);
                vc.addVertex(m, p[0], 0, p[2]).setColor(r, g * 0.8f, b, a * 0.7f);
                vc.addVertex(m, q[0], 0, q[2]).setColor(r, g * 0.8f, b, a * 0.7f);
                vc.addVertex(m, 0, sgn * s * 1.6f, 0).setColor(r, g, b, a);
            }
        }
    }
}
