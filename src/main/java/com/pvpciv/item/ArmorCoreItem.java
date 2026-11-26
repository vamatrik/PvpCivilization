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
    public static int tierFor(double armorTier) { return armorTier < 2 ? 1 : armorTier < 3 ? 2 : armorTier < 4 ? 3 : armorTier < 5 ? 4 : 5; }

    /** 1.2: что кладут в центр алтаря для брони этажа N. */
    public static String centerName(int t) {
        return switch (t) { case 1 -> "Медальон инея"; case 2 -> "Фильтр противогаза"; case 3 -> "Нефритовая печать"; case 4 -> "Квантовый чип"; default -> "Астроядро"; };
    }
    public static boolean isCenter(ItemStack c) { return tierOfCenter(c) > 0; }

    public static int tierOfCenter(ItemStack c) {
        if (c.is(ModRegistry.FROZEN_LOCKET.get())) return 1;
        if (c.is(ModRegistry.GAS_MASK_FILTER.get())) return 2;
        if (c.is(ModRegistry.JADE_SEAL.get())) return 3;
        if (c.is(com.pvpciv.registry.ModF4.QUANTUM_CHIP.get())) return 4;
        if (c.is(com.pvpciv.registry.ModF5.ASTRO_CORE.get())) return 5;
        return 0;
    }

    public static String tierName(int t) { return t == 0 ? "универсальное (старое)" : centerName(t).toLowerCase() + " — броня этажа " + t; }

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
        tip.add(Component.literal("Материал: " + tierName(tier(st)) + (tier(st) > 1 ? " и ниже" : "")).withStyle(ChatFormatting.GRAY));
    }

    @Override public boolean isFoil(ItemStack st) { return level(st) >= 9; }
}
