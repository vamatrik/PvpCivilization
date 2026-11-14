package com.pvpciv.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** 0.11: «Фазовый якорь» Стеллы. Заряжается рывками, пока лежит в инвентаре/сумке (нужно 1000). Заряженный — трофей, дорого берёт Сорока. */
public class PhaseAnchorItem extends SimpleCivItem {
    public static final int NEED = 1000;
    public PhaseAnchorItem(Properties p) { super(p, 2500, true); }

    public static int charge(ItemStack st) { CustomData c = st.get(DataComponents.CUSTOM_DATA); return c == null ? 0 : c.copyTag().getInt("charge"); }
    public static void setCharge(ItemStack st, int v) { CustomData.update(DataComponents.CUSTOM_DATA, st, t -> t.putInt("charge", v)); }
    public static boolean charged(ItemStack st) { return charge(st) >= NEED; }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        int c = Math.min(NEED, charge(st));
        tip.add(Component.literal("Прибор Стеллы. Гудит, если сделать рывок.").withStyle(ChatFormatting.GRAY));
        int fill = c * 20 / NEED;
        tip.add(Component.literal("Заряд  ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal("▮".repeat(fill)).withStyle(x -> x.withColor(0xC060FF)))
                .append(Component.literal("▮".repeat(20 - fill)).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("  " + c + "/" + NEED).withStyle(ChatFormatting.GRAY)));
        if (c >= NEED) tip.add(Component.literal("Заряжен. Трофей — Сорока даст 2500 ✦").withStyle(ChatFormatting.GOLD));
        else tip.add(Component.literal("Рывки с прибором в инвентаре или сумке заряжают его").withStyle(ChatFormatting.DARK_GRAY));
    }
}
