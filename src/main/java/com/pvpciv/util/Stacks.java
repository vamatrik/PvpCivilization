package com.pvpciv.util;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 1.2: склейка одинаковых стаков для сообщений («Лазуратрон ×200», а не «×64, ×64, ×64, ×8»). */
public final class Stacks {
    private Stacks() {}

    public static List<ItemStack> merged(List<ItemStack> in) {
        List<ItemStack> out = new ArrayList<>();
        outer:
        for (ItemStack s : in) {
            if (s.isEmpty()) continue;
            for (ItemStack o : out) if (ItemStack.isSameItemSameComponents(o, s)) { o.setCount(o.getCount() + s.getCount()); continue outer; }
            ItemStack c = s.copy();
            out.add(c);
        }
        return out;
    }
}
