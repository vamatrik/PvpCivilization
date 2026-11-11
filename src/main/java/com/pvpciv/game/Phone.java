package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Телефон от Элеоноры («Поко X3 Про»): выкуп из темницы Кармиллы, «Шпиль.Еда», «Шпиль.Такси».
 * Экран телефона — клиентский; сервер присылает данные приложений (PhoneData) и принимает команды (PhoneCmd).
 */
public final class Phone {
    public static final int BAIL_PER_MIN = 100;
    public static final long BAIL_CD_MS = 2 * 3600_000L;

    private static long now() { return System.currentTimeMillis(); }

    public static boolean has(ServerPlayer p) { return Progress.d(p).eleStage >= 2; }

    /** Цена выкупа: 100 ✦ за каждую начатую минуту оставшегося срока. */
    public static int bailPrice(ServerPlayer p) {
        int s = Vampire.jailSeconds(p);
        return s < 0 ? 0 : ((s + 59) / 60) * BAIL_PER_MIN;
    }

    public static void open(ServerPlayer p) {
        if (!has(p)) return;
        if (Marks.answer(p)) return;
        StoryData d = Progress.d(p);
        com.google.gson.JsonObject j = new com.google.gson.JsonObject();
        j.addProperty("jailed", Vampire.jailed(p));
        j.addProperty("bail", bailPrice(p));
        j.addProperty("bailCd", Math.max(0, (d.phoneCd.getLong("a0") - now()) / 1000));
        j.addProperty("food", d.food.getInt("state"));
        j.addProperty("money", d.money);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3f, 1.8f);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(0, j.toString()));
    }

    /** Команды от экрана телефона: "bail", "food", "food_order:<корзина>:<тариф>", "taxi", "taxi_order:<от>:<до>", "taxi_cancel", "answer". */
    public static void cmd(ServerPlayer p, String c) {
        if (!has(p)) return;
        String[] a = c.split(":");
        try {
            switch (a[0]) {
                case "answer" -> Marks.answer(p);
                case "bail" -> bail(p);
                case "food" -> FoodApp.open(p);
                case "food_order" -> FoodApp.order(p, a[1], Integer.parseInt(a[2]));
                case "taxi" -> Taxi.open(p);
                case "taxi_order" -> Taxi.order(p, Integer.parseInt(a[1]), Integer.parseInt(a[2]));
                case "taxi_cancel" -> Taxi.cancel(p);
                default -> {}
            }
        } catch (RuntimeException ignored) {}
    }

    private static void bail(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (!Vampire.jailed(p)) { msg(p, "Ты и так на свободе.", ChatFormatting.GRAY); return; }
        long left = d.phoneCd.getLong("a0") - now();
        if (left > 0) { msg(p, "Элеонора ещё не остыла после прошлого раза: " + (left / 60000 + 1) + " мин.", ChatFormatting.RED); return; }
        int price = bailPrice(p);
        if (d.money < price) { msg(p, "Не хватает Искр: выкуп стоит " + price + " ✦ (" + BAIL_PER_MIN + " ✦ за минуту срока).", ChatFormatting.RED); return; }
        d.money -= price;
        d.phoneCd.putLong("a0", now() + BAIL_CD_MS);
        p.setData(ModRegistry.STORY.get(), d);
        Story.sync(p);
        Story.say(p, "Элеонора", 0xB0C4FF, "Сейчас вытащу. Кармилла опять будет дуться… переживёт.");
        GameEvents.later(p.server, 60, () -> { Vampire.release(p); msg(p, "Элеонора выкупила тебя из темницы за " + price + " ✦.", ChatFormatting.AQUA); });
    }

    /** Рингтон телефона (звонок): длинный трек, останавливается stopRingtone. */
    public static void ringtone(ServerPlayer p) {
        p.playNotifySound(ModRegistry.PHONE_RING.get(), SoundSource.PLAYERS, 1f, 1f);
    }

    public static void stopRingtone(ServerPlayer p) {
        p.connection.send(new ClientboundStopSoundPacket(ModRegistry.PHONE_RING.getId(), SoundSource.PLAYERS));
    }

    /** Короткое уведомление (заказ, курьер, такси). */
    public static void chime(ServerPlayer p) {
        float[] notes = {1.5f, 2.0f};
        for (int i = 0; i < notes.length; i++) {
            final float n = notes[i];
            GameEvents.later(p.server, i * 3, () -> p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.5f, n));
        }
    }

    private static void msg(ServerPlayer p, String s, ChatFormatting c) { p.sendSystemMessage(Component.literal(s).withStyle(c)); }
}
