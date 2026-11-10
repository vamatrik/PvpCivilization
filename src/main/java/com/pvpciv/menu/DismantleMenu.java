package com.pvpciv.menu;

import com.pvpciv.inv.Bag;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Разборочный стол: обломки и оружие → пыль, Искры, шанс звёзд. */
public class DismantleMenu extends CivMenu {
    public static final int BTN_GO = 0, BTN_UPGRADE = 1;
    public final SimpleContainer box = new SimpleContainer(1);
    private final DataSlot level = DataSlot.standalone();

    public DismantleMenu(int id, Inventory inv) {
        super(ModRegistry.DISMANTLE_MENU.get(), id, inv);
        addDataSlot(level);
        level.set(inv.player instanceof ServerPlayer sp ? Progress.d(sp).dismLvl : 1);
        addSlot(new Slot(box, 0, 77, 33) {
            @Override public boolean mayPlace(ItemStack st) { return canDismantle(st); }
        });
        addBag(169, 37, () -> true);
        addHotbar(inv, 169, 181);
    }

    public int stationLevel() { return level.get(); }
    public double mult() { return 1 + 0.15 * (level.get() - 1); }

    public static boolean canDismantle(ItemStack st) {
        if (st.is(ModRegistry.WEAPON_SHARDS.get())) return true;
        return st.getItem() instanceof CivWeaponItem && !st.is(ModRegistry.NOVICE_SWORD.get());
    }

    public static List<ItemStack> yield(ItemStack st, double mult, RandomSource r) {
        List<ItemStack> out = new ArrayList<>();
        int dust = 0, sparks = 0, stars = 0, blessed = 0;
        if (st.is(ModRegistry.WEAPON_SHARDS.get())) {
            for (int i = 0; i < st.getCount(); i++) {
                dust += 1 + r.nextInt(2);
                sparks += 2 + r.nextInt(3);
                if (r.nextDouble() < 0.04 * mult) stars++;
            }
        } else if (st.getItem() instanceof CivWeaponItem w) {
            int rr = Gear.rarity(st).ordinal();
            int s = Gear.stars(st);
            dust = 2 + rr * 2 + w.tier;
            sparks = 10 * (rr + 1) * w.tier;
            if (r.nextDouble() < 0.10 * (rr + 1) * mult) stars++;
            if (rr >= 3 && r.nextDouble() < 0.10 * (rr - 2) * mult) blessed++;
            for (int i = 0; i < s; i++) if (r.nextDouble() < 0.3 * mult) stars++;
        }
        dust = (int) Math.round(dust * mult);
        sparks = (int) Math.round(sparks * mult);
        if (dust > 0) out.add(new ItemStack(ModRegistry.ASH_DUST.get(), dust));
        if (sparks > 0) out.add(new ItemStack(ModRegistry.SPARKS.get(), sparks));
        if (stars > 0) out.add(new ItemStack(ModRegistry.STAR_W1.get(), stars));
        if (blessed > 0) out.add(new ItemStack(ModRegistry.BLESSED_STAR_W1.get(), blessed));
        return out;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) return true;
        if (id == BTN_UPGRADE) {
            if (Stations.upgrade(sp, Stations.DISMANTLE)) level.set(Progress.d(sp).dismLvl);
            return true;
        }
        ItemStack in = box.getItem(0);
        if (in.isEmpty() || !canDismantle(in)) return true;
        List<ItemStack> got = DismantleMenu.yield(in, mult(), sp.getRandom());
        box.setItem(0, ItemStack.EMPTY);
        MutableComponent msg = Component.literal("Разобрано: ").withStyle(ChatFormatting.GOLD);
        boolean first = true;
        for (ItemStack g : got) {
            if (!first) msg.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            first = false;
            msg.append(g.getHoverName().copy()).append(Component.literal(" ×" + g.getCount()).withStyle(ChatFormatting.GRAY));
            Bag.give(sp, g);
        }
        sp.sendSystemMessage(msg);
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1f, 1f);
        broadcastChanges();
        return true;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (p instanceof ServerPlayer sp) {
            ItemStack st = box.removeItemNoUpdate(0);
            if (!st.isEmpty()) Bag.give(sp, st);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) { return moveBetween(index, 0, 1); }
}
