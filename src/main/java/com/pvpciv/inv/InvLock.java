package com.pvpciv.inv;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * 0.11.1 — верхние 3 строки ванильного инвентаря (слоты 9–35) закрыты «заглушками»: всё лишнее раньше утекало туда
 * мимо сумки. Заглушка — барьер с меткой; выбросить нельзя, при смерти не выпадает. Если в закрытый слот что-то попало —
 * уходит в сумку (или под ноги, если сумка полна).
 */
public final class InvLock {
    private InvLock() {}
    public static final int FROM = 9, TO = 35;

    public static ItemStack plug() {
        ItemStack s = new ItemStack(Items.BARRIER);
        s.set(DataComponents.CUSTOM_NAME, Component.literal("Закрыто").withStyle(x -> x.withItalic(false).withColor(ChatFormatting.DARK_GRAY)));
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putBoolean("pvpciv_lock", true));
        return s;
    }

    public static boolean isPlug(ItemStack s) {
        if (!s.is(Items.BARRIER)) return false;
        CustomData c = s.get(DataComponents.CUSTOM_DATA);
        return c != null && c.copyTag().getBoolean("pvpciv_lock");
    }

    public static void tick(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) return;
        var inv = p.getInventory();
        for (int i = FROM; i <= TO; i++) {
            ItemStack s = inv.getItem(i);
            if (isPlug(s) && s.getCount() == 1) continue;
            inv.setItem(i, plug());
            if (!s.isEmpty() && !isPlug(s)) {
                ItemStack rest = Bag.insert(p, s);
                if (!rest.isEmpty()) p.drop(rest, false);
            }
        }
        // заглушки, утащенные в хотбар/руку — убрать
        for (int i = 0; i < 9; i++) if (isPlug(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);
        if (isPlug(p.getOffhandItem())) p.getInventory().offhand.set(0, ItemStack.EMPTY);
        if (isPlug(p.containerMenu.getCarried())) p.containerMenu.setCarried(ItemStack.EMPTY);
    }
}
