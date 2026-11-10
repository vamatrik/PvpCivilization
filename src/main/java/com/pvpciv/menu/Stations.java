package com.pvpciv.menu;

import com.pvpciv.data.StoryData;
import com.pvpciv.game.Story;
import com.pvpciv.inv.Bag;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Уровни станций квартиры (1–5) и стоимость улучшения. */
public final class Stations {
    public static final int MAX = 5;
    public static final int FORGE = 0, DISMANTLE = 1;

    public static long moneyCost(int station, int lvl) { return Math.round((station == FORGE ? 300 : 200) * Math.pow(3, lvl - 1)); }
    public static int dustCost(int lvl) { return 10 * lvl; }

    public static int level(StoryData d, int station) { return station == FORGE ? d.forgeLvl : d.dismLvl; }

    private static int countDust(ServerPlayer p) {
        int n = 0;
        var h = Bag.get(p);
        for (int i = 0; i < h.getSlots(); i++) if (h.getStackInSlot(i).is(ModRegistry.ASH_DUST.get())) n += h.getStackInSlot(i).getCount();
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ModRegistry.ASH_DUST.get())) n += inv.getItem(i).getCount();
        return n;
    }

    public static boolean upgrade(ServerPlayer p, int station) {
        StoryData d = Progress.d(p);
        int lvl = level(d, station);
        if (lvl >= MAX) { p.displayClientMessage(Component.literal("Станция уже максимального уровня").withStyle(ChatFormatting.GRAY), true); return false; }
        long cost = moneyCost(station, lvl);
        int dust = dustCost(lvl);
        if (d.money < cost || countDust(p) < dust) {
            p.displayClientMessage(Component.literal("Нужно " + cost + " ✦ и " + dust + " пепельной пыли").withStyle(ChatFormatting.RED), true);
            return false;
        }
        for (int i = 0; i < dust; i++) Bag.take(p, ModRegistry.ASH_DUST.get());
        d = Progress.d(p);
        d.money -= cost;
        if (station == FORGE) d.forgeLvl++; else d.dismLvl++;
        p.setData(ModRegistry.STORY.get(), d);
        p.level().playSound(null, p.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
        Story.title(p, (station == FORGE ? "Кузница" : "Разборочный стол") + " ур. " + level(d, station), "Станция улучшена!", 0xFFD166);
        Story.sync(p);
        return true;
    }
}
