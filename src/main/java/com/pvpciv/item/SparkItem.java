package com.pvpciv.item;

import com.pvpciv.progress.Progress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Искры на земле: при подборе сразу превращаются в деньги. */
public class SparkItem extends Item {
    public SparkItem(Properties p) { super(p); }

    @Override
    public void inventoryTick(ItemStack st, Level level, Entity e, int slot, boolean selected) {
        if (!level.isClientSide && e instanceof ServerPlayer p && !st.isEmpty()) {
            Progress.addMoney(p, st.getCount(), true);
            st.setCount(0);
        }
    }
}
