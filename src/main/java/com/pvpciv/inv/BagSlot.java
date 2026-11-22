package com.pvpciv.inv;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/** Слот сумки: закрытые слоты не принимают предметы; видимость зависит от вкладки. */
public class BagSlot extends SlotItemHandler {
    private final int viewIndex;
    private final IntSupplier unlocked, off;
    private final BooleanSupplier visible;

    public BagSlot(IItemHandler h, int index, int x, int y, IntSupplier unlocked, IntSupplier off, BooleanSupplier visible) {
        super(h, index, x, y);
        this.viewIndex = index; this.unlocked = unlocked; this.off = off; this.visible = visible;
    }

    /** Индекс в настоящей сумке (с учётом прокрутки). */
    public int bagIndex() { return viewIndex + off.getAsInt(); }
    public boolean locked() { return bagIndex() >= unlocked.getAsInt(); }

    @Override public boolean mayPlace(ItemStack st) { return !locked() && super.mayPlace(st); }
    @Override public boolean mayPickup(Player p) { return !locked() && super.mayPickup(p); }
    @Override public boolean isActive() { return visible.getAsBoolean(); }
}
