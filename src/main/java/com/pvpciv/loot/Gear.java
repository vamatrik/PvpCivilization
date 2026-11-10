package com.pvpciv.loot;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.world.item.ItemStack;

/** Доступ к данным снаряжения в компонентах предмета. */
public final class Gear {
    public static final int MAX_STARS = 10;

    public static Rarity rarity(ItemStack st) { return Rarity.byId(st.getOrDefault(ModRegistry.RARITY.get(), 0)); }
    public static int stars(ItemStack st) { return st.getOrDefault(ModRegistry.STARS.get(), 0); }

    public static ItemStack withRarity(ItemStack st, Rarity r) { st.set(ModRegistry.RARITY.get(), r.ordinal()); return st; }
    public static void setStars(ItemStack st, int s) { st.set(ModRegistry.STARS.get(), s); }

    /** Цвет свечения заточки. */
    public static int starColor(int stars) {
        if (stars >= 11) return 0x3A2A4A;
        if (stars == 10) return 0xFF3B3B;
        if (stars == 9) return 0xFFD030;
        if (stars >= 6) return 0xC060FF;
        if (stars >= 4) return 0x3D7BFF;
        return 0x7FD4FF;
    }

    /** Шанс успеха заточки с +s на +s+1. */
    public static double chance(int s) {
        double[] t = {0.95, 0.90, 0.85, 0.75, 0.65, 0.55, 0.45, 0.35, 0.25, 0.15};
        return s >= 0 && s < t.length ? t[s] : 0;
    }
}
