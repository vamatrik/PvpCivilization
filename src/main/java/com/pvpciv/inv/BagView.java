package com.pvpciv.inv;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.IntSupplier;

/** Окно в сумку: VIEW слотов, сдвинутых на offset (в слотах). */
public final class BagView implements IItemHandlerModifiable {
    private final ItemStackHandler h;
    private final IntSupplier off;
    private final java.util.function.Supplier<int[]> map;
    public BagView(ItemStackHandler h, IntSupplier off) { this(h, off, () -> null); }
    /** map != null — режим поиска: окно показывает только эти индексы сумки (по порядку), off — сдвиг внутри списка. */
    public BagView(ItemStackHandler h, IntSupplier off, java.util.function.Supplier<int[]> map) { this.h = h; this.off = off; this.map = map; }
    public int real(int i) {
        int[] m = map.get();
        int j = i + off.getAsInt();
        if (m == null) return j;
        return j < m.length ? m[j] : -1;
    }
    private boolean ok(int i) { int r = real(i); return r >= 0 && r < h.getSlots(); }
    @Override public int getSlots() { return Bag.VIEW; }
    @Override public ItemStack getStackInSlot(int i) { return ok(i) ? h.getStackInSlot(real(i)) : ItemStack.EMPTY; }
    @Override public ItemStack insertItem(int i, ItemStack st, boolean sim) { return ok(i) ? h.insertItem(real(i), st, sim) : st; }
    @Override public ItemStack extractItem(int i, int n, boolean sim) { return ok(i) ? h.extractItem(real(i), n, sim) : ItemStack.EMPTY; }
    @Override public int getSlotLimit(int i) { return ok(i) ? h.getSlotLimit(real(i)) : 0; }
    @Override public boolean isItemValid(int i, ItemStack st) { return ok(i) && h.isItemValid(real(i), st); }
    @Override public void setStackInSlot(int i, ItemStack st) { if (ok(i)) h.setStackInSlot(real(i), st); }
}
