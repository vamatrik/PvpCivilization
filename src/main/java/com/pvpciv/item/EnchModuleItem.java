package com.pvpciv.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 0.11: модуль молекулярного стола. ПКМ рядом со столом — встаёт в следующее гнездо. */
public class EnchModuleItem extends Item {
    public final int tier;
    private static final String[] NAME = {"", "Базовый", "Улучшенный", "Продвинутый"};
    public EnchModuleItem(Properties p, int tier) { super(p); this.tier = tier; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        ItemStack st = p.getItemInHand(h);
        if (p instanceof net.minecraft.server.level.ServerPlayer sp) com.pvpciv.game.Enchanting.placeModule(sp, st, tier);
        return InteractionResultHolder.sidedSuccess(st, l.isClientSide);
    }

    @Override public boolean isFoil(ItemStack st) { return tier >= 3; }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext c, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("Модуль молекулярного стола · " + NAME[tier]).withStyle(x -> x.withColor(0x40D0FF)));
        tip.add(Component.literal("+1 вариант зачарования. Потолок ранга: " + new String[]{"", "II", "III", "IV"}[tier]).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("ПКМ рядом со столом — встанет в гнездо").withStyle(ChatFormatting.DARK_AQUA));
    }
}
