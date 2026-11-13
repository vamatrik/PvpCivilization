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

    /** С кем сейчас торгует игрок: "" — Сорока, "smuggler" — контрабандист в канализации. */
    public static final java.util.Map<java.util.UUID, String> VENDOR = new java.util.HashMap<>();

    public static List<Offer> offers(ServerPlayer p) {
        String v = VENDOR.get(p.getUUID());
        return "chef".equals(v) ? chef() : "smuggler".equals(v) ? smuggler() : "eleonora".equals(v) ? eleonora() : offers();
    }

    public static boolean tokenShop(ServerPlayer p) { return "eleonora".equals(VENDOR.get(p.getUUID())); }

    /** Лавка Элеоноры (за жетоны): зелья пачками (лимит в день) и звёзды — по одной за завоз. */
    public static List<Offer> eleonora() {
        List<Offer> out = new ArrayList<>();
        out.add(new Offer("p_heal", new ItemStack(ModRegistry.POTION_HEAL.get(), 3), 3, 5));
        out.add(new Offer("p_regen", new ItemStack(ModRegistry.POTION_REGEN.get(), 3), 4, 5));
        out.add(new Offer("p_swift", new ItemStack(ModRegistry.POTION_SWIFT.get(), 3), 3, 5));
        out.add(new Offer("p_might", new ItemStack(ModRegistry.POTION_MIGHT.get(), 2), 5, 4));
        out.add(new Offer("p_stone", new ItemStack(ModRegistry.POTION_STONE.get(), 2), 5, 4));
        out.add(new Offer("u_blessed", new ItemStack(ModRegistry.BLESSED_STAR_W1.get()), 40, 1));
        out.add(new Offer("u_blood", new ItemStack(ModRegistry.BLOOD_STAR_W1.get()), 160, 1));
        return out;
    }

    /** Покупки у Элеоноры: все лимиты (и зелья, и звёзды) сбрасываются с завозом её лавки — раз в сутки в полночь по Москве. */
    private static CompoundTag eleBought(StoryData d) {
        long day = Marks.day();
        if (d.eleShop.getLong("day") != day) { d.eleShop = new CompoundTag(); d.eleShop.putLong("day", day); }
        return d.eleShop;
    }

    private static int eleCount(StoryData d, String id) {
        CompoundTag b = eleBought(d);
        return b.getInt(id);
    }

    /** Контрабандист: еда, золотые яблоки (лимит), товар с этажа 3 (катана), второй рюкзак — один раз за игру. */
    public static List<Offer> smuggler() {
        List<Offer> out = new ArrayList<>();
        out.add(new Offer("s_beef", new ItemStack(Items.COOKED_BEEF, 4), 40, 999));
        out.add(new Offer("s_carrot", new ItemStack(Items.GOLDEN_CARROT, 2), 90, 8));
        out.add(new Offer("s_apple", new ItemStack(Items.GOLDEN_APPLE), 450, 2));
        out.add(new Offer("s_filter", new ItemStack(ModRegistry.GAS_MASK_FILTER.get()), 380, 2));
        out.add(new Offer("s_scroll", new ItemStack(ModRegistry.TEMPER_SCROLL.get()), 1600, 1));
        out.add(new Offer("s_blood", new ItemStack(ModRegistry.BLOOD_STAR_W1.get()), 5200, 1));
        out.add(new Offer("katana", Gear.withRarity(new ItemStack(ModRegistry.SAMURAI_KATANA.get()), Rarity.RARE), 4800, 1));   // 0.9.0: было 7500 — дорого для своего урона
        out.add(new Offer("pack", new ItemStack(ModRegistry.SMUGGLER_PACK.get()), 6000, 1));
        return out;
    }

    /** Повар Харуки (деревня самураев, этаж 3): японская кухня и одно особое блюдо в завоз. */
    public static List<Offer> chef() {
        List<Offer> out = new ArrayList<>();
        out.add(new Offer("c_onigiri", new ItemStack(ModRegistry.ONIGIRI.get(), 3), 45, 999));
        out.add(new Offer("c_ramen", new ItemStack(ModRegistry.RAMEN.get()), 70, 12));
        out.add(new Offer("c_sushi", new ItemStack(ModRegistry.SUSHI.get(), 2), 80, 10));
        out.add(new Offer("c_mochi", new ItemStack(ModRegistry.MOCHI.get(), 2), 60, 10));
        out.add(new Offer("c_tako", new ItemStack(ModRegistry.TAKOYAKI.get()), 110, 6));
        out.add(new Offer("c_seal", new ItemStack(ModRegistry.JADE_SEAL.get()), 520, 2));
        out.add(new Offer("c_bento", new ItemStack(ModRegistry.IMPERIAL_BENTO.get()), 2400, 1));
        return out;
    }

    /** Катана: кроме Искр — хороший меч этажа 2 (тир 2, редкий и выше, не именной). */
    private static boolean tradeBlade(ItemStack st) {
        return st.getItem() instanceof com.pvpciv.item.CivWeaponItem w && w.tier == 2 && w.skill == null
                && Gear.rarity(st).ordinal() >= Rarity.RARE.ordinal();
    }

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
        if (tokenShop(p)) return o.limit() - eleCount(d, o.id());
        if (o.id().equals("buckle") && d.boughtBuckle) return 0;   // пряжка у Сороки — один раз за игру
        if (o.id().equals("pack") && d.boughtPack) return 0;
        return o.limit() - bought(d).getInt(o.id());
    }

    public static void buy(ServerPlayer p, int idx) {
        List<Offer> list = offers(p);
        if (idx < 0 || idx >= list.size()) return;
        Offer o = list.get(idx);
        StoryData d = Progress.d(p);
        if (tokenShop(p)) { buyEle(p, d, o); return; }
        CompoundTag b = bought(d);
        if (b.getInt(o.id()) >= o.limit() || (o.id().equals("buckle") && d.boughtBuckle) || (o.id().equals("pack") && d.boughtPack)) { p.displayClientMessage(Component.literal("Распродано! Новый завоз скоро.").withStyle(ChatFormatting.GRAY), true); return; }
        if (d.money < o.price()) { p.displayClientMessage(Component.literal("Не хватает Искр: нужно " + o.price() + " ✦").withStyle(ChatFormatting.RED), true); return; }
        if (o.id().equals("katana")) {
            if (Bag.count(p, Shop::tradeBlade) < 1) {
                p.displayClientMessage(Component.literal("«Катану — только за меч в придачу: тир 2, редкий или лучше.»").withStyle(ChatFormatting.RED), true);
                return;
            }
            Bag.take(p, Shop::tradeBlade, 1);
            p.sendSystemMessage(Component.literal("Контрабандист забрал твой меч этажа 2 в придачу.").withStyle(ChatFormatting.GRAY));
        }
        d.money -= o.price();
        b.putInt(o.id(), b.getInt(o.id()) + 1);
        if (o.id().equals("buckle")) d.boughtBuckle = true;
        if (o.id().equals("pack")) d.boughtPack = true;
        p.setData(ModRegistry.STORY.get(), d);
        Bag.give(p, o.stack().copy());
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.8f, 1.4f);
        Story.sync(p);
        sendOffers(p);
    }

    private static void buyEle(ServerPlayer p, StoryData d, Offer o) {
        if (eleCount(d, o.id()) >= o.limit()) {
            p.displayClientMessage(Component.literal(o.id().startsWith("u_") ? "«Звезда — одна за завоз. Приходи завтра.»" : "«На сегодня хватит. Завтра сварю ещё.»").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (d.eleTokens < o.price()) { p.displayClientMessage(Component.literal("Не хватает жетонов: нужно " + o.price()).withStyle(ChatFormatting.RED), true); return; }
        d.eleTokens -= o.price();
        CompoundTag b = eleBought(d);
        b.putInt(o.id(), b.getInt(o.id()) + 1);
        p.setData(ModRegistry.STORY.get(), d);
        Bag.give(p, o.stack().copy());
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 1.2f);
        sendOffers(p);
    }

    public static void sendOffers(ServerPlayer p) {
        List<Network.OfferView> views = new ArrayList<>();
        for (Offer o : offers(p)) views.add(new Network.OfferView(o.stack(), o.price(), Math.max(0, left(p, o))));
        PacketDistributor.sendToPlayer(p, new Network.TraderOffers(views, tokenShop(p) ? 86_400_000L - (System.currentTimeMillis() + 3 * 3600_000L) % 86_400_000L : msLeft(), tokenShop(p) ? Progress.d(p).eleTokens : -1));
    }

    /** Цена продажи одной штуки (0 — не продаётся). */
    public static int sellPrice(ItemStack st) {
        if (st.isEmpty()) return 0;
        if (st.getItem() instanceof KeyCardItem || st.getItem() instanceof BoatSummonItem) return 0;
        if (st.is(ModRegistry.NOVICE_SWORD.get())) return 0;
        if (st.getItem() instanceof CivWeaponItem w) return (int) Math.round(8 * w.tierValue() * (Gear.rarity(st).ordinal() + 1) * (Gear.rarity(st).ordinal() + 1) * (1 + 0.3 * Gear.stars(st)));
        if (st.getItem() instanceof com.pvpciv.item.CivArmorItem a) return (int) Math.round(14 * (Gear.rarity(st).ordinal() + 1) * (Gear.rarity(st).ordinal() + 1));
        if (st.getItem() instanceof SimpleCivItem s) return s.sellPrice;
        if (st.getItem() instanceof BagBuckleItem) return 200;
        if (st.is(ModRegistry.DORM_STEW.get())) return 3;
        if (st.is(Items.ROTTEN_FLESH)) return 1;
        if (st.is(Items.BREAD)) return 1;
        return 0;
    }

    public static void sellStack(ServerPlayer p, ItemStack st) {
        if (tokenShop(p)) { p.displayClientMessage(Component.literal("«Я охотница, а не скупщица.»").withStyle(ChatFormatting.GRAY), true); return; }
        int price = sellPrice(st);
        if (price <= 0) return;
        long total = (long) price * st.getCount();
        st.setCount(0);
        Progress.addMoney(p, total, false);
        p.displayClientMessage(Component.literal("Продано: +" + total + " ✦").withStyle(ChatFormatting.GOLD), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.5f);
    }
}
