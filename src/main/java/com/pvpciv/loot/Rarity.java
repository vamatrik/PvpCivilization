package com.pvpciv.loot;

import net.minecraft.util.RandomSource;

/** Редкость предмета. */
public enum Rarity {
    COMMON("Обычный", 0xB0B0B0, 1.00, 600, 1),
    UNCOMMON("Необычный", 0x55FF55, 1.15, 250, 1),
    RARE("Редкий", 0x5599FF, 1.35, 100, 2),
    EPIC("Эпический", 0xC060FF, 1.60, 40, 2),
    LEGENDARY("Легендарный", 0xFFA020, 1.90, 9, 3),
    MYTHIC("Мифический", 0xFF3030, 2.30, 1, 3);

    public final String title;
    public final int color;
    public final double mult;
    public final int weight;
    public final int enchantSlots;

    Rarity(String title, int color, double mult, int weight, int slots) {
        this.title = title; this.color = color; this.mult = mult; this.weight = weight; this.enchantSlots = slots;
    }

    public static Rarity byId(int id) { return values()[Math.max(0, Math.min(values().length - 1, id))]; }

    /** Бросок редкости не ниже min. Мифик — только если allowMythic. */
    public static Rarity roll(RandomSource rnd, Rarity min, boolean allowMythic) {
        int total = 0;
        for (Rarity r : values()) if (r.ordinal() >= min.ordinal() && (allowMythic || r != MYTHIC)) total += r.weight;
        int x = rnd.nextInt(total);
        for (Rarity r : values()) {
            if (r.ordinal() < min.ordinal() || (!allowMythic && r == MYTHIC)) continue;
            x -= r.weight;
            if (x < 0) return r;
        }
        return min;
    }
}
