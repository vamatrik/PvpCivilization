package com.pvpciv.menu;

import com.pvpciv.data.StoryData;
import com.pvpciv.game.GameEvents;
import com.pvpciv.game.Story;
import com.pvpciv.inv.Bag;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/** Кузница: слот оружия + слот звезды, кнопка «Заточить», улучшение станции. */
public class ForgeMenu extends CivMenu {
    public static final int BTN_ENHANCE = 0, BTN_UPGRADE = 1;
    public static final int RITUAL = 44;
    public final SimpleContainer box = new SimpleContainer(2);
    private final DataSlot level = DataSlot.standalone(), progress = DataSlot.standalone(), result = DataSlot.standalone();
    private final BlockPos pos;
    private boolean pendingSuccess;
    private int pendingFrom;
    private boolean pendingBlessed;

    public ForgeMenu(int id, Inventory inv) { this(id, inv, BlockPos.ZERO); }

    public ForgeMenu(int id, Inventory inv, BlockPos pos) {
        super(ModRegistry.FORGE_MENU.get(), id, inv);
        this.pos = pos;
        addDataSlot(level); addDataSlot(progress); addDataSlot(result);
        level.set(inv.player instanceof ServerPlayer sp ? Progress.d(sp).forgeLvl : 1);
        addSlot(new Slot(box, 0, 77, 33) {
            @Override public boolean mayPlace(ItemStack st) { return st.getItem() instanceof CivWeaponItem && progress.get() == 0; }
            @Override public boolean mayPickup(Player p) { return progress.get() == 0; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(box, 1, 25, 51) {
            @Override public boolean mayPlace(ItemStack st) { return (st.is(ModRegistry.STAR_W1.get()) || st.is(ModRegistry.BLESSED_STAR_W1.get())) && progress.get() == 0; }
            @Override public boolean mayPickup(Player p) { return progress.get() == 0; }
        });
        addBag(169, 37, () -> true);
        addHotbar(inv, 169, 181);
    }

    public int stationLevel() { return level.get(); }
    public int progress() { return progress.get(); }
    public int result() { return result.get(); }
    public double chanceBonus() { return 0.01 * (level.get() - 1); }

    public double chance() {
        ItemStack w = box.getItem(0);
        if (w.isEmpty()) return 0;
        return Math.min(1.0, Gear.chance(Gear.stars(w)) + chanceBonus());
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) return true;
        if (id == BTN_UPGRADE) {
            if (Stations.upgrade(sp, Stations.FORGE)) level.set(Progress.d(sp).forgeLvl);
            return true;
        }
        if (id != BTN_ENHANCE || progress.get() > 0) return true;
        ItemStack w = box.getItem(0), star = box.getItem(1);
        if (!(w.getItem() instanceof CivWeaponItem) || star.isEmpty()) return true;
        int s = Gear.stars(w);
        if (s >= Gear.MAX_STARS) {
            sp.displayClientMessage(Component.literal("Это +10. Дальше — только Звезда Бездны с финального босса мира.").withStyle(ChatFormatting.DARK_PURPLE), true);
            return true;
        }
        pendingBlessed = star.is(ModRegistry.BLESSED_STAR_W1.get());
        star.shrink(1);
        pendingFrom = s;
        pendingSuccess = sp.getRandom().nextDouble() < chance();
        result.set(0);
        progress.set(1);
        ServerLevel lvl = sp.serverLevel();
        Vector3f col = rgb(Gear.starColor(s + 1));
        double fx = pos.getX() + 0.5, fy = pos.getY() + 1.3, fz = pos.getZ() + 0.5;
        if (s + 1 >= 8) broadcast(lvl, sp.getGameProfile().getName() + " точит " + w.getHoverName().getString() + " до +" + (s + 1) + "... шанс " + Math.round(chance() * 100) + "%", ChatFormatting.LIGHT_PURPLE);
        for (int t = 1; t <= RITUAL; t++) {
            int tt = t;
            GameEvents.later(sp.server, t, () -> {
                if (progress.get() == 0) return;
                progress.set(tt);
                if (tt % 12 == 1) {
                    lvl.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 0.8f + tt / 100f);
                    lvl.sendParticles(new DustParticleOptions(col, 1.4f), fx, fy, fz, 20, 0.25, 0.3, 0.25, 0);
                    lvl.sendParticles(ParticleTypes.LAVA, fx, fy, fz, 3, 0.1, 0.1, 0.1, 0);
                }
                if (tt == RITUAL) resolve(sp);
            });
        }
        return true;
    }

    private void resolve(ServerPlayer sp) {
        if (progress.get() == 0) return;
        progress.set(0);
        ServerLevel lvl = sp.serverLevel();
        ItemStack w = box.getItem(0);
        int target = pendingFrom + 1;
        double fx = pos.getX() + 0.5, fy = pos.getY() + 1.3, fz = pos.getZ() + 0.5;
        if (pendingSuccess) {
            Gear.setStars(w, target);
            box.setChanged();
            result.set(1);
            lvl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1f, 1.2f + target * 0.05f);
            lvl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 1f);
            lvl.sendParticles(new DustParticleOptions(rgb(Gear.starColor(target)), 2.0f), fx, fy, fz, 80, 0.5, 0.6, 0.5, 0);
            lvl.sendParticles(ParticleTypes.END_ROD, fx, fy, fz, 30, 0.2, 0.4, 0.2, 0.08);
            if (target >= 8) broadcast(lvl, "УСПЕХ! " + sp.getGameProfile().getName() + " получил +" + target + "!", ChatFormatting.GOLD);
        } else if (pendingBlessed) {
            Gear.setStars(w, Math.max(0, pendingFrom - 1));
            box.setChanged();
            result.set(2);
            lvl.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1f, 0.6f);
            lvl.sendParticles(ParticleTypes.SMOKE, fx, fy, fz, 30, 0.3, 0.3, 0.3, 0.02);
        } else {
            int tier = w.getItem() instanceof CivWeaponItem cw ? cw.tier : 1;
            String name = w.getHoverName().getString();
            box.setItem(0, new ItemStack(ModRegistry.WEAPON_SHARDS.get(), 1 + tier + pendingFrom / 2));
            result.set(3);
            lvl.playSound(null, pos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1f, 0.8f);
            lvl.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1f, 0.6f);
            lvl.sendParticles(ParticleTypes.LARGE_SMOKE, fx, fy, fz, 40, 0.4, 0.4, 0.4, 0.03);
            lvl.sendParticles(ParticleTypes.CRIT, fx, fy, fz, 40, 0.4, 0.4, 0.4, 0.4);
            if (pendingFrom >= 7) broadcast(lvl, sp.getGameProfile().getName() + " сломал " + name + ". Минута молчания.", ChatFormatting.DARK_GRAY);
        }
        broadcastChanges();
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (p instanceof ServerPlayer sp) {
            resolve(sp);
            for (int i = 0; i < 2; i++) {
                ItemStack st = box.removeItemNoUpdate(i);
                if (!st.isEmpty()) Bag.give(sp, st);
            }
            Story.ensureKit(sp);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        if (progress.get() > 0) return ItemStack.EMPTY;
        return moveBetween(index, 0, 2);
    }

    private static void broadcast(ServerLevel lvl, String msg, ChatFormatting c) {
        for (ServerPlayer pl : lvl.getServer().getPlayerList().getPlayers()) pl.sendSystemMessage(Component.literal("⚒ " + msg).withStyle(c));
    }

    private static Vector3f rgb(int c) { return new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f); }

    @SuppressWarnings("unused") private static final Class<?> D = StoryData.class;
}
