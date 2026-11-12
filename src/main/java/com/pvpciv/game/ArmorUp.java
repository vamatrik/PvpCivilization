package com.pvpciv.game;

import com.pvpciv.inv.Bag;
import com.pvpciv.item.ArmorCoreItem;
import com.pvpciv.item.CivArmorItem;
import com.pvpciv.item.SimpleCivItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Улучшение брони (стол брони). Уровень брони хранится в STARS (0–11), опыт текущего уровня — в ARMOR_XP.
 * Хлам → опыт (по цене продажи); полная полоска + ядро (+ свиток на +5 и +10) → +1, без риска.
 * Ядро: для +1…+6 любое, +7/+8 — ✦, +9 — ✦✦, +10 — ✦✦✦.
 * +11: полная полоска + 6 благословенных + 2 кровавые звезды, шанс 50%; провал — взрыв, броня падает до +5.
 */
public final class ArmorUp {
    public static final int MAX = 11, BLESSED_11 = 6, BLOOD_11 = 2;

    public static int level(ItemStack st) { return Gear.stars(st); }
    public static int xp(ItemStack st) { return st.getOrDefault(ModRegistry.ARMOR_XP.get(), 0); }
    /** Опыт, нужный на переход L → L+1. */
    public static int need(int lvl) { return (int) Math.round(60 * Math.pow(1.28, lvl)); }
    /** Ядро какого уровня нужно на переход к target: ровно target (подходит и ядро сильнее). */
    public static int coreNeed(int target) { return target; }
    public static boolean scrollNeed(int target) { return target == 5 || target == 10; }
    /** Множитель брони и здоровья от уровня. */
    public static double mult(int lvl) { return lvl >= 11 ? 2.3 : 1 + 0.08 * lvl; }

    /** Сколько опыта даёт 1 предмет (0 — не хлам). */
    public static int junk(ItemStack st) {
        if (st.isEmpty()) return 0;
        if (st.is(ModRegistry.FROZEN_LOCKET.get()) || st.is(ModRegistry.GAS_MASK_FILTER.get()) || st.is(ModRegistry.DON_RING.get()) || st.is(ModRegistry.RAT_CROWN_SHARD.get()) || st.is(ModRegistry.BRUTE_CRYSTAL.get()) || st.is(ModRegistry.KEEPER_CRYSTAL.get())
                || st.is(ModRegistry.STAR_W1.get()) || st.is(ModRegistry.BLESSED_STAR_W1.get()) || st.is(ModRegistry.TEMPER_SCROLL.get())
                || st.is(ModRegistry.BRUTE_TROPHY.get()) || st.is(ModRegistry.KEEPER_TROPHY.get())) return 0;
        if (st.getItem() instanceof SimpleCivItem s) return s.sellPrice;
        if (st.getItem() instanceof CivArmorItem && level(st) == 0) return 30 * (Gear.rarity(st).ordinal() + 1);
        return 0;
    }

    /** Вложить хлам из стопки: тратит ровно столько, сколько влезает в полоску. Возвращает число потраченных. */
    /** «Родной» хлам брони — с той же локации, где она падает: ×1.5 опыта (бета 12.11). */
    public static boolean native_(ItemStack armor, ItemStack junk) {
        if (!(armor.getItem() instanceof CivArmorItem a)) return false;
        return a.set.startsWith("Инеевый") ? junk.is(ModRegistry.RIME_SHARD.get())
                : a.set.contains("ликвидатора") && junk.is(ModRegistry.RUBBER_SCRAP.get());
    }

    public static int junkFor(ItemStack armor, ItemStack junk) {
        int v = junk(junk);
        return native_(armor, junk) ? (int) Math.round(v * 1.5) : v;
    }

    public static int feed(ItemStack armor, ItemStack junk) {
        int v = junkFor(armor, junk), lvl = level(armor);
        if (v <= 0 || lvl >= MAX) return 0;
        int cap = need(Math.min(lvl, 10)), have = xp(armor);
        int used = 0;
        while (have < cap && used < junk.getCount()) { have += v; used++; }
        armor.set(ModRegistry.ARMOR_XP.get(), Math.min(cap, have));
        return used;
    }

    public static boolean full(ItemStack armor) { return level(armor) < MAX && xp(armor) >= need(Math.min(level(armor), 10)); }

    /** Подходящее ядро с наименьшей достаточной силой, или -1. */
    public static int coreFor(ServerPlayer p, int target) {
        for (int lv = coreNeed(target); lv <= 10; lv++) {
            final int fl = lv;
            if (Bag.count(p, s -> s.is(ModRegistry.ARMOR_CORE.get()) && ArmorCoreItem.level(s) == fl) > 0) return lv;
        }
        return -1;
    }

    /** Попытка улучшения. Текст результата — в чат. */
    public static void upgrade(ServerPlayer p, ItemStack armor) {
        if (!(armor.getItem() instanceof CivArmorItem)) return;
        int lvl = level(armor);
        if (lvl >= MAX) { msg(p, "Эта броня уже +11 — выше некуда.", ChatFormatting.GOLD); return; }
        if (!full(armor)) { msg(p, "Сначала заполни полоску опыта брони хламом.", ChatFormatting.RED); return; }
        ServerLevel lvlW = p.serverLevel();
        if (lvl == 10) {
            int bl = Bag.count(p, s -> s.is(ModRegistry.BLESSED_STAR_W1.get())), bd = Bag.count(p, s -> s.is(ModRegistry.BLOOD_STAR_W1.get()));
            if (bl < BLESSED_11 || bd < BLOOD_11) { msg(p, "Для +11 нужно " + BLESSED_11 + " благословенных и " + BLOOD_11 + " кровавые звезды.", ChatFormatting.RED); return; }
            Bag.take(p, s -> s.is(ModRegistry.BLESSED_STAR_W1.get()), BLESSED_11);
            Bag.take(p, s -> s.is(ModRegistry.BLOOD_STAR_W1.get()), BLOOD_11);
            armor.set(ModRegistry.ARMOR_XP.get(), 0);
            if (p.getRandom().nextBoolean()) {
                Gear.setStars(armor, 11);
                lvlW.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
                lvlW.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 0.8, 0.6, 0.05);
                Story.announceRareDrop(lvlW, armor);
                msg(p, "БРОНЯ +11! Звёзды крови приняли твою жертву.", ChatFormatting.DARK_RED);
            } else {
                Gear.setStars(armor, 5);
                lvlW.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1f, 1.2f);
                lvlW.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(), 3, 0.5, 0.5, 0.5, 0);
                msg(p, "Взрыв! Закалка не выдержала — броня откатилась до +5.", ChatFormatting.RED);
            }
            return;
        }
        int target = lvl + 1;
        int core = coreFor(p, target);
        if (core < 0) { msg(p, "Нужно ядро брони +" + target + " или сильнее (алтарь инфузии в подвале: медальон и " + (target <= 8 ? target + " зв." : (16 - target) + " обычных + " + (target - 8) + " благословенных зв.") + ").", ChatFormatting.RED); return; }
        boolean scroll = scrollNeed(target);
        if (scroll && Bag.count(p, s -> s.is(ModRegistry.TEMPER_SCROLL.get())) < 1) { msg(p, "На +" + target + " нужен Свиток закалки.", ChatFormatting.RED); return; }
        final int fc = core;
        Bag.take(p, s -> s.is(ModRegistry.ARMOR_CORE.get()) && ArmorCoreItem.level(s) == fc, 1);
        if (scroll) Bag.take(p, s -> s.is(ModRegistry.TEMPER_SCROLL.get()), 1);
        Gear.setStars(armor, target);
        armor.set(ModRegistry.ARMOR_XP.get(), 0);
        lvlW.playSound(null, p.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1f, 1.1f);
        lvlW.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1f, 1f);
        msg(p, "Броня улучшена до +" + target + "!", ChatFormatting.GREEN);
    }

    private static void msg(ServerPlayer p, String s, ChatFormatting c) { p.sendSystemMessage(Component.literal(s).withStyle(c)); }
}
