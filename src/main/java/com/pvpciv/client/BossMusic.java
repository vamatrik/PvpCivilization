package com.pvpciv.client;

import com.google.gson.JsonObject;
import com.pvpciv.PvpCiv;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 1.0.1 — музыка босс-файтов. Сервер шлёт PhoneData 24 {"m": трек, "t": секунды от начала боя} тем, кто на арене;
 * трек начинается с того же места у всех (общий цикл), играет по кругу, ванильная музыка молчит.
 * Ушёл с арены (или 6 с без данных) — плавное затухание.
 */
public final class BossMusic {
    private BossMusic() {}
    /** Длительность треков, с (для общего цикла). */
    private static final Map<String, Double> LEN = Map.ofEntries(
            Map.entry("brute", 209.6), Map.entry("keeper", 183.7), Map.entry("don", 161.1), Map.entry("rat_king", 182.8),
            Map.entry("kappa", 168.5), Map.entry("kumiho", 267.6), Map.entry("noel", 258.8), Map.entry("luna", 224.2),
            Map.entry("diplomat", 230.0), Map.entry("worm", 288.4), Map.entry("demon", 192.8));

    private static String cur = "";
    private static Track inst;
    private static long last;

    public static boolean playing() { return inst != null && !inst.isStopped(); }

    public static void sync(JsonObject j) {
        String m = j.get("m").getAsString();
        last = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (m.isEmpty()) { stop(); return; }
        if (m.equals(cur) && inst != null && !inst.isStopped() && !inst.fading) return;
        stop();
        double t = j.get("t").getAsDouble();
        double len = LEN.getOrDefault(m, 200.0);
        cur = m;
        inst = new Track(m, t % len);
        mc.getMusicManager().stopPlaying();
        mc.getSoundManager().play(inst);
        com.mojang.logging.LogUtils.getLogger().info("[PvpCiv] музыка босса: {} с {} с", m, String.format(java.util.Locale.ROOT, "%.1f", t % len));
    }

    public static void tick() {
        ArenaFx.tick();
        if (inst != null && System.currentTimeMillis() - last > 6000) stop();
        if (playing()) Minecraft.getInstance().getMusicManager().stopPlaying();
    }

    private static void stop() {
        if (inst != null) inst.fading = true;
        cur = "";
    }

    static final class Track extends AbstractTickableSoundInstance {
        final double offset;
        boolean fading;
        float vol = 0.05f;

        Track(String id, double offset) {
            super(SoundEvent.createVariableRangeEvent(PvpCiv.id("music." + id)), SoundSource.RECORDS, RandomSource.create());   // 1.0.1: ползунок «Музыкальные блоки» — ванильную музыку часто выключают
            this.offset = offset;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = vol;
        }

        @Override
        public void tick() {
            if (isStopped()) return;
            if (fading) { vol -= 0.025f; if (vol <= 0) { stop(); return; } }
            else if (vol < 1f) vol = Math.min(1f, vol + 0.05f);
            volume = vol;
        }

        @Override public boolean canStartSilent() { return true; }

        /** Пропускаем начало потока до общего места цикла. */
        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary lib, Sound sound, boolean loop) {
            return lib.getStream(sound.getPath(), loop).thenApply(s -> {
                try {
                    var f = s.getFormat();
                    long bytes = (long) (offset * f.getSampleRate()) * f.getChannels() * 2L;
                    while (bytes > 0) {
                        ByteBuffer b = s.read((int) Math.min(bytes, 65536));
                        if (b == null || !b.hasRemaining()) break;
                        bytes -= b.remaining();
                    }
                } catch (Exception ex) { com.mojang.logging.LogUtils.getLogger().error("[PvpCiv] music stream", ex); }
                return s;
            });
        }
    }
}
