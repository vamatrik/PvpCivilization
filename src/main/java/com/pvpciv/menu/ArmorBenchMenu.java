package com.pvpciv.menu;

import com.pvpciv.game.ArmorUp;
import com.pvpciv.inv.Bag;
import com.pvpciv.item.CivArmorItem;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Стол брони: слот брони, слот хлама. «Вложить» — хлам в опыт брони; «Улучшить» — ядро (+ свиток) из сумки → +1. */
public class ArmorBenchMenu extends CivMenu {
    public static final int BTN_FEED = 0, BTN_UP = 1;
    public final SimpleContainer box = new SimpleContainer(2);

    public ArmorBenchMenu(int id, Inventory inv) {
        super(ModRegistry.ARMOR_MENU.get(), id, inv);
        addSlot(new Slot(box, 0, 22, 44) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack st) { return st.getItem() instanceof CivArmorItem; }
        });
        addSlot(new Slot(box, 1, 22, 100) {
            @Override public boolean mayPlace(ItemStack st) { return ArmorUp.junk(st) > 0; }
        });
        addBag(169, 37, () -> true);
        addHotbar(inv, 169, 181);
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) return true;
        ItemStack armor = box.getItem(0);
        if (armor.isEmpty()) return true;
        if (id == BTN_FEED) {
            ItemStack junk = box.getItem(1);
            int used = ArmorUp.feed(armor, junk);
            if (used > 0) {
                junk.shrink(used);
                sp.level().playSound(null, sp.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8f, 1.3f);
            } else if (!junk.isEmpty()) sp.displayClientMessage(Component.literal(ArmorUp.full(armor) ? "Полоска уже полная — улучшай!" : "Это не хлам.").withStyle(ChatFormatting.YELLOW), true);
        } else if (id == BTN_UP) {
            ArmorUp.upgrade(sp, armor);
        }
        box.setChanged();
        broadcastChanges();
        return true;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (p instanceof ServerPlayer sp)
            for (int i = 0; i < 2; i++) {
                ItemStack st = box.removeItemNoUpdate(i);
                if (!st.isEmpty()) Bag.give(sp, st);
            }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) { return moveBetween(index, 0, 2); }
}
