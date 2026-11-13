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
    /** Отправить игроку, какие тайники он уже обчистил (для открытой крышки). */
    public static void sync(ServerPlayer p) {
        CivData data = CivData.get(p.server);
        String suffix = "|" + p.getUUID();
        long now = System.currentTimeMillis();
        java.util.List<Long> pos = new java.util.ArrayList<>(), ready = new java.util.ArrayList<>();
        for (var e : data.stash.entrySet()) {
            if (!e.getKey().endsWith(suffix) || e.getValue() <= now || e.getValue() > now * 2) continue;
            String pre = prefix(p.level());
            boolean kf = e.getKey().startsWith("f2:") || e.getKey().startsWith("f3:");
            if (pre.isEmpty() ? kf : !e.getKey().startsWith(pre)) continue;   // крышки — только тайников своего измерения
            String[] c = e.getKey().substring(kf ? 3 : 0, e.getKey().indexOf('|')).split(",");
            pos.add(BlockPos.asLong(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2])));
            ready.add(e.getValue());
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new com.pvpciv.net.Network.StashState(pos, ready));
    }

    /** Тайник обновляется через 8 часов реального времени (у каждого игрока свой таймер). */
    public static final long COOLDOWN_MS = 8L * 60 * 60 * 1000;

    private static String key(BlockPos pos, ServerPlayer p) {
        return prefix(p.level()) + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "|" + p.getUUID();
    }

    private static String prefix(net.minecraft.world.level.Level l) { return com.pvpciv.world.CivWorld.isF3(l) ? "f3:" : com.pvpciv.world.CivWorld.isF2(l) ? "f2:" : ""; }

    /** Минимальный уровень: тайник локации открывается только с её уровня. */
    public static int minLevel(BlockPos pos) {
        if (pos.getZ() < -560 && Math.abs(pos.getX() - 3) < 12) return 6;   // Ледяной Склеп
        if (pos.getZ() < 0) return 5;                                       // Инеевый Кряж
        return 1;
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        ServerLevel lvl = p.serverLevel();
        CivData data = CivData.get(p.server);
        boolean f2 = com.pvpciv.world.CivWorld.isF2(lvl);
        int need = com.pvpciv.world.CivWorld.isF3(lvl) ? com.pvpciv.floor.Floor3Areas.stashLevel(pos) : f2 ? com.pvpciv.floor.Floor2.stashLevel(pos) : minLevel(pos);
        if (com.pvpciv.progress.Progress.d(p).level < need) {
            p.displayClientMessage(Component.literal("Ты слишком слаб, чтобы открыть этот тайник (нужен ур. " + need + ").").withStyle(ChatFormatting.RED), true);
            lvl.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8f, 0.8f);
            return;
        }
        String key = key(pos, p);
        long now = System.currentTimeMillis();
        long ready = data.stash.getOrDefault(key, 0L);
        if (ready > now * 2) ready = 0;   // старые записи (в тиках игры) — сбросить
        if (now < ready) {
            long min = (ready - now) / 60000 + 1;
            String t = min >= 60 ? (min / 60) + " ч " + (min % 60) + " мин" : min + " мин";
            p.displayClientMessage(Component.literal("Тайник пуст. Ты его уже обчистил — загляни через " + t + ".").withStyle(ChatFormatting.GRAY), true);
            lvl.playSound(null, pos, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.6f, 0.7f);
            return;
        }
        data.stash.put(key, now + COOLDOWN_MS);
        com.pvpciv.progress.Progress.d(p).stashesOpened++;
        data.setDirty();
        sync(p);
        String table = com.pvpciv.world.CivWorld.isF3(lvl) ? (pos.getX() < 0 ? "floor3b_stash" : "floor3_stash") : f2 ? com.pvpciv.floor.Floor2.stashTable(pos) : com.pvpciv.floor.FloorNorth.CRYPT_STASHES.contains(pos) || (pos.getZ() < -560 && Math.abs(pos.getX() - 3) < 10) ? "floor1b_crypt"
                : pos.getZ() < 0 ? "floor1b_stash" : "floor1_stash";
        List<ItemStack> loot = LootTables.roll(table, lvl.random, 1.0);
        MutableComponent msg = Component.literal("Тайник: ").withStyle(ChatFormatting.GOLD);
        boolean first = true;
        for (ItemStack st : loot) {
            if (!first) msg.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            first = false;
            msg.append(st.getHoverName().copy()).append(Component.literal(st.getCount() > 1 ? " ×" + st.getCount() : "").withStyle(ChatFormatting.GRAY));
            Story.announceRareDrop(lvl, st, p.getGameProfile().getName());
            ItemStack give = st.copy();
            com.pvpciv.inv.Bag.give(p, give);
        }
        p.sendSystemMessage(msg);
        lvl.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1f, 1f);
        lvl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.4f);
        lvl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0);
    }
}
