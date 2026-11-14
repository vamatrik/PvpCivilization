package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

/**
 * 0.10 — полоска здоровья над мобами с уровнем, как в RPG: при уроне полоска плавно «съезжает»,
 * а потерянная часть сначала светится и через мгновение тает. Билборд, ровные пиксельные края, рамка.
 */
public final class MobBars {
    private MobBars() {}

    /** id сущности → {показанное HP (доля), шлейф (доля), до какого времени держать шлейф (мс), последний раз видели (мс), последний урон (мс)}. */
    private static final java.util.Map<Integer, double[]> STATE = new java.util.HashMap<>();
    private static long lastPrune;

    /** Кадр отрисовки: какие сущности уже получили полоску/цифры через RenderLivingEvent (остальных — GeckoLib — дорисуем в мире). */
    private static final java.util.Set<Integer> DRAWN = new java.util.HashSet<>();

    public static void onRender(RenderLivingEvent.Post<?, ?> e) {
        DRAWN.add(e.getEntity().getId());
        draw(e.getEntity(), e.getPoseStack(), e.getMultiBufferSource());
    }

    /** 0.10.5: для моделей GeckoLib (боссы, звери) RenderLivingEvent не приходит — рисуем после всех сущностей сами. */
    public static void onLevel(net.neoforged.neoforge.client.event.RenderLevelStageEvent e) {
        var stage = e.getStage();
        if (stage == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY) { DRAWN.clear(); return; }
        if (stage != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var cam = e.getCamera().getPosition();
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = e.getPoseStack();
        for (var en : mc.level.entitiesForRendering()) {
            if (!(en instanceof LivingEntity le) || DRAWN.contains(le.getId()) || le == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            if (le.distanceToSqr(mc.player) > 30 * 30) continue;
            boolean barred = le instanceof Mob && le.getCustomName() != null && le.getCustomName().getString().startsWith("[");
            if (!barred && !Nums.has(le.getId())) continue;
            double x = net.minecraft.util.Mth.lerp(pt, le.xOld, le.getX()) - cam.x, y = net.minecraft.util.Mth.lerp(pt, le.yOld, le.getY()) - cam.y,
                    z = net.minecraft.util.Mth.lerp(pt, le.zOld, le.getZ()) - cam.z;
            ps.pushPose();
            ps.translate(x, y, z);
            draw(le, ps, buf);
            ps.popPose();
        }
        buf.endBatch();
    }

    public static void draw(LivingEntity le, PoseStack ePose, MultiBufferSource eBuf) {
        var name = le.getCustomName();
        String s = name == null ? "" : name.getString();
        if (!(le instanceof Mob m) || !m.isAlive() || m.isInvisible() || !s.startsWith("[")) {   // боссы (☠), игроки — только цифры
            Nums.standalone(null, le, ePose, eBuf);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        double d2 = m.distanceToSqr(mc.player);
        if (d2 > 26 * 26) { Nums.standalone(null, le, ePose, eBuf); return; }
        long now = System.currentTimeMillis();
        float frac = Math.max(0f, Math.min(1f, m.getHealth() / Math.max(1f, m.getMaxHealth())));
        double[] st = STATE.computeIfAbsent(m.getId(), k -> new double[]{frac, frac, 0, now, 0});
        double dt = Math.min(0.1, (now - st[3]) / 1000.0);
        st[3] = now;
        if (frac < st[0] - 1e-4) { if (st[1] < st[0]) st[1] = st[0]; st[2] = now + 450; st[4] = now; }
        // показанное HP догоняет реальное быстро и плавно
        st[0] += (frac - st[0]) * Math.min(1, dt * 18);
        if (Math.abs(st[0] - frac) < 1e-3) st[0] = frac;
        // шлейф: держится, потом тает
        if (st[1] < st[0]) st[1] = st[0];
        else if (now > st[2]) st[1] = Math.max(st[0], st[1] - dt * 0.9);
        boolean recent = now - st[4] < 6000;
        if (frac >= 0.999f && !recent && d2 > 8 * 8 && !Nums.has(m.getId())) return;
        prune(now);

        boolean elite = m.isCurrentlyGlowing();
        PoseStack ps = ePose;
        ps.pushPose();
        ps.translate(0, m.getBbHeight() + 0.8, 0);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        float sc = 0.025f;
        ps.scale(sc, -sc, sc);
        Matrix4f mat = ps.last().pose();
        MultiBufferSource buf = eBuf;
        VertexConsumer vc = buf.getBuffer(RenderType.textBackground());
        int light = 0xF000F0;
        Font font = mc.font;
        String lvl = s.substring(1, Math.max(1, s.indexOf(']')));
        float ls = 0.75f, LW = Math.max(9f, font.width(lvl) * ls + 4f);
        float W = elite ? 46 : 40, H = 7f;
        float total = LW + 1.2f + W, xL = -total / 2, x0 = xL + LW + 1.2f, y0 = 0;
        int gold = elite ? 0xFFFFC860 : 0xFFD9A441, goldD = elite ? 0xFF9A6A20 : 0xFF7A5420;
        // одна рама на всё: тёмный контур → золотой кант (светлый верх, тёмный низ) → утопленный фон
        quad(vc, mat, xL - 2.2f, y0 - 2.2f, x0 + W + 2.2f, y0 + H + 2.2f, 0.000f, 0xF007060B, light);
        quad(vc, mat, xL - 1.4f, y0 - 1.4f, x0 + W + 1.4f, y0 + H + 1.4f, 0.005f, gold, light);
        quad(vc, mat, xL - 1.4f, y0 + H * 0.5f, x0 + W + 1.4f, y0 + H + 1.4f, 0.006f, goldD, light);
        quad(vc, mat, xL - 0.5f, y0 - 0.5f, x0 + W + 0.5f, y0 + H + 0.5f, 0.008f, 0xFF0D0A14, light);
        // ячейка уровня и перегородка
        quadV(vc, mat, xL, y0, xL + LW, y0 + H, 0.010f, 0xFF2A2036, 0xFF16111E, light);
        quad(vc, mat, xL + LW, y0 - 1.4f, xL + LW + 1.2f, y0 + H + 1.4f, 0.011f, gold, light);
        // шлейф (светлый), потом основная полоса
        float tw = (float) (W * st[1]), fw = (float) (W * st[0]);
        if (tw > fw + 0.05f) quad(vc, mat, x0 + fw, y0, x0 + tw, y0 + H, 0.012f, 0xF0FFE6B0, light);
        int top = elite ? 0xFFFFB347 : 0xFFF0475A, bot = elite ? 0xFFC0701A : 0xFF8A1828;
        if (fw > 0.05f) {
            quadV(vc, mat, x0, y0, x0 + fw, y0 + H, 0.015f, top, bot, light);
            quad(vc, mat, x0, y0 + 0.5f, x0 + fw, y0 + 1.3f, 0.018f, 0x55FFFFFF, light);   // блик
            if (fw < W - 0.3f) quad(vc, mat, x0 + fw - 0.5f, y0, x0 + fw, y0 + H, 0.019f, 0x90FFFFFF, light);   // светлый край
        }
        // уровень — жирно, одним цветом
        ps.pushPose();
        ps.translate(xL + LW / 2 - font.width(lvl) * ls / 2, y0 + H / 2 - 4f * ls + 0.5f, 0.03f);
        ps.scale(ls, ls, ls);
        font.drawInBatch(net.minecraft.network.chat.Component.literal(lvl).withStyle(net.minecraft.ChatFormatting.BOLD), 0, 0, elite ? 0xFFFFC860 : 0xFFFFE08A, true,
                ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
        ps.popPose();
        // HP прямо на полоске
        String hp = com.pvpciv.combat.Balance.fmt(Math.ceil(m.getHealth())) + " / " + com.pvpciv.combat.Balance.fmt(m.getMaxHealth());
        float ts = 0.55f;
        ps.pushPose();
        ps.translate(x0 + W / 2 - font.width(hp) * ts / 2, y0 + H / 2 - 4f * ts + 0.4f, 0.03f);
        ps.scale(ts, ts, ts);
        font.drawInBatch(hp, 0, 0, 0xFFFFFFFF, true, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
        ps.popPose();
        Nums.draw(ps, buf, font, m.getId(), y0 - 4f, now, light);
        ps.popPose();
    }

    private static void quadV(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float z, int top, int bot, int light) {
        vc.addVertex(m, x1, y1, z).setColor(top).setLight(light);
        vc.addVertex(m, x1, y2, z).setColor(bot).setLight(light);
        vc.addVertex(m, x2, y2, z).setColor(bot).setLight(light);
        vc.addVertex(m, x2, y1, z).setColor(top).setLight(light);
    }

    /** Ромбик-самоцвет: светлая верхняя половина, тёмная нижняя, контур. */
    private static void gem(VertexConsumer vc, Matrix4f m, float cx, float cy, float r, float z, int hi, int lo, int light) {
        float o = r + 0.8f;
        vc.addVertex(m, cx, cy - o, z).setColor(0xFF07060B).setLight(light);
        vc.addVertex(m, cx - o, cy, z).setColor(0xFF07060B).setLight(light);
        vc.addVertex(m, cx, cy + o, z).setColor(0xFF07060B).setLight(light);
        vc.addVertex(m, cx + o, cy, z).setColor(0xFF07060B).setLight(light);
        vc.addVertex(m, cx, cy - r, z + 0.002f).setColor(hi).setLight(light);
        vc.addVertex(m, cx - r, cy, z + 0.002f).setColor(hi).setLight(light);
        vc.addVertex(m, cx, cy + r, z + 0.002f).setColor(lo).setLight(light);
        vc.addVertex(m, cx + r, cy, z + 0.002f).setColor(hi).setLight(light);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float z, int argb, int light) {
        vc.addVertex(m, x1, y1, z).setColor(argb).setLight(light);
        vc.addVertex(m, x1, y2, z).setColor(argb).setLight(light);
        vc.addVertex(m, x2, y2, z).setColor(argb).setLight(light);
        vc.addVertex(m, x2, y1, z).setColor(argb).setLight(light);
    }

    private static void prune(long now) {
        if (now - lastPrune < 5000) return;
        lastPrune = now;
        STATE.values().removeIf(v -> now - v[3] > 10000);
    }
}
