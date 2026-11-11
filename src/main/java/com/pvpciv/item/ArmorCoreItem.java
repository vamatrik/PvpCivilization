package com.pvpciv.item;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Ядро брони: крафт на алтаре инфузии (центр — Медальон инея, по краям 8 звёзд). Сила = число благословенных звёзд (0–3). */
public class ArmorCoreItem extends Item {
    public ArmorCoreItem(Properties p) { super(p); }

    public static int power(ItemStack st) { return st.getOrDefault(ModRegistry.CORE_POWER.get(), 0); }
    public static ItemStack of(int power) {
        ItemStack st = new ItemStack(ModRegistry.ARMOR_CORE.get());
        st.set(ModRegistry.CORE_POWER.get(), power);
        return st;
    }

    @Override
    public Component getName(ItemStack st) {
        int p = power(st);
        return Component.literal("Ядро брони" + (p > 0 ? " " + "✦".repeat(p) : "")).withStyle(p == 0 ? ChatFormatting.AQUA : p == 1 ? ChatFormatting.BLUE : p == 2 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag f) {
        int p = power(st);
        tip.add(Component.literal("Стол брони: полная полоска опыта + ядро → +1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal(switch (p) {
            case 0 -> "Подходит для +1 … +6";
            case 1 -> "Подходит для +1 … +8";
            case 2 -> "Подходит для +1 … +9";
            default -> "Подходит для любого уровня до +10";
        }).withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override public boolean isFoil(ItemStack st) { return power(st) > 0; }
}
