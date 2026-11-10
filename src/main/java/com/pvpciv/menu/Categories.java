package com.pvpciv.menu;

import com.pvpciv.item.BagBuckleItem;
import com.pvpciv.item.BoatSummonItem;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.item.KeyCardItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Категории предметов для фильтров и сортировки. */
public final class Categories {
    public static final int WEAPON = 0, UPGRADE = 1, MATERIAL = 2, FOOD = 3, OTHER = 4;
    public static final String[] NAMES = {"Оружие", "Улучшения", "Материалы", "Еда", "Прочее"};

    public static int of(ItemStack st) {
        if (st.getItem() instanceof CivWeaponItem) return WEAPON;
        if (st.is(ModRegistry.STAR_W1.get()) || st.is(ModRegistry.BLESSED_STAR_W1.get()) || st.getItem() instanceof BagBuckleItem) return UPGRADE;
        if (st.has(DataComponents.FOOD)) return FOOD;
        if (st.getItem() instanceof com.pvpciv.item.SimpleCivItem) return MATERIAL;
        return OTHER;
    }

    public static int rank(ItemStack st) {
        if (st.getItem() instanceof KeyCardItem || st.getItem() instanceof BoatSummonItem) return 1000;
        if (st.getItem() instanceof CivWeaponItem) return Gear.rarity(st).ordinal() * 20 + Gear.stars(st);
        if (st.is(ModRegistry.BLESSED_STAR_W1.get())) return 5;
        return 0;
    }
}
