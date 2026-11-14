package com.pvpciv.combat;

import com.pvpciv.net.Network;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.9.9 — кровотечение от лис (этаж 3). Каждый удар — стак (до 10), стаки сгорают через 6 с без ударов.
 * Каждый стак — 0.2% макс. HP в секунду истинного урона. На 10 стаках — «разрыв»: 33% макс. HP, стаки сброшены.
 */
public final class Bleed {
    private Bleed() {}
    public static final int MAX = 10;
    private record St(int n, long last) {}
    private static final java.util.Map<java.util.UUID, St> STACKS = new java.util.HashMap<>();

    public static void hit(ServerPlayer p) {
        long now = p.serverLevel().getGameTime();
        St s = STACKS.get(p.getUUID());
        int n = (s == null || now - s.last > 120 ? 0 : s.n) + 1;
        if (n >= MAX) {
            STACKS.remove(p.getUUID());
            trueDamage(p, p.getMaxHealth() * 0.33f);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1.2f, 0.6f);
            p.serverLevel().sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    p.getX(), p.getY() + 1, p.getZ(), 40, 0.35, 0.5, 0.35, 0.2);
            p.displayClientMessage(Component.literal("🩸 РАЗРЫВ! −33% здоровья").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            send(p, 0, true);
            return;
        }
        STACKS.put(p.getUUID(), new St(n, now));
        p.displayClientMessage(Component.literal("🩸 Кровотечение " + n + "/" + MAX).withStyle(ChatFormatting.RED), true);
        send(p, n, false);
    }

    /** Раз в секунду: урон от стаков, сгорание старых. */
    public static void tick(MinecraftServer server) {
        if (STACKS.isEmpty()) return;
        var it = STACKS.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null) { it.remove(); continue; }
            if (!p.isAlive()) { it.remove(); send(p, 0, false); continue; }
            St s = en.getValue();
            if (p.serverLevel().getGameTime() - s.last > 120) { it.remove(); send(p, 0, false); continue; }
            trueDamage(p, p.getMaxHealth() * 0.002f * s.n);
        }
    }

    public static void clear(ServerPlayer p) { if (STACKS.remove(p.getUUID()) != null) send(p, 0, false); }

    /** Истинный урон: мимо брони, но не добивает через «тотем» механики — просто снимает HP (смерть — обычная). */
    private static void trueDamage(ServerPlayer p, float amt) {
        if (amt <= 0) return;
        if (p.getHealth() - amt > 0.5f) p.setHealth(p.getHealth() - amt);
        else p.hurt(p.damageSources().magic(), Float.MAX_VALUE / 4);
    }

    private static void send(ServerPlayer p, int n, boolean burst) {
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(14, "{\"n\":" + n + ",\"burst\":" + burst + "}"));
    }
}
