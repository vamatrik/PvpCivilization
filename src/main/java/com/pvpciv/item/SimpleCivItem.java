package com.pvpciv.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Материалы, хлам, звёзды, кристаллы: простой предмет с описанием и ценой продажи. */
public class SimpleCivItem extends Item {
    public final int sellPrice;
    public final String[] lines;
    private final boolean foil;

    public SimpleCivItem(Properties p, int sellPrice, boolean foil, String... lines) {
        super(p);
        this.sellPrice = sellPrice; this.lines = lines; this.foil = foil;
    }

    @Override public boolean isFoil(ItemStack st) { return foil || super.isFoil(st); }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        for (String l : lines) tip.add(Component.literal(l).withStyle(ChatFormatting.GRAY));
        if (sellPrice > 0) tip.add(Component.literal("Скупщик даст: " + sellPrice + " ✦ за шт.").withStyle(ChatFormatting.GOLD));
    }
}
