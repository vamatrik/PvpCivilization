package com.pvpciv.client;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Простые кат-сцены на клиенте.
 *  0.12: launch — тряска и белая вспышка (ракета), arrive — проявление из темноты.
 *  1.0: lift — затемнение (лифт станции), airlock — шлюз (тряска, затемнение), rift — взрыв червя и тьма (фаза 2),
 *       worm — появление Галактического червя: камера отъезжает (широкий FOV), имя во весь экран.
 */
public final class Cutscene {
    private Cutscene() {}
    private static String kind = "";
    private static long start;
    private static int len;

    public static void start(String k, int ticks) { kind = k; len = ticks; start = System.currentTimeMillis(); look = null; }

    /** 1.1: камера смотрит на взлетающую ракету (x,y,z — точка на ракете, t0/acc — закон подъёма). */
    private static double[] look;
    public static void start(com.google.gson.JsonObject j) {
        int skip = j.has("skip") ? j.get("skip").getAsInt() : 0;
        start(j.get("cut").getAsString(), j.get("len").getAsInt() - skip);
        if (j.has("x")) look = new double[]{j.get("x").getAsDouble(), j.get("y").getAsDouble(), j.get("z").getAsDouble(), j.get("t0").getAsDouble() - skip, j.get("acc").getAsDouble()};
    }

    private static float ticks() { return (System.currentTimeMillis() - start) / 50f; }

    public static void camera(ViewportEvent.ComputeCameraAngles e) {
        float t = ticks();
        if (kind.equals("launch") || kind.equals("airlock") || kind.equals("rift")) {
            if (t > len + 5) { kind = ""; look = null; return; }
            float amp = kind.equals("launch") ? (t < 55 ? 0.15f : Math.min(3.5f, (t - 55) / 14f)) : kind.equals("rift") ? Math.max(0, 3f - t / 12f) : 0.6f;
            if (look != null && kind.equals("launch")) {
                double tt = t - look[3];
                double ry = look[1] + (tt > 0 ? look[4] * tt * tt * 0.85 : 0);   // чуть отстаём — ракета «уходит» вверх по кадру
                var cam = e.getCamera().getPosition();
                double dx = look[0] - cam.x, dy = ry - cam.y, dz = look[2] - cam.z;
                double h = Math.sqrt(dx * dx + dz * dz);
                e.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90));
                e.setPitch((float) -Math.toDegrees(Math.atan2(dy, h)));
                amp = tt < -15 ? 0.05f : Math.min(0.9f, (float) (tt + 15) / 40f);
            }
            double s = System.nanoTime() / 1e7;
            e.setPitch(e.getPitch() + (float) Math.sin(s * 0.9) * amp);
            e.setYaw(e.getYaw() + (float) Math.cos(s * 1.3) * amp * 0.6f);
            e.setRoll(e.getRoll() + (float) Math.sin(s * 0.7) * amp * 0.5f);
        }
    }

    /** Отъезд камеры при появлении червя. */
    public static void fov(ViewportEvent.ComputeFov e) {
        if (!kind.equals("worm")) return;
        float t = ticks();
        if (t > len) return;
        float k = t < 15 ? t / 15f : t > len - 20 ? (len - t) / 20f : 1f;
        e.setFOV(e.getFOV() + 35 * k);
    }

    public static void overlay(GuiGraphics g, int w, int h) {
        if (kind.isEmpty()) return;
        float t = ticks();
        int a = 0, rgb = 0xFFFFFF;
        switch (kind) {
            case "launch" -> {
                if (t > len - 18) a = (int) Math.min(255, (t - (len - 18)) / 18f * 255);
                if (t > len + 5) { kind = ""; return; }
            }
            case "arrive" -> {
                rgb = 0x000000;
                a = (int) Math.max(0, 255 - t / 30f * 255);
                if (t > 30) { kind = ""; return; }
            }
            case "lift", "airlock" -> {
                rgb = 0x000000;
                a = (int) Math.min(255, t / Math.max(1f, len - 15) * 255);
                if (t > len + 10) { kind = ""; return; }
            }
            case "rift" -> {
                if (t < 6) { rgb = 0xFFFFFF; a = (int) (255 * (1 - t / 6f)); }
                else { rgb = 0x000000; a = (int) Math.min(255, (t - 6) / 20f * 255); }
                if (t > len + 10) { kind = ""; return; }
            }
            case "worm" -> {
                if (t > len) { kind = ""; return; }
                float fa = t < 10 ? t / 10f : t > len - 15 ? (len - t) / 15f : 1f;
                g.fill(0, 0, w, h / 7, ((int) (255 * fa) << 24));
                g.fill(0, h - h / 7, w, h, ((int) (255 * fa) << 24));
                var font = net.minecraft.client.Minecraft.getInstance().font;
                String name = "ГАЛАКТИЧЕСКИЙ ЧЕРВЬ";
                float sc = Math.min(4f, w / (float) font.width(name) * 0.8f);
                g.pose().pushPose();
                g.pose().translate(w / 2f, h / 2f - 20, 0);
                g.pose().scale(sc, sc, 1);
                int col = ((int) (255 * fa) << 24) | 0xFF60E0;
                g.drawString(font, name, -font.width(name) / 2, 0, col, true);
                g.pose().popPose();
                String sub = "Пожиратель астероидов · ур. 31";
                g.drawCenteredString(font, sub, w / 2, (int) (h / 2f + 20), ((int) (255 * fa) << 24) | 0xC0B0E0);
                return;
            }
            default -> {}
        }
        if (a > 0) g.fill(0, 0, w, h, (a << 24) | rgb);
    }
}
