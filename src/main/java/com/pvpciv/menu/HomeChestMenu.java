package com.pvpciv.menu;

import com.pvpciv.game.HomeChest;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Домашний сундук: 5×9 личных слотов слева, сумка справа. */
public class HomeChestMenu extends CivMenu {
    public final ItemStackHandler home;

    public HomeChestMenu(int id, Inventory inv) {
        super(ModRegistry.HOME_CHEST_MENU.get(), id, inv);
        home = inv.player.getData(ModRegistry.HOME_STORAGE.get());
        for (int i = 0; i < HomeChest.SIZE; i++) addSlot(new SlotItemHandler(home, i, 10 + (i % 9) * 18, 37 + (i / 9) * 18));
        addBag(185, 37, () -> true);
        addHotbar(inv, 185, 181);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) { return moveBetween(index, 0, HomeChest.SIZE); }
}
