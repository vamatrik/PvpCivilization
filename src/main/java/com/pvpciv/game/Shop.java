package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.inv.Bag;
import com.pvpciv.item.BagBuckleItem;
import com.pvpciv.item.BoatSummonItem;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.item.KeyCardItem;
import com.pvpciv.item.SimpleCivItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.loot.Rarity;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Магазин Сороки: постоянный ассортимент + ротация раз в 8 часов (реальное время). */
public final class Shop {
    public static final long ROTATION_MS = 8L * 60 * 60 * 1000;
    public record Offer(String id, ItemStack stack, int price, int limit) {}

    public static long rotation() { return System.currentTimeMillis() / ROTATION_MS; }
    public static long msLeft() { return ROTATION_MS - System.currentTimeMillis() % ROTATION_MS; }

    public static List<Offer> offers() {
        long rot = rotation();
        RandomSource r = RandomSource.create(rot * 7919L + 17);
        List<Offer> out = new ArrayList<>();
        out.add(new Offer("stew", new ItemStack(ModRegistry.DORM_STEW.get()), 12, 999));
        out.add(new Offer("bread", new ItemStack(Items.BREAD), 6, 999));
        out.add(new Offer("star", new ItemStack(ModRegistry.STAR_W1.get()), 120, 1));
        Item[] ws = {ModRegistry.RUSTY_CLEAVER.get(), ModRegistry.ASH_SABER.get(), ModRegistry.BONE_DAGGER.get(), ModRegistry.CHARRED_AXE.get()};
        for (int i = 0; i < 2; i++) {
            Rarity rar = r.nextFloat() < 0.35f ? Rarity.UNCOMMON : Rarity.COMMON;
            ItemStack w = Gear.withRarity(new ItemStack(ws[r.nextInt(ws.length)]), rar);
            out.add(new Offer("weapon" + i, w, rar == Rarity.UNCOMMON ? 150 : 60, 1));
        }
        out.add(new Offer("dust", new ItemStack(ModRegistry.ASH_DUST.get(), 8), 40, 3));
        if (r.nextFloat() < 0.4f) out.add(new Offer("buckle", new ItemStack(ModRegistry.BAG_BUCKLE.get()), 900, 1));
        return out;
    }

    private static CompoundTag bought(StoryData d) {
        long rot = rotation();
        if (d.shop.getLong("rot") != rot) { d.shop = new CompoundTag(); d.shop.putLong("rot", rot); }
        return d.shop;
    }

    public static int left(ServerPlayer p, Offer o) {
        StoryData d = Progress.d(p);
        return o.limit() - bought(d).getInt(o.id());
    }

    public static void buy(ServerPlayer p, int idx) {
        List<Offer> list = offers();
        if (idx < 0 || idx >= list.size()) return;
        Offer o = list.get(idx);
        StoryData d = Progress.d(p);
        CompoundTag b = bought(d);
        if (b.getInt(o.id()) >= o.limit()) { p.displayClientMessage(Component.literal("Распродано! Новый завоз скоро.").withStyle(ChatFormatting.GRAY), true); return; }
        if (d.money < o.price()) { p.displayClientMessage(Component.literal("Не хватает Искр: нужно " + o.price() + " ✦").withStyle(ChatFormatting.RED), true); return; }
        d.money -= o.price();
        b.putInt(o.id(), b.getInt(o.id()) + 1);
        p.setData(ModRegistry.STORY.get(), d);
        Bag.give(p, o.stack().copy());
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.8f, 1.4f);
        Story.sync(p);
        sendOffers(p);
    }

    public static void sendOffers(ServerPlayer p) {
        List<Network.OfferView> views = new ArrayList<>();
        for (Offer o : offers()) views.add(new Network.OfferView(o.stack(), o.price(), Math.max(0, left(p, o))));
        PacketDistributor.sendToPlayer(p, new Network.TraderOffers(views, msLeft()));
    }

    /** Цена продажи одной штуки (0 — не продаётся). */
    public static int sellPrice(ItemStack st) {
        if (st.isEmpty()) return 0;
        if (st.getItem() instanceof KeyCardItem || st.getItem() instanceof BoatSummonItem) return 0;
        if (st.is(ModRegistry.NOVICE_SWORD.get())) return 0;
        if (st.getItem() instanceof CivWeaponItem w) return (int) Math.round(8 * w.tier * (Gear.rarity(st).ordinal() + 1) * (Gear.rarity(st).ordinal() + 1) * (1 + 0.3 * Gear.stars(st)));
        if (st.getItem() instanceof SimpleCivItem s) return s.sellPrice;
        if (st.getItem() instanceof BagBuckleItem) return 200;
        if (st.is(ModRegistry.DORM_STEW.get())) return 3;
        if (st.is(Items.ROTTEN_FLESH)) return 1;
        if (st.is(Items.BREAD)) return 1;
        return 0;
    }

    public static void sellStack(ServerPlayer p, ItemStack st) {
        int price = sellPrice(st);
        if (price <= 0) return;
        long total = (long) price * st.getCount();
        st.setCount(0);
        Progress.addMoney(p, total, false);
        p.displayClientMessage(Component.literal("Продано: +" + total + " ✦").withStyle(ChatFormatting.GOLD), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.5f);
    }
}
