package com.pvpciv.game;

import com.pvpciv.floor.Floor4;
import com.pvpciv.floor.Floor5;
import com.pvpciv.net.Network;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 0.12 — ракета «Заря-5»: космодром NaZa (этаж 4) ↔ ангар станции «Орбита» (этаж 5).
 * Открывается после победы над доктором Ноэлем (трофей 64). Полёт — кат-сцена ~6 с: отсчёт, тряска камеры, белая вспышка,
 * на станции — проявление из темноты (PhoneData 22). Пока игрок «в ракете», он не двигается и не получает урон.
 */
public final class Rocket {
    private Rocket() {}
    public static final int LEN = 120;
    private record Flight(boolean up, int t) {}
    private static final Map<UUID, Flight> FLYING = new HashMap<>();

    public static boolean unlocked(com.pvpciv.data.StoryData d) { return (d.trophies & 64) != 0; }
    public static boolean flying(ServerPlayer p) { return FLYING.containsKey(p.getUUID()); }

    /** ПКМ по блоку: пульт космодрома (этаж 4) или ангара (этаж 5). */
    public static boolean onUse(ServerPlayer p, BlockPos pos) {
        boolean f4 = CivWorld.isF4(p.level()) && pos.getZ() == Floor4.COSMO.getZ() - 16 && Math.abs(pos.getX() - Floor4.COSMO.getX()) <= 3
                && (pos.getY() == Floor4.Y || pos.getY() == Floor4.Y + 1);
        boolean f5 = CivWorld.isF5(p.level()) && pos.equals(Floor5.HANGAR_CONSOLE());
        if (!f4 && !f5) return false;
        if (flying(p)) return true;
        var d = com.pvpciv.progress.Progress.d(p);
        if (f4 && !unlocked(d)) {
            Story.say(p, "Пульт «Заря-5»", 0xFF8A30, "СТАРТ ЗАБЛОКИРОВАН. Ключ запуска — у руководства NaZa. (Победи доктора Ноэля в «Ковчеге».)");
            return true;
        }
        FLYING.put(p.getUUID(), new Flight(f4, 0));
        send(p, "launch");
        p.displayClientMessage(Component.literal(f4 ? "«Заря-5»: курс — станция «Орбита»" : "«Заря-5»: курс — космодром NaZa").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private static void send(ServerPlayer p, String kind) {
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"" + kind + "\",\"len\":" + LEN + "}"));
    }

    public static void tick(MinecraftServer server) {
        FLYING.entrySet().removeIf(en -> {
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null) return true;
            Flight f = en.getValue();
            int t = f.t() + 1;
            en.setValue(new Flight(f.up(), t));
            ServerLevel l = p.serverLevel();
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 128, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 4, false, false));
            Vec3 base = f.up() ? new Vec3(Floor4.COSMO.getX() + 0.5, Floor4.Y + 2, Floor4.COSMO.getZ() + 2.5)
                    : new Vec3(Floor5.HANGAR_M.center().getX() + 0.5, Floor5.Y, Floor5.HANGAR_M.center().getZ() + 4.5);
            if (t == 1) l.playSound(null, BlockPos.containing(base), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2f, 0.6f);
            if (t == 20 || t == 40 || t == 60) {
                int n = 4 - t / 20;
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(String.valueOf(n)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                l.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 0.8f + n * 0.1f);
            }
            if (t > 60) {
                if (t == 61) {
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("ПУСК").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
                    l.playSound(null, BlockPos.containing(base), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2f, 0.5f);
                }
                if (t % 4 == 0) l.playSound(null, BlockPos.containing(base), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.5f, 0.5f);
                l.sendParticles(ParticleTypes.FLAME, base.x, base.y, base.z, 25, 1.2, 0.3, 1.2, 0.08);
                l.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, base.x, base.y, base.z, 10, 2.5, 0.5, 2.5, 0.03);
            }
            if (t >= LEN) {
                if (f.up()) {
                    ServerLevel to = CivWorld.f5(server);
                    Vec3 a = Floor5.HANGAR_ARRIVE();
                    p.teleportTo(to, a.x, a.y, a.z, 180, 0);
                    Floor5.onArrive(p);
                } else {
                    ServerLevel to = CivWorld.f4(server);
                    p.teleportTo(to, Floor4.COSMO.getX() + 0.5, Floor4.Y, Floor4.COSMO.getZ() - 20.5, 180, 0);
                }
                p.resetFallDistance();
                send(p, "arrive");
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(f.up() ? "Станция «Орбита»" : "Космодром NaZa").withStyle(ChatFormatting.LIGHT_PURPLE)));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(f.up() ? "Этаж 5" : "Этаж 4").withStyle(ChatFormatting.GRAY)));
                return true;
            }
            return false;
        });
    }
}
