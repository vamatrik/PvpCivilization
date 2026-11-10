package com.pvpciv.game;

import com.pvpciv.data.CivData;
import com.pvpciv.loot.LootTables;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Личный тайник: у каждого игрока свой таймер, лут сразу в инвентарь. */
public final class Stash {
    public static final long COOLDOWN = 20L * 60 * 30;

    public static void open(ServerPlayer p, BlockPos pos) {
        ServerLevel lvl = p.serverLevel();
        CivData data = CivData.get(p.server);
        String key = pos.getX() + "," + pos.getY() + "," + pos.getZ() + "|" + p.getUUID();
        long now = lvl.getGameTime();
        long ready = data.stash.getOrDefault(key, 0L);
        if (now < ready) {
            long min = (ready - now) / 1200 + 1;
            p.displayClientMessage(Component.literal("Тайник пуст. Ты его уже обчистил — загляни через " + min + " мин.").withStyle(ChatFormatting.GRAY), true);
            lvl.playSound(null, pos, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.6f, 0.7f);
            return;
        }
        data.stash.put(key, now + COOLDOWN);
        data.setDirty();
        String table = com.pvpciv.floor.FloorNorth.CRYPT_STASHES.contains(pos) || (pos.getZ() < -560 && Math.abs(pos.getX() - 3) < 10) ? "floor1b_crypt"
                : pos.getZ() < 0 ? "floor1b_stash" : "floor1_stash";
        List<ItemStack> loot = LootTables.roll(table, lvl.random, 1.0);
        MutableComponent msg = Component.literal("Тайник: ").withStyle(ChatFormatting.GOLD);
        boolean first = true;
        for (ItemStack st : loot) {
            if (!first) msg.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            first = false;
            msg.append(st.getHoverName().copy()).append(Component.literal(st.getCount() > 1 ? " ×" + st.getCount() : "").withStyle(ChatFormatting.GRAY));
            Story.announceRareDrop(lvl, st);
            ItemStack give = st.copy();
            com.pvpciv.inv.Bag.give(p, give);
        }
        p.sendSystemMessage(msg);
        lvl.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1f, 1f);
        lvl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.4f);
        lvl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0);
    }
}
