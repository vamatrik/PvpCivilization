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

/** Общая основа меню: сумка 9×7 (листается колесом) + хотбар, синхронизация числа открытых слотов. */
public abstract class CivMenu extends AbstractContainerMenu {
    public final Player player;
    public final ItemStackHandler bag;
    protected final DataSlot unlocked = DataSlot.standalone();
    public int bagStart = -1, hotStart = -1, bagX, bagY;
    /** прокрутка сумки в строках (клиент и сервер синхронны через кнопку SCROLL_BTN+row) */
    public int scroll;
    public static final int SCROLL_BTN = 900;

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
        bagX = x; bagY = y;
        var view = new com.pvpciv.inv.BagView(bag, () -> scroll * 9);
        for (int i = 0; i < Bag.VIEW; i++)
            addSlot(new BagSlot(view, i, x + (i % 9) * 18, y + (i / 9) * 18, unlocked::get, () -> scroll * 9, visible));
    }

    /** всего строк, доступных к прокрутке (открытые + одна закрытая «на вырост») */
    public int bagRows() { return Math.min(Bag.MAX / 9, (unlocked.get() + 8) / 9 + 1); }
    public int maxScroll() { return Math.max(0, bagRows() - Bag.VIEW / 9); }

    /** обработка кнопки прокрутки; вызывать первой строкой clickMenuButton подклассов */
    protected boolean scrollButton(int id) {
        if (id < SCROLL_BTN || id > SCROLL_BTN + Bag.MAX / 9) return false;
        scroll = Math.max(0, Math.min(maxScroll(), id - SCROLL_BTN));
        broadcastChanges();
        return true;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) { return scrollButton(id) || super.clickMenuButton(p, id); }

    /** Положить в сумку целиком (не только видимую часть). */
    protected boolean toBag(ItemStack st) {
        int before = st.getCount();
        int n = unlocked.get();
        for (int i = 0; i < n && !st.isEmpty(); i++) if (!bag.getStackInSlot(i).isEmpty() && ItemStack.isSameItemSameComponents(bag.getStackInSlot(i), st)) st.setCount(bag.insertItem(i, st.copy(), false).getCount());
        for (int i = 0; i < n && !st.isEmpty(); i++) if (bag.getStackInSlot(i).isEmpty()) st.setCount(bag.insertItem(i, st.copy(), false).getCount());
        return st.getCount() != before;
    }

    protected void addHotbar(Inventory inv, int x, int y) {
        hotStart = slots.size();
        for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, x + i * 18, y));
    }

    public boolean isBagSlot(int idx) { return bagStart >= 0 && idx >= bagStart && idx < bagStart + Bag.VIEW; }
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
            moved = toBag(st) || moveItemStackTo(st, hotStart, hotStart + 9, false);
        } else if (extraStart >= 0 && moveItemStackTo(st, extraStart, extraEnd, false)) {
            moved = true;
        } else if (isBagSlot(index)) {
            moved = moveItemStackTo(st, hotStart, hotStart + 9, false);
        } else {
            moved = toBag(st);
        }
        if (!moved) return ItemStack.EMPTY;
        if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player p) { return p.isAlive(); }
}
