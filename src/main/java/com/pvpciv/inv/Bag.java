package com.pvpciv.inv;

import com.pvpciv.data.StoryData;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.Predicate;

/** Сумка игрока: до 63 слотов, открываются уровнем и пряжками. Ванильные 27 слотов не используются. */
public final class Bag {
    public static final int MAX = 63, BASE = 18;
    public static final int[] LEVEL_MILESTONES = {4, 8, 12, 16, 20};

    public static ItemStackHandler get(Player p) { return p.getData(ModRegistry.BAG.get()); }

    public static int unlocked(int level, int extra) {
        int n = BASE;
        for (int m : LEVEL_MILESTONES) if (level >= m) n += 9;
        return Math.min(MAX, n + extra);
    }

    public static int unlocked(Player p) {
        StoryData d = p.getData(ModRegistry.STORY.get());
        return unlocked(d.level, d.extraSlots);
    }

    /** Кладёт в сумку, остаток возвращает. */
    public static ItemStack insert(Player p, ItemStack st) {
        if (st.getItem() instanceof com.pvpciv.item.SparkItem && p instanceof ServerPlayer sp) {
            com.pvpciv.progress.Progress.addMoney(sp, st.getCount(), true);
            return ItemStack.EMPTY;
        }
        ItemStackHandler h = get(p);
        int n = unlocked(p);
        ItemStack rest = st;
        for (int i = 0; i < n && !rest.isEmpty(); i++)
            if (!h.getStackInSlot(i).isEmpty()) rest = h.insertItem(i, rest, false);
        for (int i = 0; i < n && !rest.isEmpty(); i++) rest = h.insertItem(i, rest, false);
        return rest;
    }

    /** Выдать игроку: сумка → хотбар → на землю. */
    public static void give(ServerPlayer p, ItemStack st) {
        ItemStack rest = insert(p, st.copy());
        if (!rest.isEmpty() && !p.getInventory().add(rest)) p.drop(rest, false);
    }

    public static boolean has(Player p, Predicate<ItemStack> pred) {
        ItemStackHandler h = get(p);
        for (int i = 0; i < h.getSlots(); i++) if (pred.test(h.getStackInSlot(i))) return true;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (pred.test(inv.getItem(i))) return true;
        return false;
    }

    /** Забрать 1 предмет из сумки или инвентаря. */
    public static boolean take(Player p, Item item) {
        ItemStackHandler h = get(p);
        for (int i = 0; i < h.getSlots(); i++) if (h.getStackInSlot(i).is(item)) { h.extractItem(i, 1, false); return true; }
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) { inv.getItem(i).shrink(1); return true; }
        return false;
    }

    /** Переносит предметы из ванильных слотов 9–35 в сумку. */
    public static void sweep(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) return;
        ItemStackHandler h = get(p);
        for (int i = 0; i < h.getSlots(); i++)
            if (h.getStackInSlot(i).getItem() instanceof com.pvpciv.item.SparkItem) {
                com.pvpciv.progress.Progress.addMoney(p, h.getStackInSlot(i).getCount(), true);
                h.setStackInSlot(i, ItemStack.EMPTY);
            }
        Inventory inv = p.getInventory();
        for (int i = 9; i < 36; i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            ItemStack rest = insert(p, st);
            inv.setItem(i, rest);
        }
    }
}
