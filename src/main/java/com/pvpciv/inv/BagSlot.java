package com.pvpciv.inv;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/** Слот сумки: закрытые слоты не принимают предметы; видимость зависит от вкладки. */
public class BagSlot extends SlotItemHandler {
    public final int bagIndex;
    private final IntSupplier unlocked;
    private final BooleanSupplier visible;

    public BagSlot(IItemHandler h, int index, int x, int y, IntSupplier unlocked, BooleanSupplier visible) {
        super(h, index, x, y);
        this.bagIndex = index; this.unlocked = unlocked; this.visible = visible;
    }

    public boolean locked() { return bagIndex >= unlocked.getAsInt(); }

    @Override public boolean mayPlace(ItemStack st) { return !locked() && super.mayPlace(st); }
    @Override public boolean mayPickup(Player p) { return !locked() && super.mayPickup(p); }
    @Override public boolean isActive() { return visible.getAsBoolean(); }
}
