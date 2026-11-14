package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** 0.10 — всплывающие цифры урона (клиент): вылетают над полоской здоровья, «выстреливают» крупнее и тают вверх. */
public final class Nums {
    private Nums() {}
    private record Num(String text, int color, boolean bold, float scale, long born, float dx) {}
    private static final java.util.Map<Integer, java.util.List<Num>> BY = new java.util.HashMap<>();
    private static final long LIFE = 950;

    public static void add(int entity, String text, int color, boolean bold, float scale) {
        var l = BY.computeIfAbsent(entity, k -> new java.util.ArrayList<>());
        if (l.size() > 8) l.remove(0);
        float dx = (float) ((Math.random() - 0.5) * 22);
        l.add(new Num(text, color, bold, scale, System.currentTimeMillis(), dx));
    }

    public static boolean has(int entity) { var l = BY.get(entity); return l != null && !l.isEmpty(); }

    /** Рисует цифры в уже повёрнутой к камере системе полоски (единицы — пиксели полоски, y вниз). baseY — верх полоски. */
    public static void draw(PoseStack ps, MultiBufferSource buf, Font font, int entity, float baseY, long now, int light) {
        var l = BY.get(entity);
        if (l == null) return;
        l.removeIf(n -> now - n.born > LIFE);
        if (l.isEmpty()) { BY.remove(entity); return; }
        for (Num n : l) {
            float t = (now - n.born) / (float) LIFE;
            float rise = 1 - (1 - t) * (1 - t);
            float pop = t < 0.12f ? 1.5f - 0.5f * (t / 0.12f) : 1f;
            float a = t < 0.65f ? 1f : 1f - (t - 0.65f) / 0.35f;
            int alpha = Math.max(8, (int) (255 * a));
            float sc = 0.9f * n.scale * pop;
            Component c = Component.literal(n.text).withStyle(Style.EMPTY.withBold(n.bold));
            ps.pushPose();
            ps.translate(n.dx * rise * 0.6f, baseY - 6 - 22 * rise, 0.06f);
            ps.scale(sc, sc, sc);
            font.drawInBatch(c, -font.width(c) / 2f, -8, alpha << 24 | (n.color & 0xFFFFFF), true, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
            ps.popPose();
        }
    }

    /** Для сущностей без полоски (игроки, боссы): свой билборд над головой. */
    public static void standalone(net.minecraft.client.renderer.entity.EntityRenderer<?> r, net.minecraft.world.entity.LivingEntity e, PoseStack ps, MultiBufferSource buf) {
        if (!has(e.getId())) return;
        Minecraft mc = Minecraft.getInstance();
        ps.pushPose();
        ps.translate(0, e.getBbHeight() + 0.5, 0);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(0.025f, -0.025f, 0.025f);
        draw(ps, buf, mc.font, e.getId(), 0, System.currentTimeMillis(), 0xF000F0);
        ps.popPose();
    }
}
