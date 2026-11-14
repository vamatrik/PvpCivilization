package com.pvpciv.item;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ядро брони (алтарь инфузии): в центре — Медальон инея или фильтр противогаза, по краям — звёзды.
 * Ядро +N — медальон и N обычных звёзд (N = 1…8); +9 — 7 обычных + 1 благословенная, +10 — 6 обычных + 2 благословенные.
 * Ядро +N подходит для улучшения до +N включительно (стол берёт самое слабое из подходящих).
 */
public class ArmorCoreItem extends Item {
    public ArmorCoreItem(Properties p) { super(p); }

    /** До какого + подходит. Старые ядра (до 0.8.6, «сила» 0–3) пересчитываются: 0 → +6, 1 → +8, 2 → +9, 3 → +10. */
    public static int level(ItemStack st) {
        Integer l = st.get(ModRegistry.CORE_LEVEL.get());
        if (l != null) return l;
        return switch (st.getOrDefault(ModRegistry.CORE_POWER.get(), 0)) { case 0 -> 6; case 1 -> 8; case 2 -> 9; default -> 10; };
    }

    public static ItemStack of(int level) {
        ItemStack st = new ItemStack(ModRegistry.ARMOR_CORE.get());
        st.set(ModRegistry.CORE_LEVEL.get(), level);
        return st;
    }

    public static ItemStack of(int level, int tier) {
        ItemStack st = of(level);
        if (tier > 0) st.set(ModRegistry.CORE_TIER.get(), tier);
        return st;
    }

    /** Материал ядра: 1–3; 0 — старое ядро без материала (подходит ко всему). */
    public static int tier(ItemStack st) { return st.getOrDefault(ModRegistry.CORE_TIER.get(), 0); }

    /** Какой материал нужен броне: этаж 1 — медальон, этаж 2 — фильтр, этаж 3 — печать. */
    public static int tierFor(double armorTier) { return armorTier < 2 ? 1 : armorTier < 3 ? 2 : 3; }

    public static int tierOfCenter(ItemStack c) {
        if (c.is(ModRegistry.FROZEN_LOCKET.get())) return 1;
        if (c.is(ModRegistry.GAS_MASK_FILTER.get())) return 2;
        if (c.is(ModRegistry.JADE_SEAL.get())) return 3;
        return 0;
    }

    public static String tierName(int t) { return t == 1 ? "медальон инея — броня этажа 1" : t == 2 ? "фильтр — броня этажа 2" : t == 3 ? "нефритовая печать — броня этажа 3" : "универсальное (старое)"; }

    /** Подходит ли ядро к броне: материал не ниже нужного. */
    public static boolean fits(ItemStack core, double armorTier) { int t = tier(core); return t == 0 || t >= tierFor(armorTier); }

    @Override
    public Component getName(ItemStack st) {
        int l = level(st);
        return Component.literal("Ядро брони +" + l).withStyle(l <= 4 ? ChatFormatting.AQUA : l <= 8 ? ChatFormatting.BLUE : l == 9 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag f) {
        int l = level(st);
        tip.add(Component.literal("Стол брони: полная полоска опыта + ядро → +1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Подходит для улучшения до +" + l + " включительно").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("Материал: " + tierName(tier(st)) + (tier(st) > 0 && tier(st) < 3 ? " и ниже" : "")).withStyle(ChatFormatting.GRAY));
    }

    @Override public boolean isFoil(ItemStack st) { return level(st) >= 9; }
}
