package com.pvpciv.menu;

import com.pvpciv.inv.Bag;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** RPG-инвентарь: вкладки «Сумка», «Снаряжение», «Персонаж». */
public class CivInventoryMenu extends CivMenu {
    public static final int TAB_BAG = 0, TAB_EQUIP = 1, TAB_STATS = 2;
    public static final int BTN_SORT = 10;
    private final DataSlot tab = DataSlot.standalone();
    public int armorStart;

    public CivInventoryMenu(int id, Inventory inv) {
        super(ModRegistry.INV_MENU.get(), id, inv);
        addDataSlot(tab);
        addBag(10, 30, () -> tab.get() == TAB_BAG);
        addHotbar(inv, 10, 166);
        armorStart = slots.size();
        EquipmentSlot[] eq = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            EquipmentSlot es = eq[i];
            addSlot(new Slot(inv, 39 - i, 96, 34 + i * 20) {
                @Override public boolean mayPlace(ItemStack st) { return st.canEquip(es, inv.player); }
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean isActive() { return tab.get() == TAB_EQUIP; }
            });
        }
        addSlot(new Slot(inv, 40, 96, 120) { @Override public boolean isActive() { return tab.get() == TAB_EQUIP; } });
    }

    public int tab() { return tab.get(); }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id >= 0 && id <= 2) { tab.set(id); return true; }
        if (id == BTN_SORT && p instanceof ServerPlayer sp) { sort(sp); return true; }
        return false;
    }

    public static void sort(ServerPlayer p) {
        ItemStackHandler h = Bag.get(p);
        int n = Bag.unlocked(p);
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < n; i++) { ItemStack st = h.getStackInSlot(i); if (!st.isEmpty()) items.add(st.copy()); h.setStackInSlot(i, ItemStack.EMPTY); }
        items.sort(Comparator.comparingInt(Categories::of).thenComparing(Comparator.comparingInt(Categories::rank).reversed())
                .thenComparing(st -> st.getHoverName().getString()));
        for (ItemStack st : items) Bag.insert(p, st);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        if (tab.get() == TAB_EQUIP && !(index >= armorStart)) {
            Slot s = slots.get(index);
            if (s.hasItem()) {
                ItemStack st = s.getItem();
                for (int i = 0; i < 4; i++) {
                    Slot a = slots.get(armorStart + i);
                    if (!a.hasItem() && a.mayPlace(st)) {
                        ItemStack copy = st.copy();
                        a.setByPlayer(st.split(1));
                        s.setChanged();
                        return copy;
                    }
                }
            }
        }
        if (index >= armorStart) {
            Slot s = slots.get(index);
            if (!s.hasItem()) return ItemStack.EMPTY;
            ItemStack st = s.getItem(), copy = st.copy();
            if (!moveItemStackTo(st, bagStart, bagStart + unlockedSlots(), false) && !moveItemStackTo(st, hotStart, hotStart + 9, false)) return ItemStack.EMPTY;
            s.setChanged();
            return copy;
        }
        return moveBetween(index, -1, -1);
    }
}
