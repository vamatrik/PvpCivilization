package com.pvpciv.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Альманах Сёдо: ПКМ — бестиарий, книга оружия, книга брони. */
public class AlmanacItem extends Item {
    public AlmanacItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) com.pvpciv.game.Library.openAlmanac(sp);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override public boolean isFoil(ItemStack st) { return true; }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Бестиарий, оружие, броня. Пишется сам.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("ПКМ — открыть.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
