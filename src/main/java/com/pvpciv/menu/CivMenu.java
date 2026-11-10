package com.pvpciv.menu;

import com.pvpciv.inv.Bag;
import com.pvpciv.inv.BagSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.BooleanSupplier;

/** Общая основа меню: сумка 9×7 + хотбар, синхронизация числа открытых слотов. */
public abstract class CivMenu extends AbstractContainerMenu {
    public final Player player;
    public final ItemStackHandler bag;
    protected final DataSlot unlocked = DataSlot.standalone();
    public int bagStart = -1, hotStart = -1;

    protected CivMenu(MenuType<?> type, int id, Inventory inv) {
        super(type, id);
        this.player = inv.player;
        this.bag = Bag.get(inv.player);
        unlocked.set(Bag.unlocked(inv.player));
        addDataSlot(unlocked);
    }

    public int unlockedSlots() { return unlocked.get(); }

    protected void addBag(int x, int y, BooleanSupplier visible) {
        bagStart = slots.size();
        for (int i = 0; i < Bag.MAX; i++)
            addSlot(new BagSlot(bag, i, x + (i % 9) * 18, y + (i / 9) * 18, unlocked::get, visible));
    }

    protected void addHotbar(Inventory inv, int x, int y) {
        hotStart = slots.size();
        for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, x + i * 18, y));
    }

    public boolean isBagSlot(int idx) { return bagStart >= 0 && idx >= bagStart && idx < bagStart + Bag.MAX; }
    public boolean isHotbarSlot(int idx) { return hotStart >= 0 && idx >= hotStart && idx < hotStart + 9; }

    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer) unlocked.set(Bag.unlocked(player));
        super.broadcastChanges();
    }

    /** Перенос по Shift-клику между сумкой и хотбаром (и в доп. слоты станции). */
    protected ItemStack moveBetween(int index, int extraStart, int extraEnd) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        boolean moved;
        if (extraStart >= 0 && index >= extraStart && index < extraEnd) {
            moved = moveItemStackTo(st, bagStart, bagStart + unlocked.get(), false) || moveItemStackTo(st, hotStart, hotStart + 9, false);
        } else if (extraStart >= 0 && moveItemStackTo(st, extraStart, extraEnd, false)) {
            moved = true;
        } else if (isBagSlot(index)) {
            moved = moveItemStackTo(st, hotStart, hotStart + 9, false);
        } else {
            moved = moveItemStackTo(st, bagStart, bagStart + unlocked.get(), false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player p) { return p.isAlive(); }
}
