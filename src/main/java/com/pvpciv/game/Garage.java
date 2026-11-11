package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.inv.Bag;
import com.pvpciv.item.BoatSummonItem;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Гараж лодок: каждая найденная лодка навсегда открывается в гараже, при себе — только выбранная.
 * Новая лодка лучше текущей — выбирается сама; сменить можно во вкладке «Герой» → «Лодки».
 */
public final class Garage {
    public static final String DEFAULT = "pvpciv:oak_boat_t1";

    public static String id(Item it) { return BuiltInRegistries.ITEM.getKey(it).toString(); }
    public static Item item(String id) {
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id.isEmpty() ? DEFAULT : id));
        return it instanceof BoatSummonItem ? it : ModRegistry.OAK_BOAT_T1.get();
    }
    private static int tier(String id) { return item(id) instanceof BoatSummonItem b ? b.tier : 0; }

    public static ItemStack activeStack(ServerPlayer p) {
        StoryData d = Progress.d(p);
        return new ItemStack(item(d.activeBoat));
    }

    /** Раз в секунду: занести найденные лодки в гараж, лишние убрать, выбранная — при себе. */
    public static void tick(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) return;
        StoryData d = Progress.d(p);
        boolean changed = false;
        if (d.boats.add(DEFAULT)) changed = true;
        if (d.activeBoat.isEmpty()) { d.activeBoat = DEFAULT; changed = true; }
        Inventory inv = p.getInventory();
        ItemStackHandler bag = Bag.get(p);
        String best = null;
        for (int i = 0; i < inv.getContainerSize(); i++) best = see(d, inv.getItem(i), best);
        for (int i = 0; i < bag.getSlots(); i++) best = see(d, bag.getStackInSlot(i), best);
        if (best != null) {
            changed = true;
            if (tier(best) > tier(d.activeBoat)) {
                d.activeBoat = best;
                p.sendSystemMessage(Component.literal("⛵ Новая лодка в гараже: ").withStyle(ChatFormatting.AQUA)
                        .append(new ItemStack(item(best)).getHoverName()).append(Component.literal(" — выбрана по умолчанию. Сменить: Герой → Лодки.").withStyle(ChatFormatting.GRAY)));
            } else p.sendSystemMessage(Component.literal("⛵ Лодка добавлена в гараж (Герой → Лодки).").withStyle(ChatFormatting.AQUA));
        }
        if (changed) p.setData(ModRegistry.STORY.get(), d);
        // при себе — только выбранная, одна
        Item act = item(d.activeBoat);
        boolean kept = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!(st.getItem() instanceof BoatSummonItem)) continue;
            if (st.is(act) && !kept) kept = true; else inv.setItem(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < bag.getSlots(); i++) {
            ItemStack st = bag.getStackInSlot(i);
            if (!(st.getItem() instanceof BoatSummonItem)) continue;
            if (st.is(act) && !kept) kept = true; else bag.setStackInSlot(i, ItemStack.EMPTY);
        }
        if (!kept && !inv.add(new ItemStack(act))) Bag.give(p, new ItemStack(act));
    }

    private static String see(StoryData d, ItemStack st, String best) {
        if (!(st.getItem() instanceof BoatSummonItem)) return best;
        String id = id(st.getItem());
        if (d.boats.add(id) && (best == null || tier(id) > tier(best))) return id;
        return best;
    }

    public static void select(ServerPlayer p, String id) {
        StoryData d = Progress.d(p);
        if (!d.boats.contains(id) || id.equals(d.activeBoat)) return;
        Inventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).getItem() instanceof BoatSummonItem) { slot = i; inv.setItem(i, ItemStack.EMPTY); }
        d.activeBoat = id;
        p.setData(ModRegistry.STORY.get(), d);
        ItemStack st = new ItemStack(item(id));
        if (slot >= 0) inv.setItem(slot, st); else if (!inv.add(st)) Bag.give(p, st);
        p.displayClientMessage(Component.literal("⛵ Выбрана лодка: ").withStyle(ChatFormatting.AQUA).append(st.getHoverName()), true);
        tick(p);
        Story.sync(p);
    }
}
