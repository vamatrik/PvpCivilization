package com.pvpciv.menu;

import com.pvpciv.game.Shop;
import com.pvpciv.item.SimpleCivItem;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Магазин Сороки: покупка по кнопкам, продажа ПКМ по предмету в сумке. */
public class TraderMenu extends CivMenu {
    public static final int BTN_SELL_JUNK = 99, BTN_BUY0 = 100;

    public TraderMenu(int id, Inventory inv) {
        super(ModRegistry.TRADER_MENU.get(), id, inv);
        addBag(181, 37, () -> true);
        addHotbar(inv, 181, 181);
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player p) {
        if (type == ClickType.PICKUP && button == 1 && slotId >= 0 && slotId < slots.size() && getCarried().isEmpty()) {
            Slot s = slots.get(slotId);
            if ((isBagSlot(slotId) || isHotbarSlot(slotId)) && Shop.sellPrice(s.getItem()) > 0) {
                if (p instanceof ServerPlayer sp) { Shop.sellStack(sp, s.getItem()); s.setChanged(); }
                return;
            }
        }
        super.clicked(slotId, button, type, p);
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) return true;
        if (id == BTN_SELL_JUNK) {
            for (Slot s : slots) {
                ItemStack st = s.getItem();
                if (st.getItem() instanceof SimpleCivItem si && si.sellPrice > 0 && !st.is(ModRegistry.STAR_W1.get()) && !st.is(ModRegistry.BLESSED_STAR_W1.get())
                        && !st.is(ModRegistry.BRUTE_CRYSTAL.get()) && !st.is(ModRegistry.ASH_DUST.get())) {
                    Shop.sellStack(sp, st);
                    s.setChanged();
                }
            }
            return true;
        }
        if (id >= BTN_BUY0) Shop.buy(sp, id - BTN_BUY0);
        return true;
    }

    @Override public ItemStack quickMoveStack(Player p, int index) { return moveBetween(index, -1, -1); }
}
