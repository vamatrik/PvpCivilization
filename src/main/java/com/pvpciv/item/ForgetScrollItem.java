package com.pvpciv.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1.0 — свиток забвения: сброс класса и узлов, тёмная материя возвращается. Перезарядка — сутки. */
public class ForgetScrollItem extends SimpleCivItem {
    public ForgetScrollItem(Properties p) {
        super(p, 0, true, "Свиток забвения. Пахнет лунной пылью.", "ПКМ — забыть класс и все узлы дерева.", "Вся потраченная тёмная материя вернётся.", "Перезарядка памяти — 24 часа.");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level lvl, Player pl, InteractionHand hand) {
        ItemStack st = pl.getItemInHand(hand);
        if (pl instanceof ServerPlayer p && com.pvpciv.game.Classes.forget(p)) st.shrink(1);
        return InteractionResultHolder.sidedSuccess(st, lvl.isClientSide);
    }
}
